package com.nanji.interview.ai_interview_system.modules.interview.media;

import com.nanji.interview.ai_interview_system.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/** 前端根据能力探测决定展示文字、语音或视频模式。 */
@RestController
@RequestMapping("/interview/media")
public class MediaController {
    @GetMapping("/capabilities")
    public Result<Map<String, Object>> capabilities() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("text", true);
        value.put("browserCapture", true);
        value.put("webSocket", true);
        value.put("serverAsr", false);
        value.put("serverTts", false);
        value.put("videoPreview", true);
        value.put("videoUpload", false);
        value.put("notice", "当前版本支持浏览器采集和媒体通道回执；正式 ASR/TTS 与媒体存储适配器待接入");
        return Result.success(value);
    }
}
