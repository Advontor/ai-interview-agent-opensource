package com.nanji.interview.ai_interview_system.infrastructure.llm;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 阿里云百炼 DashScope OpenAI Compatible API 客户端。
 *
 * 支持模型 Fallback：
 * 主模型因额度耗尽、限流或服务端异常不可用时，
 * 自动尝试下一个备用模型。
 */
@Component
public class DashScopeClient implements LlmClient {

    private final DashScopeProperties properties;
    private final RestClient restClient;

    public DashScopeClient(DashScopeProperties properties) {
        this.properties = properties;

        SimpleClientHttpRequestFactory factory =
                new SimpleClientHttpRequestFactory();

        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(120));

        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .build();
    }

    @Override
    public String chat(String systemPrompt, String userPrompt) {

        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new IllegalStateException(
                    "未配置 DASHSCOPE_API_KEY。请通过环境变量设置阿里云百炼 API Key"
            );
        }
        if (properties.getBaseUrl() == null || properties.getBaseUrl().isBlank()) {
            throw new IllegalStateException("未配置 llm.base-url");
        }

        List<String> models = properties.getAllModels();

        if (models.isEmpty()) {
            throw new IllegalStateException("未配置可用的 LLM 模型");
        }

        RuntimeException lastException = null;

        for (int i = 0; i < models.size(); i++) {

            String model = models.get(i);

            try {
                System.out.println(
                        "[LLM] 正在调用模型："
                                + model
                                + "（"
                                + (i + 1)
                                + "/"
                                + models.size()
                                + "）"
                );

                String result = callModel(
                        model,
                        systemPrompt,
                        userPrompt
                );

                System.out.println(
                        "[LLM] 模型调用成功：" + model
                );

                return result;

            } catch (HttpStatusCodeException e) {

                lastException = e;

                int statusCode = e.getStatusCode().value();
                String responseBody = e.getResponseBodyAsString();

                System.err.println(
                        "[LLM] 模型调用失败："
                                + model
                                + "，HTTP "
                                + statusCode
                );

                if (!shouldFallback(statusCode, responseBody)) {
                    if (statusCode == 401) {
                        throw new IllegalStateException(
                                "阿里云百炼鉴权失败（HTTP 401）。请检查 DASHSCOPE_API_KEY 是否有效且属于当前地域/接口，未切换模型",
                                e
                        );
                    }
                    if (statusCode == 400) {
                        throw new IllegalStateException(
                                "阿里云百炼请求参数错误（HTTP 400），未切换模型：" + sanitizeError(responseBody), e);
                    }
                    if (statusCode == 403) {
                        throw new IllegalStateException(
                                "阿里云百炼权限不足（HTTP 403，非确认的额度错误），未切换模型：" + sanitizeError(responseBody), e);
                    }
                    if (statusCode == 404) {
                        throw new IllegalStateException(
                                "阿里云百炼接口或模型不存在（HTTP 404），请检查 base-url 和模型名，未切换模型："
                                        + sanitizeError(responseBody), e);
                    }
                    throw e;
                }

                if (i < models.size() - 1) {
                    System.err.println(
                            "[LLM] 当前错误允许 Fallback，准备切换备用模型..."
                    );
                }

            } catch (Exception e) {

                /*
                 * 网络异常、连接超时等情况允许尝试备用模型。
                 *
                 * 注意：
                 * 这里不进行同一模型的重复调用，
                 * 只切换到下一个模型。
                 */
                lastException = new RuntimeException(e);

                System.err.println(
                        "[LLM] 模型调用发生异常："
                                + model
                                + "，异常类型："
                                + e.getClass().getSimpleName()
                );

                if (i < models.size() - 1) {
                    System.err.println(
                            "[LLM] 准备切换备用模型..."
                    );
                }
            }
        }

        String detail = lastException == null ? "未知原因" : describeFailure(lastException);
        throw new RuntimeException(
                "所有配置的 LLM 模型均调用失败：" + detail,
                lastException
        );
    }

    /**
     * 调用指定模型。
     */
    private String callModel(
            String model,
            String systemPrompt,
            String userPrompt
    ) {

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "max_tokens", 4096,
                "messages", List.of(
                        Map.of(
                                "role", "system",
                                "content", systemPrompt
                        ),
                        Map.of(
                                "role", "user",
                                "content", userPrompt
                        )
                )
        );

        Map response = restClient.post()
                .uri(properties.getBaseUrl().replaceAll("/+$", "") + "/chat/completions")
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + properties.getApiKey()
                )
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(Map.class);

        if (response == null) {
            throw new IllegalStateException(
                    "LLM 返回结果为空"
            );
        }

        List choices = (List) response.get("choices");

        if (choices == null || choices.isEmpty()) {
            throw new IllegalStateException(
                    "LLM 返回结果中不存在 choices"
            );
        }

        Map firstChoice = (Map) choices.get(0);
        Map message = (Map) firstChoice.get("message");

        if (message == null) {
            throw new IllegalStateException(
                    "LLM 返回结果中不存在 message"
            );
        }

        Object content = message.get("content");

        if (content == null) {
            throw new IllegalStateException(
                    "LLM 返回结果中不存在 content"
            );
        }

        return content.toString();
    }

    /**
     * 判断当前错误是否允许自动切换备用模型。
     *
     * 允许：
     * 403：免费额度耗尽/模型权限或资源限制
     * 429：限流或额度限制
     * 5xx：模型服务端临时异常
     *
     * 不允许：
     * 400：请求本身存在问题
     * 401：API Key 鉴权问题
     * 404：接口/模型配置问题
     */
    private boolean shouldFallback(
            int statusCode,
            String responseBody
    ) {

        if (statusCode == 429) {
            return true;
        }

        if (statusCode >= 500 && statusCode <= 599) {
            return true;
        }

        if (statusCode == 403) {

            if (responseBody == null) {
                return false;
            }

            String body = responseBody.toLowerCase();

            return body.contains("quota")
                    || body.contains("free tier")
                    || body.contains("allocationquota")
                    || body.contains("free_tier")
                    || body.contains("free-tier")
                    || body.contains("quota exhausted")
                    || body.contains("quota_exceeded");
        }

        return false;
    }

    private String sanitizeError(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "服务未返回详细错误";
        }
        String compact = responseBody.replaceAll("\\s+", " ").trim();
        String apiKey = properties.getApiKey();
        if (apiKey != null && !apiKey.isBlank()) {
            compact = compact.replace(apiKey, "[REDACTED]");
        }
        return compact.length() > 300 ? compact.substring(0, 300) : compact;
    }

    private String describeFailure(Throwable failure) {
        Throwable current = failure;
        Throwable deepest = failure;
        while (current != null) {
            deepest = current;
            current = current.getCause();
        }
        String message = deepest.getMessage();
        if (message == null || message.isBlank()) message = deepest.getClass().getSimpleName();
        return sanitizeError(message);
    }
}

