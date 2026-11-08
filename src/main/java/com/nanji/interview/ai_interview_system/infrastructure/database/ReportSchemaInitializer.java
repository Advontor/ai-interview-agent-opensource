package com.nanji.interview.ai_interview_system.infrastructure.database;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Keeps report persistence compatible with databases created before the
 * extended interview report fields were introduced.
 *
 * <p>The migration is intentionally metadata-driven instead of relying on
 * MySQL's version-specific {@code ADD COLUMN IF NOT EXISTS} syntax. It is
 * therefore safe to run on every application start and on older MySQL 8
 * installations.</p>
 */
@Component
public class ReportSchemaInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    private static final Map<String, String> REPORT_COLUMNS = new LinkedHashMap<>();

    static {
        REPORT_COLUMNS.put("summary", "TEXT COMMENT '总体评价'");
        REPORT_COLUMNS.put("professional_ability", "INT DEFAULT NULL COMMENT '专业能力 0-100'");
        REPORT_COLUMNS.put("knowledge_coverage", "INT DEFAULT NULL COMMENT '知识覆盖度 0-100'");
        REPORT_COLUMNS.put("job_match", "INT DEFAULT NULL COMMENT '岗位匹配度 0-100'");
        REPORT_COLUMNS.put("communication", "INT DEFAULT NULL COMMENT '表达沟通 0-100'");
        REPORT_COLUMNS.put("logical_thinking", "INT DEFAULT NULL COMMENT '逻辑结构 0-100'");
        REPORT_COLUMNS.put("question_reviews_json", "LONGTEXT COMMENT '逐题评价 JSON'");
        REPORT_COLUMNS.put("transcript_json", "LONGTEXT COMMENT '完整问答 JSON'");
        REPORT_COLUMNS.put("chart_data_json", "LONGTEXT COMMENT '可视化图表数据 JSON'");
        REPORT_COLUMNS.put("resource_recommendations_json", "LONGTEXT COMMENT '学习资源推荐 JSON'");
        REPORT_COLUMNS.put("emotion_timeline_json", "LONGTEXT COMMENT '情绪时序分析 JSON'");
        REPORT_COLUMNS.put("star_analysis_json", "LONGTEXT COMMENT 'STAR 分析 JSON'");
    }

    public ReportSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!tableExists("report")) {
            return;
        }

        REPORT_COLUMNS.forEach(this::addColumnIfMissing);
    }

    private void addColumnIfMissing(String columnName, String definition) {
        if (columnExists("report", columnName)) {
            return;
        }

        jdbcTemplate.execute("ALTER TABLE `report` ADD COLUMN `" + columnName + "` " + definition);
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                """,
                Integer.class,
                tableName
        );
        return count != null && count > 0;
    }

    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND column_name = ?
                """,
                Integer.class,
                tableName,
                columnName
        );
        return count != null && count > 0;
    }
}
