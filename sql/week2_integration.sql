-- =============================================================
-- AI 模拟面试系统 · 第二周集成 DDL
-- 数据库：ai_interview（MySQL 8，utf8mb4）
-- 本文件为 UTF-8 编码；执行前确保客户端连接字符集为 utf8mb4
-- 用法：全新环境直接执行（CREATE TABLE IF NOT EXISTS 幂等）
-- =============================================================

SET NAMES utf8mb4;
USE ai_interview;

-- -------------------------------------------------------------
-- 知识图谱节点表（岗位 / 技能 / 知识点）
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS graph_node (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    node_type   VARCHAR(32)  NOT NULL COMMENT '节点类型：JOB岗位 / SKILL技能 / KNOWLEDGE知识点',
    name        VARCHAR(255) NOT NULL COMMENT '节点名称',
    description TEXT         COMMENT '节点描述',
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_node_type (node_type),
    KEY idx_name (name)
) ENGINE = InnoDB COMMENT = '知识图谱节点表';

-- -------------------------------------------------------------
-- 知识图谱关系表（节点之间的关联）
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS graph_edge (
    id             BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    source_node_id BIGINT      NOT NULL COMMENT '源节点id，关联graph_node',
    target_node_id BIGINT      NOT NULL COMMENT '目标节点id，关联graph_node',
    relation_type  VARCHAR(32) NOT NULL COMMENT '关系类型：REQUIRES需要 / CONTAINS包含 / PREREQUISITE前置',
    weight         INT         DEFAULT 0 COMMENT '关系权重（可选）',
    created_at     DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_source_node (source_node_id),
    KEY idx_target_node (target_node_id),
    KEY idx_relation_type (relation_type)
) ENGINE = InnoDB COMMENT = '知识图谱关系表';

-- -------------------------------------------------------------
-- 学习资源表（Agent 根据面试薄弱点推送学习资源用）
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS learning_resource (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    title         VARCHAR(255) NOT NULL COMMENT '资源标题',
    url           VARCHAR(500) NOT NULL COMMENT '资源链接',
    category      VARCHAR(64)  DEFAULT NULL COMMENT '分类：Java基础 / 算法 / 项目经验 / 操作系统等',
    related_skill VARCHAR(128) DEFAULT NULL COMMENT '关联技能标签（用于匹配面试薄弱点）',
    description   TEXT         COMMENT '资源描述',
    created_at    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_category (category),
    KEY idx_related_skill (related_skill)
) ENGINE = InnoDB COMMENT = '学习资源表';

-- -------------------------------------------------------------
-- 情绪特征表（语音面试进阶功能：记录每轮回答的情绪分析结果）
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS emotion_feature (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    session_id   BIGINT       NOT NULL COMMENT '面试会话id，关联interview_session',
    turn_index   INT          DEFAULT NULL COMMENT '第几轮回答',
    emotion_type VARCHAR(32)  DEFAULT NULL COMMENT '情绪类型：紧张 / 自信 / 犹豫 / 平静等',
    confidence   DECIMAL(5,2) DEFAULT NULL COMMENT '情绪识别置信度 0-100',
    audio_path   VARCHAR(255) DEFAULT NULL COMMENT '对应音频文件路径（可选）',
    created_at   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_session_id (session_id),
    KEY idx_emotion_type (emotion_type)
) ENGINE = InnoDB COMMENT = '情绪特征表';

-- -------------------------------------------------------------
-- RAG知识库分块表（文档分块存储，用于AI面试检索增强）
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS knowledge_chunk (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    doc_title   VARCHAR(255) NOT NULL COMMENT '文档标题',
    doc_source  VARCHAR(500) DEFAULT '' COMMENT '来源链接或说明',
    position    VARCHAR(100) NOT NULL COMMENT '岗位标签',
    chunk_index INT          NOT NULL COMMENT '分块序号，从0开始',
    content     TEXT         NOT NULL COMMENT '分块文本内容',
    embedding   TEXT         NOT NULL COMMENT '逗号分隔的向量字符串',
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    INDEX idx_position (position)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COMMENT='RAG知识库分块表';
