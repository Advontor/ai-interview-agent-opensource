-- =============================================================
-- 知识图谱数据（graph_node / graph_edge）
-- 由 scripts/gen_knowledge_graph_sql.py 自动生成，请勿手改
-- 数据来源：interview_rag_structured_v1.csv（20000 条面试问答的标签统计）
-- 执行：mysql -u root -p ai_interview < sql/knowledge_graph_data.sql
-- =============================================================

SET NAMES utf8mb4;
USE ai_interview;

CREATE TABLE IF NOT EXISTS graph_node (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    node_type   VARCHAR(32)  NOT NULL COMMENT '节点类型：JOB岗位 / SKILL技能 / KNOWLEDGE知识点',
    name        VARCHAR(255) NOT NULL COMMENT '节点名称',
    description TEXT         COMMENT '节点描述',
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_node_type (node_type),
    KEY idx_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '知识图谱节点表';

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
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '知识图谱关系表';

TRUNCATE TABLE graph_edge;
TRUNCATE TABLE graph_node;

-- ---------- 节点 ----------
-- 岗位节点（JOB）
INSERT INTO graph_node (id, node_type, name, description) VALUES (1, 'JOB', '通用面试', '该岗位面试题共 5209 道');
INSERT INTO graph_node (id, node_type, name, description) VALUES (2, 'JOB', '后端开发', '该岗位面试题共 3707 道');
INSERT INTO graph_node (id, node_type, name, description) VALUES (3, 'JOB', '产品经理', '该岗位面试题共 3303 道');
INSERT INTO graph_node (id, node_type, name, description) VALUES (4, 'JOB', '数据分析', '该岗位面试题共 1647 道');
INSERT INTO graph_node (id, node_type, name, description) VALUES (5, 'JOB', '运维/SRE/DevOps', '该岗位面试题共 924 道');
INSERT INTO graph_node (id, node_type, name, description) VALUES (6, 'JOB', '设计/UIUX', '该岗位面试题共 907 道');
INSERT INTO graph_node (id, node_type, name, description) VALUES (7, 'JOB', '测试开发/QA', '该岗位面试题共 808 道');
INSERT INTO graph_node (id, node_type, name, description) VALUES (8, 'JOB', '运营/市场', '该岗位面试题共 737 道');
INSERT INTO graph_node (id, node_type, name, description) VALUES (9, 'JOB', '算法/机器学习', '该岗位面试题共 577 道');
INSERT INTO graph_node (id, node_type, name, description) VALUES (10, 'JOB', '前端开发', '该岗位面试题共 543 道');
INSERT INTO graph_node (id, node_type, name, description) VALUES (11, 'JOB', '项目经理', '该岗位面试题共 512 道');
INSERT INTO graph_node (id, node_type, name, description) VALUES (12, 'JOB', '移动开发', '该岗位面试题共 260 道');
INSERT INTO graph_node (id, node_type, name, description) VALUES (13, 'JOB', '安全', '该岗位面试题共 205 道');
INSERT INTO graph_node (id, node_type, name, description) VALUES (14, 'JOB', '人力资源', '该岗位面试题共 201 道');
INSERT INTO graph_node (id, node_type, name, description) VALUES (15, 'JOB', '销售/商务', '该岗位面试题共 200 道');
INSERT INTO graph_node (id, node_type, name, description) VALUES (16, 'JOB', '财务/审计', '该岗位面试题共 178 道');
INSERT INTO graph_node (id, node_type, name, description) VALUES (17, 'JOB', '嵌入式/硬件', '该岗位面试题共 71 道');
INSERT INTO graph_node (id, node_type, name, description) VALUES (18, 'JOB', '全栈开发', '该岗位面试题共 11 道');

-- 能力维度节点（SKILL）
INSERT INTO graph_node (id, node_type, name, description) VALUES (101, 'SKILL', '业务理解', '出现于 11008 道面试题');
INSERT INTO graph_node (id, node_type, name, description) VALUES (102, 'SKILL', '数据能力', '出现于 8146 道面试题');
INSERT INTO graph_node (id, node_type, name, description) VALUES (103, 'SKILL', '项目经验', '出现于 7763 道面试题');
INSERT INTO graph_node (id, node_type, name, description) VALUES (104, 'SKILL', '问题解决', '出现于 6717 道面试题');
INSERT INTO graph_node (id, node_type, name, description) VALUES (105, 'SKILL', '沟通协作', '出现于 5641 道面试题');
INSERT INTO graph_node (id, node_type, name, description) VALUES (106, 'SKILL', '工程能力', '出现于 5508 道面试题');
INSERT INTO graph_node (id, node_type, name, description) VALUES (107, 'SKILL', '学习能力', '出现于 3620 道面试题');
INSERT INTO graph_node (id, node_type, name, description) VALUES (108, 'SKILL', '领导力', '出现于 2290 道面试题');
INSERT INTO graph_node (id, node_type, name, description) VALUES (109, 'SKILL', '通用能力', '出现于 1462 道面试题');
INSERT INTO graph_node (id, node_type, name, description) VALUES (110, 'SKILL', '抗压能力', '出现于 891 道面试题');

-- 考察场景节点（KNOWLEDGE）
INSERT INTO graph_node (id, node_type, name, description) VALUES (201, 'KNOWLEDGE', '业务分析与策略', '出现于 7476 道面试题');
INSERT INTO graph_node (id, node_type, name, description) VALUES (202, 'KNOWLEDGE', '项目经历深挖', '出现于 7432 道面试题');
INSERT INTO graph_node (id, node_type, name, description) VALUES (203, 'KNOWLEDGE', '通用场景', '出现于 4600 道面试题');
INSERT INTO graph_node (id, node_type, name, description) VALUES (204, 'KNOWLEDGE', '行为面试(STAR)', '出现于 4253 道面试题');
INSERT INTO graph_node (id, node_type, name, description) VALUES (205, 'KNOWLEDGE', '技术原理与系统设计', '出现于 3602 道面试题');
INSERT INTO graph_node (id, node_type, name, description) VALUES (206, 'KNOWLEDGE', '实习经历复盘', '出现于 1779 道面试题');
INSERT INTO graph_node (id, node_type, name, description) VALUES (207, 'KNOWLEDGE', '合规与安全', '出现于 1418 道面试题');
INSERT INTO graph_node (id, node_type, name, description) VALUES (208, 'KNOWLEDGE', '反问与职业规划', '出现于 955 道面试题');
INSERT INTO graph_node (id, node_type, name, description) VALUES (209, 'KNOWLEDGE', '自我介绍与动机', '出现于 240 道面试题');

-- ---------- 关系 ----------
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (1, 1, 103, 'REQUIRES', 1976);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (2, 1, 101, 'REQUIRES', 1721);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (3, 1, 107, 'REQUIRES', 1640);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (4, 1, 105, 'REQUIRES', 1496);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (5, 1, 203, 'CONTAINS', 1883);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (6, 1, 202, 'CONTAINS', 1726);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (7, 1, 204, 'CONTAINS', 1110);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (8, 2, 102, 'REQUIRES', 2291);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (9, 2, 106, 'REQUIRES', 2151);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (10, 2, 101, 'REQUIRES', 2129);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (11, 2, 104, 'REQUIRES', 1595);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (12, 2, 202, 'CONTAINS', 1551);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (13, 2, 201, 'CONTAINS', 1404);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (14, 2, 204, 'CONTAINS', 1005);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (15, 3, 101, 'REQUIRES', 2379);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (16, 3, 105, 'REQUIRES', 1475);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (17, 3, 103, 'REQUIRES', 1415);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (18, 3, 104, 'REQUIRES', 1068);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (19, 3, 201, 'CONTAINS', 1689);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (20, 3, 202, 'CONTAINS', 1371);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (21, 3, 204, 'CONTAINS', 725);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (22, 4, 102, 'REQUIRES', 1513);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (23, 4, 101, 'REQUIRES', 1342);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (24, 4, 104, 'REQUIRES', 795);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (25, 4, 103, 'REQUIRES', 601);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (26, 4, 201, 'CONTAINS', 1388);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (27, 4, 202, 'CONTAINS', 604);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (28, 4, 204, 'CONTAINS', 249);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (29, 5, 101, 'REQUIRES', 597);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (30, 5, 102, 'REQUIRES', 512);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (31, 5, 106, 'REQUIRES', 466);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (32, 5, 104, 'REQUIRES', 391);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (33, 5, 201, 'CONTAINS', 521);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (34, 5, 202, 'CONTAINS', 287);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (35, 5, 205, 'CONTAINS', 169);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (36, 6, 106, 'REQUIRES', 630);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (37, 6, 101, 'REQUIRES', 532);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (38, 6, 104, 'REQUIRES', 299);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (39, 6, 102, 'REQUIRES', 218);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (40, 6, 203, 'CONTAINS', 347);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (41, 6, 202, 'CONTAINS', 212);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (42, 6, 201, 'CONTAINS', 204);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (43, 7, 101, 'REQUIRES', 473);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (44, 7, 104, 'REQUIRES', 471);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (45, 7, 103, 'REQUIRES', 457);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (46, 7, 102, 'REQUIRES', 442);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (47, 7, 201, 'CONTAINS', 453);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (48, 7, 202, 'CONTAINS', 440);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (49, 7, 205, 'CONTAINS', 270);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (50, 8, 101, 'REQUIRES', 480);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (51, 8, 104, 'REQUIRES', 233);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (52, 8, 105, 'REQUIRES', 199);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (53, 8, 102, 'REQUIRES', 164);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (54, 8, 203, 'CONTAINS', 266);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (55, 8, 201, 'CONTAINS', 243);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (56, 8, 204, 'CONTAINS', 141);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (57, 9, 102, 'REQUIRES', 366);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (58, 9, 101, 'REQUIRES', 342);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (59, 9, 104, 'REQUIRES', 268);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (60, 9, 107, 'REQUIRES', 218);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (61, 9, 201, 'CONTAINS', 250);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (62, 9, 202, 'CONTAINS', 171);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (63, 9, 203, 'CONTAINS', 138);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (64, 10, 101, 'REQUIRES', 274);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (65, 10, 104, 'REQUIRES', 235);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (66, 10, 106, 'REQUIRES', 232);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (67, 10, 102, 'REQUIRES', 210);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (68, 10, 202, 'CONTAINS', 202);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (69, 10, 203, 'CONTAINS', 179);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (70, 10, 201, 'CONTAINS', 143);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (71, 11, 103, 'REQUIRES', 447);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (72, 11, 108, 'REQUIRES', 362);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (73, 11, 105, 'REQUIRES', 357);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (74, 11, 101, 'REQUIRES', 208);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (75, 11, 202, 'CONTAINS', 454);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (76, 11, 204, 'CONTAINS', 182);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (77, 11, 205, 'CONTAINS', 104);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (78, 12, 101, 'REQUIRES', 198);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (79, 12, 104, 'REQUIRES', 109);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (80, 12, 102, 'REQUIRES', 93);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (81, 12, 106, 'REQUIRES', 89);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (82, 12, 201, 'CONTAINS', 96);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (83, 12, 203, 'CONTAINS', 75);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (84, 12, 202, 'CONTAINS', 71);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (85, 13, 102, 'REQUIRES', 97);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (86, 13, 101, 'REQUIRES', 79);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (87, 13, 109, 'REQUIRES', 37);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (88, 13, 104, 'REQUIRES', 35);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (89, 13, 207, 'CONTAINS', 193);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (90, 13, 204, 'CONTAINS', 28);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (91, 13, 201, 'CONTAINS', 26);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (92, 14, 103, 'REQUIRES', 74);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (93, 14, 101, 'REQUIRES', 63);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (94, 14, 109, 'REQUIRES', 47);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (95, 14, 105, 'REQUIRES', 46);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (96, 14, 203, 'CONTAINS', 73);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (97, 14, 202, 'CONTAINS', 71);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (98, 14, 201, 'CONTAINS', 35);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (99, 15, 103, 'REQUIRES', 73);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (100, 15, 101, 'REQUIRES', 68);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (101, 15, 102, 'REQUIRES', 62);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (102, 15, 104, 'REQUIRES', 50);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (103, 15, 203, 'CONTAINS', 64);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (104, 15, 202, 'CONTAINS', 60);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (105, 15, 204, 'CONTAINS', 47);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (106, 16, 101, 'REQUIRES', 84);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (107, 16, 102, 'REQUIRES', 58);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (108, 16, 103, 'REQUIRES', 58);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (109, 16, 106, 'REQUIRES', 42);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (110, 16, 202, 'CONTAINS', 55);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (111, 16, 203, 'CONTAINS', 47);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (112, 16, 201, 'CONTAINS', 39);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (113, 17, 101, 'REQUIRES', 29);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (114, 17, 102, 'REQUIRES', 23);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (115, 17, 107, 'REQUIRES', 23);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (116, 17, 104, 'REQUIRES', 21);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (117, 17, 203, 'CONTAINS', 25);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (118, 17, 201, 'CONTAINS', 18);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (119, 17, 202, 'CONTAINS', 13);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (120, 18, 101, 'REQUIRES', 10);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (121, 18, 102, 'REQUIRES', 9);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (122, 18, 104, 'REQUIRES', 5);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (123, 18, 103, 'REQUIRES', 3);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (124, 18, 202, 'CONTAINS', 5);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (125, 18, 201, 'CONTAINS', 3);
INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (126, 18, 204, 'CONTAINS', 3);

-- 完成：37 个节点（岗位 18 / 能力 10 / 场景 9），126 条关系
