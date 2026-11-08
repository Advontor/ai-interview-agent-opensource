package com.nanji.interview.ai_interview_system.modules.resume.parser;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Locale;

/**
 * 简历文件读取工具类（迁移自 feature/resume-parser，并按 PDFBox 3.x 调整加载方式）。
 *
 * 职责（仅此而已）：把 PDF / Word 文件提取为纯文字字符串。
 *  - PDF（文字型）：Apache PDFBox
 *  - Word（.docx）：Apache POI
 *  - .doc 旧版 / 其它格式：第一版不支持，抛出异常交由上层记录 warning
 *
 * 注意：扫描件 PDF（纯图片）提取出的文字会极短或为空，由上层解析器判断并标记 warning。
 */
public final class ResumeFileExtractor {

    private ResumeFileExtractor() {
        // 工具类不允许实例化
    }

    /**
     * 根据文件路径提取纯文字内容。
     *
     * @param filePath 简历文件绝对路径
     * @return 提取出的纯文字
     * @throws IOException              文件读取失败 / 文件损坏
     * @throws IllegalArgumentException 不支持的文件类型
     */
    public static String extractText(String filePath) throws IOException {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("文件路径为空");
        }
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IOException("文件不存在：" + filePath);
        }

        String name = file.getName().toLowerCase(Locale.ROOT);
        if (name.endsWith(".pdf")) {
            return extractFromPdf(file);
        } else if (name.endsWith(".docx")) {
            return extractFromDocx(file);
        } else if (name.endsWith(".doc")) {
            throw new IllegalArgumentException("旧版 .doc 格式第一版暂不支持，请另存为 .docx 后重试");
        } else {
            throw new IllegalArgumentException("不支持的文件类型，仅支持 .pdf / .docx：" + name);
        }
    }

    /**
     * 根据文件名与文件字节内容提取纯文字（供上传接口直接使用，无需落盘）。
     *
     * @param fileName 原始文件名（用于判断文件类型）
     * @param content  文件字节内容
     * @return 提取出的纯文字
     * @throws IOException              文件读取失败 / 文件损坏
     * @throws IllegalArgumentException 不支持的文件类型
     */
    public static String extractText(String fileName, byte[] content) throws IOException {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("文件名为空");
        }
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("文件内容为空");
        }
        String name = fileName.toLowerCase(Locale.ROOT);
        if (name.endsWith(".pdf")) {
            return extractFromPdf(content);
        } else if (name.endsWith(".docx")) {
            return extractFromDocx(content);
        } else if (name.endsWith(".doc")) {
            throw new IllegalArgumentException("旧版 .doc 格式第一版暂不支持，请另存为 .docx 后重试");
        } else {
            throw new IllegalArgumentException("不支持的文件类型，仅支持 .pdf / .docx：" + name);
        }
    }

    /**
     * 使用 PDFBox 提取文字型 PDF 的文本内容。
     */
    private static String extractFromPdf(File file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file)) {
            return extractPdfText(document);
        }
    }

    private static String extractFromPdf(byte[] content) throws IOException {
        try (PDDocument document = Loader.loadPDF(content)) {
            return extractPdfText(document);
        }
    }

    private static String extractPdfText(PDDocument document) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        // 按页面顺序提取，保留换行
        stripper.setSortByPosition(true);
        String text = stripper.getText(document);
        return text == null ? "" : text;
    }

    /**
     * 使用 Apache POI 提取 .docx 的文本内容（包含段落和表格中的文字）。
     */
    private static String extractFromDocx(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            return extractDocxText(fis);
        }
    }

    private static String extractFromDocx(byte[] content) throws IOException {
        return extractDocxText(new ByteArrayInputStream(content));
    }

    private static String extractDocxText(java.io.InputStream in) throws IOException {
        try (XWPFDocument document = new XWPFDocument(in);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            String text = extractor.getText();
            return text == null ? "" : text;
        }
    }
}
