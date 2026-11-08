package com.nanji.interview.ai_interview_system.modules.report.service.impl;

import com.nanji.interview.ai_interview_system.common.exception.BusinessException;
import com.nanji.interview.ai_interview_system.modules.interview.dto.InterviewEvaluation;
import com.nanji.interview.ai_interview_system.modules.interview.dto.InterviewTurn;
import com.nanji.interview.ai_interview_system.modules.interview.entity.InterviewSession;
import com.nanji.interview.ai_interview_system.modules.interview.service.InterviewSessionService;
import com.nanji.interview.ai_interview_system.modules.report.service.InterviewReportPdfService;
import com.nanji.interview.ai_interview_system.modules.report.service.ReportService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * PDF 报告生成实现（迁移自 feature/interview-agent 的 InterviewReportPdfServiceImpl，
 * 数据来源由内存 DTO 改为数据库会话/报告/消息）。
 */
@Service
public class InterviewReportPdfServiceImpl implements InterviewReportPdfService {

    /**
     * 中文字体候选路径（均为纯 TTF，PDFBox 可直接加载；TTC 集合字体不支持）。
     * 可在 application.yml 中用 report.pdf.font-path 覆盖。
     */
    private static final List<String> DEFAULT_FONT_CANDIDATES = List.of(
            "C:/Windows/Fonts/simhei.ttf",
            "C:/Windows/Fonts/simkai.ttf",
            "C:/Windows/Fonts/Deng.ttf",
            "C:/Windows/Fonts/simsunb.ttf",
            "C:/Windows/Fonts/SimsunExtG.ttf",
            "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttf",
            "/usr/share/fonts/truetype/droid/DroidSansFallbackFull.ttf"
    );

    @Value("${report.pdf.font-path:}")
    private String fontPath;

    private final InterviewSessionService sessionService;
    private final ReportService reportService;

    public InterviewReportPdfServiceImpl(InterviewSessionService sessionService,
                                         ReportService reportService) {
        this.sessionService = sessionService;
        this.reportService = reportService;
    }

    @Override
    public byte[] generateReportPdf(Long sessionId) {
        InterviewSession session = sessionService.getById(sessionId);
        if (session == null) {
            throw new BusinessException("面试会话不存在");
        }
        InterviewEvaluation evaluation = reportService.getEvaluationBySessionId(sessionId);
        List<InterviewTurn> turns = sessionService.getTurns(sessionId);

        try (PDDocument document = new PDDocument()) {
            PdfWriter writer = new PdfWriter(document, fontPath);
            writer.draw(session.getJobTitle(), session.getDifficulty(), evaluation, turns);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new BusinessException("PDF 生成失败：" + e.getMessage());
        }
    }

    /**
     * 单次 PDF 生成过程的状态载体，避免在单例 Service 上保存可变状态。
     */
    private static class PdfWriter {

        private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
        private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();
        private static final float MARGIN = 50f;
        private static final float BOTTOM_MARGIN = 60f;
        private static final float CONTENT_WIDTH = PAGE_WIDTH - MARGIN * 2;

        private final PDDocument document;
        private final PDType0Font font;
        private PDPageContentStream stream;
        private float y;

        PdfWriter(PDDocument document, String configuredFontPath) throws IOException {
            this.document = document;
            this.font = loadChineseFont(document, configuredFontPath);
            newPage();
        }

