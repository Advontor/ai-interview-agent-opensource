-- =============================================================
-- 给 report 表新增「应聘岗位」「难度」「逐题评分与点评」三列
-- 用途：报告页需要展示本场面试的岗位/难度（不同岗位的报告据此区分），
--       以及每道题的得分与 AI 点评。
-- 幂等性：MySQL 8.0 不支持 ADD COLUMN IF NOT EXISTS，
--        重复执行会报 Duplicate column name，属预期，可忽略。
-- 执行：mysql -u ai_interview -p ai_interview < sql/add_report_question_reviews.sql
-- =============================================================

ALTER TABLE report
    ADD COLUMN job_title VARCHAR(128) NULL COMMENT '本次面试的应聘岗位' AFTER logical_thinking,
    ADD COLUMN difficulty VARCHAR(32) NULL COMMENT '本次面试难度：EASY/MEDIUM/HARD' AFTER job_title,
    ADD COLUMN question_reviews TEXT NULL COMMENT '逐题评分与点评（JSON 数组）' AFTER difficulty;
