-- =============================================================
-- AI 模拟面试系统 · 第一周集成 DDL
-- 数据库：ai_interview（MySQL 8，utf8mb4）
-- 本文件为 UTF-8 编码；执行前确保客户端连接字符集为 utf8mb4
-- 用法：
--   1) 全新环境：直接执行本文件（CREATE TABLE IF NOT EXISTS 幂等）
--   2) 已有旧表：执行文件末尾的 ALTER TABLE 语句补齐新增字段
-- =============================================================

SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS ai_interview DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE ai_interview;

-- -------------------------------------------------------------
-- 用户表
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user` (
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
CREATE TABLE IF NOT EXISTS resume (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id        BIGINT       DEFAULT NULL COMMENT '用户id，关联user表',
    resume_name    VARCHAR(255) DEFAULT NULL COMMENT '简历名称',
    resume_content LONGTEXT     COMMENT '简历内容（原文/结构化 JSON）',
    create_time    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB COMMENT = '简历表';

-- -------------------------------------------------------------
-- 面试会话表
-- 第一周新增字段：difficulty、question_index、resume_json
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS interview_session (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id        BIGINT       DEFAULT 1 COMMENT '用户id，关联user表（无登录系统时默认 1，否则历史面试查不到）',
    job_title      VARCHAR(255) NOT NULL COMMENT '面试岗位名称',
    difficulty     VARCHAR(32)  DEFAULT 'MEDIUM' COMMENT '面试难度：EASY/MEDIUM/HARD',
    status         VARCHAR(32)  DEFAULT 'ONGOING' COMMENT '面试状态：ONGOING / FINISHED',
    question_index INT          DEFAULT 1 COMMENT '当前题目序号（从1开始）',
    resume_json    LONGTEXT     COMMENT '简历结构化数据（ResumeDTO JSON），供生成题目与评价使用',
    agent_state_json LONGTEXT   COMMENT 'Agent 面试状态（AgentState JSON）：有效题数/当前知识点/追问次数/已覆盖知识点等',
    create_time    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    end_time       DATETIME     DEFAULT NULL COMMENT '结束时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB COMMENT = '面试会话表';

-- -------------------------------------------------------------
-- 面试消息表（AI 题目与用户回答均存于此）
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS interview_message (
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
-- 原有字段：score / advantage / disadvantage / suggestion（建议以 JSON 字符串数组存储）
-- 第一周新增字段：summary、professional_ability、knowledge_coverage、
--                job_match、communication、logical_thinking
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS report (
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
    job_title            VARCHAR(128) DEFAULT NULL COMMENT '本次面试的应聘岗位',
    difficulty           VARCHAR(32)  DEFAULT NULL COMMENT '本次面试难度：EASY/MEDIUM/HARD',
    question_reviews     TEXT     COMMENT '逐题评分与点评（JSON 数组）',
    create_time          DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_session_id (session_id)
) ENGINE = InnoDB COMMENT = '面试报告表';

-- =============================================================
-- 已有旧表升级：按需取消注释并执行（MySQL 不支持 ADD COLUMN IF NOT EXISTS，
-- 若已执行过会报列重复错误，属正常现象）
-- =============================================================
-- ALTER TABLE interview_session ADD COLUMN difficulty VARCHAR(32) DEFAULT 'MEDIUM' COMMENT '面试难度：EASY/MEDIUM/HARD' AFTER job_title;
-- ALTER TABLE interview_session ADD COLUMN question_index INT DEFAULT 1 COMMENT '当前题目序号（从1开始）' AFTER status;
-- ALTER TABLE interview_session ADD COLUMN resume_json LONGTEXT COMMENT '简历结构化数据（ResumeDTO JSON）' AFTER question_index;
-- ALTER TABLE interview_session ADD COLUMN agent_state_json LONGTEXT COMMENT 'Agent 面试状态（AgentState JSON）' AFTER resume_json;
-- ALTER TABLE interview_session ALTER COLUMN status SET DEFAULT 'ONGOING';
-- ALTER TABLE report ADD COLUMN summary TEXT COMMENT '总体评价' AFTER suggestion;
-- ALTER TABLE report ADD COLUMN professional_ability INT COMMENT '专业能力 0-100' AFTER summary;
-- ALTER TABLE report ADD COLUMN knowledge_coverage INT COMMENT '知识覆盖度 0-100' AFTER professional_ability;
-- ALTER TABLE report ADD COLUMN job_match INT COMMENT '岗位匹配度 0-100' AFTER knowledge_coverage;
-- ALTER TABLE report ADD COLUMN communication INT COMMENT '表达沟通 0-100' AFTER job_match;
-- ALTER TABLE report ADD COLUMN logical_thinking INT COMMENT '逻辑结构 0-100' AFTER communication;
-- ALTER TABLE report ADD COLUMN job_title VARCHAR(128) COMMENT '本次面试的应聘岗位' AFTER logical_thinking;
-- ALTER TABLE report ADD COLUMN difficulty VARCHAR(32) COMMENT '本次面试难度：EASY/MEDIUM/HARD' AFTER job_title;
-- ALTER TABLE report ADD COLUMN question_reviews TEXT COMMENT '逐题评分与点评（JSON 数组）' AFTER difficulty;
