package com.nanji.interview.ai_interview_system.modules.voice.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nanji.interview.ai_interview_system.modules.voice.service.XfyunService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 讯飞语音识别服务实现
 * <p>
 * 调用讯飞实时语音转写API（WebSocket）
 * 文档：https://www.xfyun.cn/doc/asr/voicedictation/API.html
 * <p>
 * 音频格式：16kHz 16位单声道PCM（已由AudioConverter转好）
 */
@Service
public class XfyunServiceImpl implements XfyunService {

    @Value("${xfyun.app-id:}")
    private String appId;

    @Value("${xfyun.api-key:}")
    private String apiKey;

    @Value("${xfyun.api-secret:}")
    private String apiSecret;

    @Resource
    private ObjectMapper objectMapper;

    /**
     * 讯飞语音识别WebSocket地址
     */
    private static final String WS_URL = "wss://iat-api.xfyun.cn/v2/iat";

    /**
     * 每片音频大小（字节），讯飞推荐128字节
     */
    private static final int CHUNK_SIZE = 128;

    @Override
    public String recognize(String pcmFilePath) {
        System.out.println("[讯飞识别] 开始识别，文件：" + pcmFilePath);
        System.out.println("[讯飞识别] appId=" + appId + "，apiKey=" + (apiKey.isBlank() ? "未配置" : "已配置"));

        // 如果没有配置讯飞密钥，返回提示
        if (appId.isBlank() || apiKey.isBlank() || apiSecret.isBlank()) {
            System.err.println("[讯飞识别] 讯飞密钥未配置，请在application.yml中配置xfyun.app-id、xfyun.api-key、xfyun.api-secret");
            return "讯飞密钥未配置，请联系管理员";
        }

        try {
            // 1. 读取PCM文件
            byte[] pcmData = readPcmFile(pcmFilePath);
            if (pcmData.length == 0) {
                return "音频文件为空";
            }
            System.out.println("[讯飞识别] 读取PCM文件成功，大小：" + pcmData.length + "字节");

            // 2. 生成鉴权URL
            String authUrl = generateAuthUrl();
            System.out.println("[讯飞识别] 生成鉴权URL成功");

            // 3. 建立WebSocket连接，发送音频，接收结果
            StringBuilder resultBuilder = new StringBuilder();
            CountDownLatch latch = new CountDownLatch(1);

            HttpClient client = HttpClient.newHttpClient();
            WebSocket webSocket = client.newWebSocketBuilder()
                    .buildAsync(URI.create(authUrl), new WebSocket.Listener() {
                        @Override
                        public void onOpen(WebSocket webSocket) {
                            System.out.println("[讯飞识别] WebSocket连接成功");
                            webSocket.request(1);

                            try {
                                // 4. 发送首帧（参数配置）
                                String firstFrame = buildFirstFrame();
                                webSocket.sendText(firstFrame, true);
                                System.out.println("[讯飞识别] 发送首帧成功");

                                // 5. 分片发送音频数据
                                int offset = 0;
                                while (offset < pcmData.length) {
                                    int end = Math.min(offset + CHUNK_SIZE, pcmData.length);
                                    byte[] chunk = new byte[end - offset];
                                    System.arraycopy(pcmData, offset, chunk, 0, chunk.length);

                                    // base64编码
                                    String base64Chunk = Base64.getEncoder().encodeToString(chunk);
                                    String audioFrame = buildAudioFrame(base64Chunk);
                                    webSocket.sendText(audioFrame, true);

                                    offset = end;

                                    // 模拟实时音频，每片间隔20ms
                                    Thread.sleep(20);
                                }

                                // 6. 发送结束帧
                                String endFrame = buildEndFrame();
                                webSocket.sendText(endFrame, true);
                                System.out.println("[讯飞识别] 音频发送完成，等待识别结果...");

                            } catch (Exception e) {
                                e.printStackTrace();
                                latch.countDown();
                            }
                        }

                        @Override
                        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                            webSocket.request(1);
                            try {
                                // 解析讯飞返回的结果
                                JsonNode root = objectMapper.readTree(data.toString());
                                int code = root.path("code").asInt();
                                String message = root.path("message").asText();

                                if (code != 0) {
                                    System.err.println("[讯飞识别] 识别出错，code=" + code + "，message=" + message);
                                    latch.countDown();
                                    return null;
                                }

                                // 提取识别文字
                                JsonNode dataNode = root.path("data").path("result").path("ws");
                                if (dataNode.isArray()) {
                                    for (JsonNode wsNode : dataNode) {
                                        JsonNode cwNode = wsNode.path("cw");
                                        if (cwNode.isArray() && cwNode.size() > 0) {
                                            String word = cwNode.get(0).path("w").asText();
                                            resultBuilder.append(word);
                                        }
                                    }
                                }

                                // 判断是否是最后一帧
                                boolean isLast = root.path("data").path("result").path("ls").asBoolean(false);
                                if (isLast) {
                                    System.out.println("[讯飞识别] 识别完成，结果：" + resultBuilder);
                                    latch.countDown();
                                }

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            return null;
                        }

                        @Override
                        public void onError(WebSocket webSocket, Throwable error) {
                            System.err.println("[讯飞识别] WebSocket出错：" + error.getMessage());
                            error.printStackTrace();
                            latch.countDown();
                        }
                    })
                    .join();

            // 等待识别完成，最多等30秒
            boolean completed = latch.await(30, TimeUnit.SECONDS);
            if (!completed) {
                System.err.println("[讯飞识别] 识别超时");
            }

            // 关闭连接
            try {
                webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "done");
            } catch (Exception ignored) {}

            String result = resultBuilder.toString().trim();
            return result.isEmpty() ? "未识别到语音内容" : result;

        } catch (Exception e) {
            e.printStackTrace();
            return "语音识别出错：" + e.getMessage();
        }
    }

    /**
     * 生成讯飞鉴权URL
     */
    private String generateAuthUrl() throws Exception {
        String host = "iat-api.xfyun.cn";
        String date = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US)
                .format(new Date());
        // 设置时区为GMT
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        date = sdf.format(new Date());

        String requestLine = "GET /v2/iat HTTP/1.1";

        // 拼接签名字符串
        String signatureOrigin = "host: " + host + "\ndate: " + date + "\n" + requestLine;

        // HMAC-SHA256签名
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] signatureBytes = mac.doFinal(signatureOrigin.getBytes(StandardCharsets.UTF_8));
        String signature = Base64.getEncoder().encodeToString(signatureBytes);

        // 拼接authorization
        String authorizationOrigin = String.format(
                "api_key=\"%s\", algorithm=\"hmac-sha256\", headers=\"host date request-line\", signature=\"%s\"",
                apiKey, signature);

        // URL编码
        String authorizationEncoded = URLEncoder.encode(authorizationOrigin, StandardCharsets.UTF_8);
        String dateEncoded = URLEncoder.encode(date, StandardCharsets.UTF_8);
        String hostEncoded = URLEncoder.encode(host, StandardCharsets.UTF_8);

        // 拼接最终URL
        return WS_URL + "?authorization=" + authorizationEncoded
                + "&date=" + dateEncoded
                + "&host=" + hostEncoded;
    }

    /**
     * 构建首帧（参数配置）
     */
    private String buildFirstFrame() {
        return "{"
                + "\"common\":{\"app_id\":\"" + appId + "\"},"
                + "\"business\":{"
                + "\"language\":\"zh_cn\","
                + "\"domain\":\"iat\","
                + "\"accent\":\"mandarin\","
                + "\"vad_eos\":5000,"
                + "\"dwa\":\"wpgs\","
                + "\"nbest\":1,"
                + "\"wbest\":1"
                + "},"
                + "\"data\":{"
                + "\"status\":0,"
                + "\"format\":\"audio/L16;rate=16000\","
                + "\"encoding\":\"raw\","
                + "\"audio\":\"\""
                + "}"
                + "}";
    }

    /**
     * 构建音频帧
     */
    private String buildAudioFrame(String base64Audio) {
        return "{"
                + "\"data\":{"
                + "\"status\":1,"
                + "\"format\":\"audio/L16;rate=16000\","
                + "\"encoding\":\"raw\","
                + "\"audio\":\"" + base64Audio + "\""
                + "}"
                + "}";
    }

    /**
     * 构建结束帧
     */
    private String buildEndFrame() {
        return "{"
                + "\"data\":{"
                + "\"status\":2,"
                + "\"format\":\"audio/L16;rate=16000\","
                + "\"encoding\":\"raw\","
                + "\"audio\":\"\""
                + "}"
                + "}";
    }

    /**
     * 读取PCM文件为字节数组
     */
    private byte[] readPcmFile(String filePath) {
        try {
            File file = new File(filePath);
            byte[] data = new byte[(int) file.length()];
            try (FileInputStream fis = new FileInputStream(file)) {
                fis.read(data);
            }
            return data;
        } catch (Exception e) {
            e.printStackTrace();
            return new byte[0];
        }
    }
}
