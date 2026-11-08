# -*- coding: utf-8 -*-
"""一次性生成 sql/init_learning_resources.sql（B 站搜索直达链接）。"""
import os
from urllib.parse import quote

RESOURCES = [
    # (title, bilibili 搜索关键词, category, related_skill, description)
    ("Spring Boot 自动装配原理深度解析", "Spring Boot 自动装配原理", "Java", "Spring Boot",
     "从源码层面理解自动装配机制，掌握 @EnableAutoConfiguration 与 SPI 扩展点。"),
    ("JVM 内存结构与垃圾回收机制", "JVM 内存结构与垃圾回收", "Java", "JVM",
     "系统讲解运行时数据区、常见垃圾收集器以及性能调优实战技巧。"),
    ("MySQL 索引底层实现与优化", "MySQL 索引原理与优化", "Java", "MySQL",
     "深入 B+ 树索引结构，理解聚簇索引、覆盖索引与最左前缀匹配原则。"),
    ("Redis 核心原理与实战", "Redis 核心原理与实战", "Java", "Redis",
     "讲解数据结构、持久化、集群方案以及缓存穿透/击穿/雪崩的解决方案。"),
    ("Java 并发编程与 JUC 工具包", "Java 并发编程 JUC", "Java", "并发编程",
     "线程池、锁机制、CAS、AQS 与并发容器的原理与面试高频考点。"),
    ("Spring AOP 与事务原理", "Spring AOP 事务原理", "Java", "Spring",
     "动态代理、事务传播行为与失效场景分析，面试常考的源码级问题。"),

    ("Vue 3 响应式原理与 Composition API", "Vue3 响应式原理 Composition API", "前端", "Vue",
     "从 Proxy 到 ref/reactive，深入理解 Vue 3 响应式系统与组合式 API 设计。"),
    ("TypeScript 高级类型与实战", "TypeScript 高级类型实战", "前端", "TypeScript",
     "系统讲解高级类型、泛型、类型体操以及工程化最佳实践。"),
    ("前端工程化与 Vite 构建优化", "前端工程化 Vite 构建优化", "前端", "工程化",
     "模块化、打包原理、构建性能优化与前端规范体系搭建。"),
    ("JavaScript 事件循环与异步编程", "JavaScript 事件循环 异步编程", "前端", "JavaScript",
     "宏任务/微任务、Promise 与 async/await 的执行顺序，面试必考。"),
    ("浏览器渲染原理与性能优化", "浏览器渲染原理 性能优化", "前端", "性能优化",
     "重排重绘、关键渲染路径、首屏优化与 Core Web Vitals 指标解读。"),
    ("前端面试高频手写题精讲", "前端面试 手写题", "前端", "手写代码",
     "手写 Promise、防抖节流、深拷贝、call/apply/bind 等高频题目。"),

    ("动态规划入门到精通", "动态规划 入门到精通", "算法", "动态规划",
     "从斐波那契到背包问题，系统掌握动态规划的解题思路与常见题型。"),
    ("二叉树与图论算法精讲", "二叉树 图论算法", "算法", "数据结构",
     "二叉树遍历、图的搜索、最短路径等核心算法，配合真题演练。"),
    ("排序算法与复杂度分析", "排序算法 复杂度分析", "算法", "排序算法",
     "快排、归并、堆排的实现与稳定性分析，以及时间/空间复杂度推导。"),
    ("哈希表与字符串算法", "哈希表 字符串算法", "算法", "哈希表",
     "哈希冲突处理、字符串匹配（KMP）与高频字符串题型套路。"),
    ("滑动窗口与双指针技巧", "滑动窗口 双指针算法", "算法", "双指针",
     "子数组/子串类问题的通用解法，从暴力到 O(n) 的优化思路。"),
    ("LeetCode 高频面试题精讲", "LeetCode 高频题精讲", "算法", "刷题",
     "按题型分类讲解大厂高频题，总结可复用的解题模板。"),

    ("STAR 法则：让回答更有结构", "STAR法则 面试回答", "行为面试", "STAR法则",
     "情境-任务-行动-结果四步法，让行为面试回答更有逻辑和说服力。"),
    ("如何优雅地介绍你的项目经历", "面试 项目经历 介绍", "行为面试", "项目介绍",
     "掌握项目介绍的黄金公式，突出个人贡献与技术难点。"),
    ("自我介绍模板与表达技巧", "面试 自我介绍 模板", "行为面试", "自我介绍",
     "一分钟讲清背景、亮点与求职意向，避开流水账式自我介绍。"),
    ("高频行为面试题解析", "行为面试 高频问题", "行为面试", "行为面试",
     "优缺点、离职原因、团队冲突、职业规划等高频问题的回答框架。"),
    ("面试反问环节该问什么", "面试 反问环节 技巧", "行为面试", "反问环节",
     "用高质量反问体现思考深度，同时判断岗位是否适合自己。"),
    ("谈薪与 offer 沟通技巧", "面试 谈薪 offer 沟通", "行为面试", "薪资沟通",
     "薪资构成、涨幅谈判与多 offer 权衡的表达方式。"),
]

HEADER = """-- =============================================================
-- 学习资源初始化脚本
-- 数据库：ai_interview（MySQL 8，utf8mb4）
-- 由 scripts/gen_learning_resources_sql.py 生成，可重复执行（幂等）
-- 说明：url 使用 B 站「搜索直达」链接（https://search.bilibili.com/all?keyword=...）
--       这样永远有效、直达该主题的视频列表；如需精确到某个视频，
--       把 url 换成具体视频地址即可（如 https://www.bilibili.com/video/BVxxxx）。
-- =============================================================

SET NAMES utf8mb4;
USE ai_interview;

CREATE TABLE IF NOT EXISTS learning_resource (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    title         VARCHAR(255) NOT NULL COMMENT '资源标题',
    url           VARCHAR(500) NOT NULL COMMENT '资源链接',
    category      VARCHAR(64)  DEFAULT NULL COMMENT '分类：Java / 前端 / 算法 / 行为面试',
    related_skill VARCHAR(128) DEFAULT NULL COMMENT '关联技能标签（用于匹配面试薄弱点）',
    description   TEXT         COMMENT '资源描述',
    created_at    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_category (category),
    KEY idx_related_skill (related_skill)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '学习资源表';

TRUNCATE TABLE learning_resource;

"""


def esc(value: str) -> str:
    return "'" + str(value).replace("\\", "\\\\").replace("'", "''") + "'"


def main():
    root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    out_path = os.path.join(root, "sql", "init_learning_resources.sql")

    lines = [HEADER]
    for title, keyword, category, skill, desc in RESOURCES:
        url = "https://search.bilibili.com/all?keyword=" + quote(keyword)
        lines.append(
            "INSERT INTO learning_resource (title, url, category, related_skill, description) VALUES\n"
            "(%s, %s, %s, %s, %s);\n" % (esc(title), esc(url), esc(category), esc(skill), esc(desc))
        )
    lines.append("\n-- 共 %d 条学习资源\n" % len(RESOURCES))

    with open(out_path, "w", encoding="utf-8", newline="\n") as f:
        f.write("".join(lines))
    print("已生成", out_path, "共", len(RESOURCES), "条")


if __name__ == "__main__":
    main()
