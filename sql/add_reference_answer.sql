-- ============================================================
-- ai-interview-agent · 新功能迁移：面试消息表加 reference_answer
-- 适用版本：v1.1+ （2026-10-03 加入"参考答案 / 自我介绍开场"功能）
-- 已部署过此 SQL 的可安全重跑：再次执行会因列已存在报错（信息可忽略）
-- ============================================================

ALTER TABLE interview_message
    ADD COLUMN reference_answer LONGTEXT
        COMMENT '参考答案（仅 AI 题目的消息在「基于知识库面试」模式下从知识库命中后填入）'
        AFTER content;
