-- =============================================================
-- AI 模拟面试系统 · 数据库从零初始化脚本（第一周集成版）
-- 数据库：ai_interview（MySQL 8，utf8mb4）
-- 本文件为 UTF-8 编码；执行前确保客户端连接字符集为 utf8mb4
--
-- 说明：
--   1) 本脚本会创建数据库 ai_interview 及全部 5 张业务表
--      （user / resume / interview_session / interview_message / report），
--      结构与当前代码中的 Entity 一一对应，含 agent_state_json 等全部字段。
--   2) 脚本会先 DROP TABLE IF EXISTS 再重建：重复执行会清空表内数据，
--      仅在"全新/可重建"环境执行，切勿在存有重要数据的库上运行。
--   3) 不含任何业务数据、密码或密钥；系统运行不需要预置数据行
--      （第一周无登录，面试会话等数据全部由接口运行时写入）。
--   4) 旧表升级场景（保留数据只加字段）请改用 sql/week1_integration.sql。
--
-- 执行方式（项目根目录）：
--   mysql -u root -p < sql/init.sql
-- =============================================================

SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS ai_interview DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE ai_interview;

-- -------------------------------------------------------------
-- 先按依赖顺序清理旧表（子表在前）
-- -------------------------------------------------------------
DROP TABLE IF EXISTS report;
DROP TABLE IF EXISTS interview_message;
DROP TABLE IF EXISTS interview_session;
DROP TABLE IF EXISTS resume;
DROP TABLE IF EXISTS `user`;

-- -------------------------------------------------------------
-- 用户表
-- -------------------------------------------------------------
CREATE TABLE `user` (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username    VARCHAR(64)  DEFAULT NULL COMMENT '用户名',
    password    VARCHAR(128) DEFAULT NULL COMMENT '密码',
    real_name   VARCHAR(64)  DEFAULT NULL COMMENT '真实姓名',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB COMMENT = '用户表';

-- -------------------------------------------------------------
-- 简历表
-- -------------------------------------------------------------
CREATE TABLE resume (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id        BIGINT       DEFAULT NULL COMMENT '用户id，关联user表',
    resume_name    VARCHAR(255) DEFAULT NULL COMMENT '简历名称',
    resume_content LONGTEXT     COMMENT '简历内容（原文/结构化 JSON）',
    structured_json LONGTEXT    COMMENT '用户确认后的 ResumeDTO JSON',
    score_json      LONGTEXT    COMMENT '规则评分和自动填充建议 JSON',
    target_role     VARCHAR(128) DEFAULT NULL COMMENT '用户确认后的目标岗位',
    version         INT          NOT NULL DEFAULT 1 COMMENT '简历确认版本号',
    create_time    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB COMMENT = '简历表';

-- -------------------------------------------------------------
-- 面试会话表
-- 字段：difficulty、question_index、resume_json、agent_state_json
-- -------------------------------------------------------------
CREATE TABLE interview_session (
    id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id          BIGINT       DEFAULT NULL COMMENT '用户id，关联user表（第一周无登录，可为空）',
    job_title        VARCHAR(255) NOT NULL COMMENT '面试岗位名称',
    difficulty       VARCHAR(32)  DEFAULT 'MEDIUM' COMMENT '面试难度：EASY/MEDIUM/HARD',
    mode             VARCHAR(16)  DEFAULT 'TEXT' COMMENT '面试模式：TEXT/VOICE/VIDEO',
    target_level     VARCHAR(64)  DEFAULT NULL COMMENT '目标职级',
    config_json      LONGTEXT     COMMENT '面试配置 JSON',
    status           VARCHAR(32)  DEFAULT 'ONGOING' COMMENT '面试状态：ONGOING / FINISHED',
    question_index   INT          DEFAULT 1 COMMENT '当前题目序号（从1开始）',
    resume_json      LONGTEXT     COMMENT '简历结构化数据（ResumeDTO JSON），供生成题目与评价使用',
    agent_state_json LONGTEXT     COMMENT 'Agent 面试状态（AgentState JSON）：有效题数/当前知识点/追问次数/已覆盖知识点等',
    create_time      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    end_time         DATETIME     DEFAULT NULL COMMENT '结束时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB COMMENT = '面试会话表';

-- -------------------------------------------------------------
-- 面试消息表（AI 题目与用户回答均存于此）
-- -------------------------------------------------------------
CREATE TABLE interview_message (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    session_id  BIGINT      NOT NULL COMMENT '所属会话id，关联interview_session',
    sender      VARCHAR(16) NOT NULL COMMENT '消息发送方：user / ai',
    content     LONGTEXT    COMMENT '消息内容',
    create_time DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_session_id (session_id)
) ENGINE = InnoDB COMMENT = '面试消息表';

-- -------------------------------------------------------------
-- 面试报告表
-- 原有字段：score / advantage / disadvantage / suggestion
-- 新增字段：summary、professional_ability、knowledge_coverage、
--          job_match、communication、logical_thinking
-- -------------------------------------------------------------
CREATE TABLE report (
    id                   BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    session_id           BIGINT   NOT NULL COMMENT '面试会话id，关联interview_session',
    score                INT      DEFAULT NULL COMMENT '综合得分 0-100',
    advantage            TEXT     COMMENT '亮点（对应评价 strengths）',
    disadvantage         TEXT     COMMENT '薄弱点（对应评价 weaknesses）',
    suggestion           TEXT     COMMENT '改进建议（JSON 字符串数组，对应评价 suggestions）',
    summary              TEXT     COMMENT '总体评价',
    professional_ability INT      DEFAULT NULL COMMENT '专业能力 0-100',
    knowledge_coverage   INT      DEFAULT NULL COMMENT '知识覆盖度 0-100',
    job_match            INT      DEFAULT NULL COMMENT '岗位匹配度 0-100',
    communication        INT      DEFAULT NULL COMMENT '表达沟通 0-100',
    logical_thinking     INT      DEFAULT NULL COMMENT '逻辑结构 0-100',
    question_reviews_json LONGTEXT COMMENT '逐题评价 JSON',
    transcript_json       LONGTEXT COMMENT '完整问答 JSON',
    chart_data_json       LONGTEXT COMMENT '可视化图表数据 JSON',
    resource_recommendations_json LONGTEXT COMMENT '学习资源推荐 JSON',
    emotion_timeline_json LONGTEXT COMMENT '情绪时序分析 JSON',
    star_analysis_json    LONGTEXT COMMENT 'STAR 分析 JSON',
    create_time          DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_session_id (session_id)
) ENGINE = InnoDB COMMENT = '面试报告表';

-- =============================================================
-- 无初始化数据：第一周流程无登录、无预置数据依赖，
-- 面试会话/消息/报告全部由接口运行时写入。
-- =============================================================
