-- AI 面试系统第二阶段：结构化简历和评分字段升级
-- 适用于已经执行过 sql/week1_integration.sql 的数据库。
-- 执行前请先备份数据库，字段只新增，不删除既有数据。
USE ai_interview;

ALTER TABLE resume
    ADD COLUMN structured_json LONGTEXT NULL COMMENT '用户确认后的 ResumeDTO JSON' AFTER resume_content,
    ADD COLUMN score_json LONGTEXT NULL COMMENT '规则评分和自动填充建议 JSON' AFTER structured_json,
    ADD COLUMN target_role VARCHAR(128) NULL COMMENT '用户确认后的目标岗位' AFTER score_json,
    ADD COLUMN version INT NOT NULL DEFAULT 1 COMMENT '简历确认版本号' AFTER target_role;
