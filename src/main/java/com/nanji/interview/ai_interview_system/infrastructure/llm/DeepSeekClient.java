package com.nanji.interview.ai_interview_system.infrastructure.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * DeepSeek Chat Completions API 客户端（迁移自 feature/interview-agent）。
 * <p>
 * 除同步 chat 外，额外实现了 chatStream：以 stream=true 调用 DeepSeek，
 * 逐帧解析 SSE 增量并把 delta.content 实时回调给调用方（真流式，非打字机模拟）。
 */
@Primary
@Component
public class DeepSeekClient implements LlmClient {

    private final DeepSeekProperties properties;
    private final RestClient restClient;

    /**
     * 流式解析用。此处自行创建而非注入容器中的 ObjectMapper，
     * 避免改动构造函数签名触发历史出现过的 Bean 歧义问题。
     */
    private final ObjectMapper streamMapper = new ObjectMapper();

    public DeepSeekClient(DeepSeekProperties properties) {
        this.properties = properties;
        // 生成题目/评价的 LLM 调用耗时较长，设置 10s 连接超时、120s 读超时
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(120));
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public String chat(String systemPrompt, String userPrompt) {

        Map<String, Object> requestBody = Map.of(
                "model", properties.getModel(),
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
                .uri(properties.getBaseUrl() + "/chat/completions")
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + properties.getApiKey()
                )
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(Map.class);

        List choices = (List) response.get("choices");

        Map firstChoice = (Map) choices.get(0);

        Map message = (Map) firstChoice.get("message");

        return (String) message.get("content");
    }

    /**
     * 流式调用 DeepSeek：每收到模型的一小段输出就回调一次 onToken。
     */
    @Override
    public void chatStream(String systemPrompt, String userPrompt, Consumer<String> onToken) {
        String requestBody;
        try {
            requestBody = streamMapper.writeValueAsString(Map.of(
                    "model", properties.getModel(),
                    "stream", true,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userPrompt)
                    )
            ));
        } catch (Exception e) {
            throw new IllegalStateException("构造 DeepSeek 流式请求失败", e);
        }

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(properties.getBaseUrl() + "/chat/completions"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.ACCEPT, "text/event-stream")
                .timeout(Duration.ofSeconds(180))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<InputStream> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (Exception e) {
            throw new IllegalStateException("DeepSeek 流式请求失败", e);
        }

        if (response.statusCode() != 200) {
            String body = readFullySnippet(response.body());
            throw new IllegalStateException("DeepSeek 流式调用失败，HTTP " + response.statusCode()
                    + "：" + body);
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) {
                    continue;
                }
                if (!line.startsWith("data:")) {
                    continue;
                }
                String data = line.substring(5).trim();
                if (data.isEmpty() || "[DONE]".equals(data)) {
                    break;
                }
                String delta = extractDelta(data);
                if (delta != null && !delta.isEmpty()) {
                    onToken.accept(delta);
                }
            }
        } catch (java.io.IOException e) {
            throw new IllegalStateException("读取 DeepSeek 流式响应中断", e);
        }
    }

    /** 从一帧 SSE data 中取出 choices[0].delta.content */
    private String extractDelta(String data) {
        try {
            JsonNode root = streamMapper.readTree(data);
            JsonNode choices = root.get("choices");
            if (choices == null || !choices.isArray() || choices.isEmpty()) {
                return null;
            }
            JsonNode delta = choices.get(0).get("delta");
            if (delta == null) {
                return null;
            }
            JsonNode content = delta.get("content");
            if (content == null || content.isNull()) {
                return null;
            }
            return content.asText();
        } catch (Exception e) {
            // 单帧解析失败不应中断整个流（偶发心跳帧/非 JSON 行）
            return null;
        }
    }

    private String readFullySnippet(InputStream in) {
        if (in == null) {
            return "";
        }
        try {
            byte[] buf = new byte[512];
            int n = in.read(buf);
            String s = n > 0 ? new String(buf, 0, n, StandardCharsets.UTF_8) : "";
            return s.length() > 300 ? s.substring(0, 300) : s;
        } catch (Exception e) {
            return "";
        }
    }
}
