#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
把 interview_rag_structured_v1.csv 转换为可直接导入 MySQL 的 SQL 脚本。

映射规则（目标表 knowledge_chunk）：
    doc_title   <- "{primary_job_tag}面试题库"（按岗位分类，共 18 个文档）
    doc_source  <- 源文件名
    position    <- primary_job_tag
    chunk_index <- 组内自增序号（从 0 开始，保持原始行序）
    content     <- retrieval_text（自带标签/问题/参考答案的检索块）
    embedding   <- ''（占位；当前后端未实现向量检索，字段为 NOT NULL）

用法：
    python scripts/gen_knowledge_chunk_sql.py                 # 用默认输入输出
    python scripts/gen_knowledge_chunk_sql.py --input a.csv --output out.sql --batch 200
"""

import argparse
import csv
import os
import sys

csv.field_size_limit(10 ** 7)

TABLE_DDL = """-- =============================================================
-- RAG 知识库分块表 + 面试问答数据集导入脚本
-- 由 scripts/gen_knowledge_chunk_sql.py 自动生成，请勿手改
-- 数据源：interview_rag_structured_v1.csv（20000 条面试问答）
-- 数据库：ai_interview（MySQL 8，utf8mb4）
-- 执行：mysql -u root -p ai_interview < sql/knowledge_chunk_data.sql
-- =============================================================

SET NAMES utf8mb4;
USE ai_interview;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RAG知识库分块表';

"""

COLS = "doc_title, doc_source, position, chunk_index, content, embedding"


def escape(value):
    """转义为 MySQL 单引号字符串字面量。

    注意：这里刻意**不使用**反斜杠转义引号（\\'）。
    mysql 命令行客户端在批处理模式下会把部分 \\' 序列误判为客户端命令
    （报 "Unknown command '\\''"），改用 SQL 标准的双写引号更稳，
    且与 NO_BACKSLASH_ESCAPES 模式也兼容。
    """
    if value is None:
        return "''"
    s = str(value)
    # 去掉会干扰 SQL 文本解析的控制字符（保留换行）
    s = s.replace("\x00", "").replace("\x1a", "").replace("\r\n", "\n").replace("\r", "\n")
    s = s.replace("\\", "\\\\")   # 反斜杠翻倍（默认 sql_mode 下反斜杠是转义符）
    s = s.replace("'", "''")      # 单引号翻倍（SQL 标准写法）
    return "'" + s + "'"


def build(input_path, output_path, batch_size, source_name, limit=None):
    groups = {}
    rows = []

    with open(input_path, encoding="utf-8-sig", newline="") as f:
        reader = csv.DictReader(f)
        missing = {"primary_job_tag", "retrieval_text"} - set(reader.fieldnames or [])
        if missing:
            sys.exit("CSV 缺少必要列: %s" % ", ".join(sorted(missing)))

        for i, row in enumerate(reader):
            if limit is not None and i >= limit:
                break
            job = (row.get("primary_job_tag") or "").strip() or "通用面试"
            doc_title = "通用面试题库" if job == "通用面试" else "%s面试题库" % job
            idx = groups.get(doc_title, 0)
            groups[doc_title] = idx + 1
            content = (row.get("retrieval_text") or "").strip()
            if not content:
                content = "%s\n%s" % ((row.get("question") or "").strip(), (row.get("answer") or "").strip())
            rows.append((doc_title, source_name, job, idx, content, ""))

    total = len(rows)
    with open(output_path, "w", encoding="utf-8", newline="\n") as out:
        out.write(TABLE_DDL)
        # 重复导入时先清空，保持幂等
        out.write("-- 幂等：先清空再导入\nTRUNCATE TABLE knowledge_chunk;\n\n")
        for start in range(0, total, batch_size):
            chunk = rows[start:start + batch_size]
            out.write("INSERT INTO knowledge_chunk (%s) VALUES\n" % COLS)
            out.write(",\n".join(
                "(%s, %s, %s, %d, %s, %s)" % (
                    escape(r[0]), escape(r[1]), escape(r[2]), r[3], escape(r[4]), escape(r[5]))
                for r in chunk
            ))
            out.write(";\n")
            if (start // batch_size) % 20 == 0:
                print("  已写入 %d / %d 行" % (start + len(chunk), total), flush=True)
        out.write("\n-- 完成：共 %d 行，%d 个文档分组\n" % (total, len(groups)))

    return total, groups


def main():
    root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    default_in = os.path.join(root, "data", "knowledge", "interview_rag_structured_v1.csv")
    default_out = os.path.join(root, "sql", "knowledge_chunk_data.sql")

    p = argparse.ArgumentParser()
    p.add_argument("--input", default=default_in)
    p.add_argument("--output", default=default_out)
    p.add_argument("--batch", type=int, default=200)
    p.add_argument("--source-name", default="interview_rag_structured_v1.csv")
    p.add_argument("--limit", type=int, default=None, help="仅处理前 N 行（调试用）")
    args = p.parse_args()

    if not os.path.exists(args.input):
        sys.exit("找不到输入文件: %s" % args.input)

    print("读取 %s ..." % args.input)
    total, groups = build(args.input, args.output, args.batch, args.source_name, args.limit)
    size = os.path.getsize(args.output)
    print("已生成 %s" % args.output)
    print("总行数: %d，文档分组: %d 个，文件大小: %.1f MB" % (total, len(groups), size / 1024 / 1024))
    for title in sorted(groups):
        print("  - %-24s %d 条" % (title, groups[title]))


if __name__ == "__main__":
    main()
