package com.nanji.interview.ai_interview_system.modules.voice.util;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 音频转码工具（基于FFmpeg）
 * <p>
 * 把前端发来的WebM/Opus音频转码成讯飞要求的PCM格式：
 * - 采样率：16000 Hz
 * - 声道：单声道
 * - 位深：16位
 * <p>
 * 注意：运行环境需要安装FFmpeg，并配置到环境变量PATH中。
 * Windows：下载ffmpeg，把bin目录加到PATH
 * Linux：apt install ffmpeg 或 yum install ffmpeg
 */
@Component
public class AudioConverter {

    /**
     * 把WebM/Opus转码成16kHz 16位单声道PCM
     *
     * @param inputPath  输入文件路径（WebM/Opus）
     * @param outputPath 输出文件路径（PCM）
     * @return 转码是否成功
     */
    public boolean convertToPcm(String inputPath, String outputPath) {
        try {
            // 检查输入文件是否存在
            File inputFile = new File(inputPath);
            if (!inputFile.exists()) {
                System.err.println("[音频转码] 输入文件不存在：" + inputPath);
                return false;
            }

            // 构建FFmpeg命令
            // -i 输入文件
            // -ar 16000 采样率16kHz
            // -ac 1 单声道
            // -sample_fmt s16 16位深度
            // -f s16le 输出PCM格式
            // -y 覆盖输出文件
            String[] command = {
                    "ffmpeg",
                    "-i", inputPath,
                    "-ar", "16000",
                    "-ac", "1",
                    "-sample_fmt", "s16",
                    "-f", "s16le",
                    "-y",
                    outputPath
            };

            System.out.println("[音频转码] 执行命令：" + String.join(" ", command));

            // 执行命令
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // 读取输出（必须读取，否则进程会卡住）
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                // FFmpeg输出比较多，可以不打印，或者只打印错误
                // System.out.println(line);
            }
            reader.close();

            // 等待命令执行完成
            int exitCode = process.waitFor();

            // 检查输出文件是否生成
            File outputFile = new File(outputPath);
            boolean success = exitCode == 0 && outputFile.exists() && outputFile.length() > 0;

            if (success) {
                System.out.println("[音频转码] 转码成功，输出文件：" + outputPath + "，大小：" + outputFile.length() + "字节");
            } else {
                System.err.println("[音频转码] 转码失败，exitCode=" + exitCode);
            }

            return success;

        } catch (Exception e) {
            System.err.println("[音频转码] 转码出错：" + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 检查FFmpeg是否可用
     *
     * @return 是否可用
     */
    public boolean isFfmpegAvailable() {
        try {
            Process process = new ProcessBuilder("ffmpeg", "-version").start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