        void draw(String jobTitle, String difficulty,
                  InterviewEvaluation ev, List<InterviewTurn> turns) throws IOException {
            drawTitle();
            addParagraph("应聘岗位：" + jobTitle, 12f, 20f);
            addParagraph("面试难度：" + difficultyLabel(difficulty), 12f, 20f);
            y -= 8;

            drawSectionTitle("综合得分");
            ensureSpace(44);
            String scoreText = ev.getScore() + " 分";
            drawCenteredText(scoreText, 28f, y);
            y -= 46;

            drawSectionTitle("总体评价");
            addParagraph(ev.getSummary(), 11f, 18f);
            y -= 8;

            drawSectionTitle("五维能力评分");
            addDimensionRow("专业能力（professionalAbility）", ev.getProfessionalAbility());
            addDimensionRow("知识覆盖度（knowledgeCoverage）", ev.getKnowledgeCoverage());
            addDimensionRow("岗位匹配度（jobMatch）", ev.getJobMatch());
            addDimensionRow("表达沟通（communication）", ev.getCommunication());
            addDimensionRow("逻辑结构（logicalThinking）", ev.getLogicalThinking());
            y -= 8;

            drawSectionTitle("亮点");
            addParagraph(ev.getStrengths(), 11f, 18f);
            y -= 8;

            drawSectionTitle("薄弱点");
            addParagraph(ev.getWeaknesses(), 11f, 18f);
            y -= 8;

            drawSectionTitle("改进建议");
            List<String> suggestions = ev.getSuggestions();
            if (suggestions == null || suggestions.isEmpty()) {
                addParagraph("（无）", 11f, 18f);
            } else {
                int index = 1;
                for (String suggestion : suggestions) {
                    addParagraph(index + ". " + suggestion, 11f, 18f);
                    index++;
                }
            }
            y -= 8;

            drawSectionTitle("问答摘要");
            if (turns == null || turns.isEmpty()) {
                addParagraph("（暂无问答记录）", 11f, 18f);
            } else {
                int index = 1;
                for (InterviewTurn turn : turns) {
                    drawTurnLabel("第 " + index + " 轮 · 面试官提问");
                    addParagraph(turn.getQuestion(), 11f, 17f);
                    drawTurnLabel("候选人回答");
                    addParagraph(turn.getAnswer(), 11f, 17f);
                    y -= 6;
                    index++;
                }
            }

            drawGenerationTime();
            stream.close();
        }

        private void newPage() throws IOException {
            if (stream != null) {
                stream.close();
            }
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            stream = new PDPageContentStream(document, page);
            y = PAGE_HEIGHT - MARGIN;
        }

        private void ensureSpace(float needed) throws IOException {
            if (y - needed < BOTTOM_MARGIN) {
                newPage();
            }
        }

        private void drawTitle() throws IOException {
            stream.beginText();
            stream.setFont(font, 22f);
            String title = "AI 模拟面试报告";
            stream.newLineAtOffset((PAGE_WIDTH - textWidth(title, 22f)) / 2, y);
            stream.showText(escapePdfText(title));
            stream.endText();
            y -= 46;
        }

        private void drawSectionTitle(String title) throws IOException {
            ensureSpace(40);
            y -= 8;
            stream.setNonStrokingColor(new Color(30, 64, 175));
            stream.beginText();
            stream.setFont(font, 15f);
            stream.newLineAtOffset(MARGIN, y);
            stream.showText(escapePdfText(title));
            stream.endText();
            stream.setNonStrokingColor(Color.BLACK);
            y -= 26;
        }

        private void drawCenteredText(String text, float size, float baselineY) throws IOException {
            stream.beginText();
            stream.setFont(font, size);
            stream.newLineAtOffset((PAGE_WIDTH - textWidth(text, size)) / 2, baselineY);
            stream.showText(escapePdfText(text));
            stream.endText();
        }

        private void drawTurnLabel(String text) throws IOException {
            ensureSpace(24);
            stream.setNonStrokingColor(new Color(55, 65, 81));
            stream.beginText();
            stream.setFont(font, 12f);
            stream.newLineAtOffset(MARGIN, y);
            stream.showText(escapePdfText(text));
            stream.endText();
            stream.setNonStrokingColor(Color.BLACK);
            y -= 20;
        }

