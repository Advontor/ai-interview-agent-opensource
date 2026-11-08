#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
从面试问答数据集生成知识图谱数据（graph_node / graph_edge）。

图谱结构（全部来自真实数据统计，非编造）：
    JOB       <- primary_job_tag            共 18 个岗位
    SKILL     <- skill_tags  聚合去重        共 10 个能力维度
    KNOWLEDGE <- scene_tags  聚合去重        共 9 个考察场景
    JOB -REQUIRES-> SKILL      取该岗位占比最突出的前 N 项
    JOB -CONTAINS-> KNOWLEDGE  取该岗位占比最突出的前 M 项

节点数量刻意控制在几十个：前端 GraphPage 会对**每个节点**各发 2 次请求
（/graph/edge/source/{id} 与 /graph/edge/target/{id}）且是顺序执行，
节点过多会导致页面加载非常慢。

用法：
    python scripts/gen_knowledge_graph_sql.py
    python scripts/gen_knowledge_graph_sql.py --top-skills 4 --top-scenes 3
"""

import argparse
import csv
import collections
import os
import sys

csv.field_size_limit(10 ** 7)

JOB_BASE = 1
SKILL_BASE = 101
SCENE_BASE = 201
EDGE_BASE = 1

DDL = """-- =============================================================
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

"""


def esc(value):
    return "'" + str(value).replace("\\", "\\\\").replace("'", "''") + "'"


def split_tags(value):
    return [t.strip() for t in (value or "").split("|") if t.strip()]


def main():
    root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    default_in = os.path.join(root, "data", "knowledge", "interview_rag_structured_v1.csv")
    default_out = os.path.join(root, "sql", "knowledge_graph_data.sql")

    p = argparse.ArgumentParser()
    p.add_argument("--input", default=default_in)
    p.add_argument("--output", default=default_out)
    p.add_argument("--top-skills", type=int, default=4, help="每个岗位保留的前 N 个能力维度")
    p.add_argument("--top-scenes", type=int, default=3, help="每个岗位保留的前 N 个考察场景")
    args = p.parse_args()

    if not os.path.exists(args.input):
        sys.exit("找不到输入文件: %s" % args.input)

    job_count = collections.Counter()
    skill_count = collections.Counter()
    scene_count = collections.Counter()
    job_skill = collections.Counter()
    job_scene = collections.Counter()

    with open(args.input, encoding="utf-8-sig", newline="") as f:
        for row in csv.DictReader(f):
            job = (row.get("primary_job_tag") or "").strip() or "通用面试"
            job_count[job] += 1
            for s in split_tags(row.get("skill_tags")):
                skill_count[s] += 1
                job_skill[(job, s)] += 1
            for s in split_tags(row.get("scene_tags")):
                scene_count[s] += 1
                job_scene[(job, s)] += 1

    if not job_count:
        sys.exit("没有解析到任何数据")

    jobs = [j for j, _ in job_count.most_common()]
    skills = [s for s, _ in skill_count.most_common()]
    scenes = [s for s, _ in scene_count.most_common()]

    job_id = {j: JOB_BASE + i for i, j in enumerate(jobs)}
    skill_id = {s: SKILL_BASE + i for i, s in enumerate(skills)}
    scene_id = {s: SCENE_BASE + i for i, s in enumerate(scenes)}

    out = [DDL]
    out.append("-- ---------- 节点 ----------\n")

    out.append("-- 岗位节点（JOB）\n")
    for j in jobs:
        desc = "该岗位面试题共 %d 道" % job_count[j]
        out.append(
            "INSERT INTO graph_node (id, node_type, name, description) VALUES (%d, 'JOB', %s, %s);\n"
            % (job_id[j], esc(j), esc(desc))
        )

    out.append("\n-- 能力维度节点（SKILL）\n")
    for s in skills:
        desc = "出现于 %d 道面试题" % skill_count[s]
        out.append(
            "INSERT INTO graph_node (id, node_type, name, description) VALUES (%d, 'SKILL', %s, %s);\n"
            % (skill_id[s], esc(s), esc(desc))
        )

    out.append("\n-- 考察场景节点（KNOWLEDGE）\n")
    for s in scenes:
        desc = "出现于 %d 道面试题" % scene_count[s]
        out.append(
            "INSERT INTO graph_node (id, node_type, name, description) VALUES (%d, 'KNOWLEDGE', %s, %s);\n"
            % (scene_id[s], esc(s), esc(desc))
        )

    out.append("\n-- ---------- 关系 ----------\n")
    edge_lines = []
    for j in jobs:
        top_skills = sorted(
            ((s, job_skill[(j, s)]) for s in skills if job_skill[(j, s)] > 0),
            key=lambda x: -x[1],
        )[: args.top_skills]
        for s, w in top_skills:
            edge_lines.append((job_id[j], skill_id[s], "REQUIRES", w))

        top_scenes = sorted(
            ((s, job_scene[(j, s)]) for s in scenes if job_scene[(j, s)] > 0),
            key=lambda x: -x[1],
        )[: args.top_scenes]
        for s, w in top_scenes:
            edge_lines.append((job_id[j], scene_id[s], "CONTAINS", w))

    for i, (src, dst, rel, w) in enumerate(edge_lines):
        out.append(
            "INSERT INTO graph_edge (id, source_node_id, target_node_id, relation_type, weight) VALUES (%d, %d, %d, '%s', %d);\n"
            % (EDGE_BASE + i, src, dst, rel, w)
        )

    total_nodes = len(jobs) + len(skills) + len(scenes)
    out.append(
        "\n-- 完成：%d 个节点（岗位 %d / 能力 %d / 场景 %d），%d 条关系\n"
        % (total_nodes, len(jobs), len(skills), len(scenes), len(edge_lines))
    )

    with open(args.output, "w", encoding="utf-8", newline="\n") as f:
        f.write("".join(out))

    print("已生成", args.output)
    print("节点 %d（岗位 %d / 能力 %d / 场景 %d），关系 %d"
          % (total_nodes, len(jobs), len(skills), len(scenes), len(edge_lines)))
    print("前端会发起约 %d 次边查询请求（每个节点 2 次）" % (total_nodes * 2))


if __name__ == "__main__":
    main()