        private void addDimensionRow(String label, int score) throws IOException {
            ensureSpace(28);
            stream.beginText();
            stream.setFont(font, 12f);
            stream.newLineAtOffset(MARGIN, y);
            stream.showText(escapePdfText(label));
            stream.endText();

            String scoreText = String.valueOf(score);
            stream.beginText();
            stream.setFont(font, 12f);
            stream.newLineAtOffset(MARGIN + 210, y);
            stream.showText(scoreText);
            stream.endText();

            // 分数条
            float barX = MARGIN + 240;
            float barWidth = CONTENT_WIDTH - 240 - 40;
            float clamped = Math.max(0, Math.min(100, score));
            stream.setNonStrokingColor(new Color(226, 232, 240));
            stream.addRect(barX, y - 2, barWidth, 8);
            stream.fill();
            stream.setNonStrokingColor(new Color(59, 130, 246));
            stream.addRect(barX, y - 2, barWidth * clamped / 100f, 8);
            stream.fill();
            stream.setNonStrokingColor(Color.BLACK);
            y -= 28;
        }

        /**
         * 按字符宽度自动换行的段落，超出页面底部时自动分页。
         */
        private void addParagraph(String text, float size, float leading) throws IOException {
            if (text == null || text.isEmpty()) {
                return;
            }
            for (String line : wrapLines(text, size)) {
                ensureSpace(leading);
                if (!line.isEmpty()) {
                    stream.beginText();
                    stream.setFont(font, size);
                    stream.newLineAtOffset(MARGIN, y);
                    stream.showText(escapePdfText(line));
                    stream.endText();
                }
                y -= leading;
            }
        }

        private List<String> wrapLines(String text, float size) throws IOException {
            List<String> lines = new ArrayList<>();
            for (String raw : text.split("\n", -1)) {
                StringBuilder line = new StringBuilder();
                for (int i = 0; i < raw.length(); i++) {
                    char c = raw.charAt(i);
                    if (line.length() > 0
                            && textWidth(line.toString() + c, size) > CONTENT_WIDTH) {
                        lines.add(line.toString());
                        line.setLength(0);
                    }
                    line.append(c);
                }
                lines.add(line.toString());
            }
            return lines;
        }

        private float textWidth(String text, float size) throws IOException {
            return font.getStringWidth(text) / 1000f * size;
        }

        private void drawGenerationTime() throws IOException {
            ensureSpace(30);
            y -= 12;
            String time = "生成时间："
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            stream.setNonStrokingColor(Color.GRAY);
            stream.beginText();
            stream.setFont(font, 10f);
            stream.newLineAtOffset(PAGE_WIDTH - MARGIN - textWidth(time, 10f), y);
            stream.showText(escapePdfText(time));
            stream.endText();
        }

        private static String difficultyLabel(String difficulty) {
            if (difficulty == null) {
                return "未知";
            }
            return switch (difficulty.toLowerCase()) {
                case "easy" -> "简单";
                case "medium" -> "中等";
                case "hard" -> "困难";
                default -> difficulty;
            };
        }

        /**
         * PDF 文本中的特殊字符需要转义，否则 ( ) \ 会破坏 PDF 语法。
         */
        private static String escapePdfText(String text) {
            return text
                    .replace("\\", "\\\\")
                    .replace("(", "\\(")
                    .replace(")", "\\)");
        }
    }

    private static PDType0Font loadChineseFont(PDDocument document, String configuredFontPath) throws IOException {
        if (configuredFontPath != null && !configuredFontPath.isBlank()) {
            File file = new File(configuredFontPath);
            if (!file.isFile()) {
                throw new IOException("配置的中文字体文件不存在：" + configuredFontPath);
            }
            return loadFontFile(document, file);
        }
        for (String candidate : DEFAULT_FONT_CANDIDATES) {
            File file = new File(candidate);
            if (file.isFile()) {
                try {
                    return loadFontFile(document, file);
                } catch (IOException ignored) {
                    // 尝试下一个候选字体
                }
            }
        }
        throw new IOException("未找到可用的中文字体，请在 application.yml 中配置 report.pdf.font-path");
    }

    private static PDType0Font loadFontFile(PDDocument document, File file) throws IOException {
        try (InputStream in = new FileInputStream(file)) {
            return PDType0Font.load(document, in);
        }
    }
}
