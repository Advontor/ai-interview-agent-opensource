-- =============================================================
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

INSERT INTO learning_resource (title, url, category, related_skill, description) VALUES
('Spring Boot 自动装配原理深度解析', 'https://search.bilibili.com/all?keyword=Spring%20Boot%20%E8%87%AA%E5%8A%A8%E8%A3%85%E9%85%8D%E5%8E%9F%E7%90%86', 'Java', 'Spring Boot', '从源码层面理解自动装配机制，掌握 @EnableAutoConfiguration 与 SPI 扩展点。');
INSERT INTO learning_resource (title, url, category, related_skill, description) VALUES
('JVM 内存结构与垃圾回收机制', 'https://search.bilibili.com/all?keyword=JVM%20%E5%86%85%E5%AD%98%E7%BB%93%E6%9E%84%E4%B8%8E%E5%9E%83%E5%9C%BE%E5%9B%9E%E6%94%B6', 'Java', 'JVM', '系统讲解运行时数据区、常见垃圾收集器以及性能调优实战技巧。');
INSERT INTO learning_resource (title, url, category, related_skill, description) VALUES
('MySQL 索引底层实现与优化', 'https://search.bilibili.com/all?keyword=MySQL%20%E7%B4%A2%E5%BC%95%E5%8E%9F%E7%90%86%E4%B8%8E%E4%BC%98%E5%8C%96', 'Java', 'MySQL', '深入 B+ 树索引结构，理解聚簇索引、覆盖索引与最左前缀匹配原则。');
INSERT INTO learning_resource (title, url, category, related_skill, description) VALUES
('Redis 核心原理与实战', 'https://search.bilibili.com/all?keyword=Redis%20%E6%A0%B8%E5%BF%83%E5%8E%9F%E7%90%86%E4%B8%8E%E5%AE%9E%E6%88%98', 'Java', 'Redis', '讲解数据结构、持久化、集群方案以及缓存穿透/击穿/雪崩的解决方案。');
INSERT INTO learning_resource (title, url, category, related_skill, description) VALUES
('Java 并发编程与 JUC 工具包', 'https://search.bilibili.com/all?keyword=Java%20%E5%B9%B6%E5%8F%91%E7%BC%96%E7%A8%8B%20JUC', 'Java', '并发编程', '线程池、锁机制、CAS、AQS 与并发容器的原理与面试高频考点。');
INSERT INTO learning_resource (title, url, category, related_skill, description) VALUES
('Spring AOP 与事务原理', 'https://search.bilibili.com/all?keyword=Spring%20AOP%20%E4%BA%8B%E5%8A%A1%E5%8E%9F%E7%90%86', 'Java', 'Spring', '动态代理、事务传播行为与失效场景分析，面试常考的源码级问题。');
INSERT INTO learning_resource (title, url, category, related_skill, description) VALUES
('Vue 3 响应式原理与 Composition API', 'https://search.bilibili.com/all?keyword=Vue3%20%E5%93%8D%E5%BA%94%E5%BC%8F%E5%8E%9F%E7%90%86%20Composition%20API', '前端', 'Vue', '从 Proxy 到 ref/reactive，深入理解 Vue 3 响应式系统与组合式 API 设计。');
INSERT INTO learning_resource (title, url, category, related_skill, description) VALUES
('TypeScript 高级类型与实战', 'https://search.bilibili.com/all?keyword=TypeScript%20%E9%AB%98%E7%BA%A7%E7%B1%BB%E5%9E%8B%E5%AE%9E%E6%88%98', '前端', 'TypeScript', '系统讲解高级类型、泛型、类型体操以及工程化最佳实践。');
INSERT INTO learning_resource (title, url, category, related_skill, description) VALUES
('前端工程化与 Vite 构建优化', 'https://search.bilibili.com/all?keyword=%E5%89%8D%E7%AB%AF%E5%B7%A5%E7%A8%8B%E5%8C%96%20Vite%20%E6%9E%84%E5%BB%BA%E4%BC%98%E5%8C%96', '前端', '工程化', '模块化、打包原理、构建性能优化与前端规范体系搭建。');
INSERT INTO learning_resource (title, url, category, related_skill, description) VALUES
('JavaScript 事件循环与异步编程', 'https://search.bilibili.com/all?keyword=JavaScript%20%E4%BA%8B%E4%BB%B6%E5%BE%AA%E7%8E%AF%20%E5%BC%82%E6%AD%A5%E7%BC%96%E7%A8%8B', '前端', 'JavaScript', '宏任务/微任务、Promise 与 async/await 的执行顺序，面试必考。');
INSERT INTO learning_resource (title, url, category, related_skill, description) VALUES
('浏览器渲染原理与性能优化', 'https://search.bilibili.com/all?keyword=%E6%B5%8F%E8%A7%88%E5%99%A8%E6%B8%B2%E6%9F%93%E5%8E%9F%E7%90%86%20%E6%80%A7%E8%83%BD%E4%BC%98%E5%8C%96', '前端', '性能优化', '重排重绘、关键渲染路径、首屏优化与 Core Web Vitals 指标解读。');
INSERT INTO learning_resource (title, url, category, related_skill, description) VALUES
('前端面试高频手写题精讲', 'https://search.bilibili.com/all?keyword=%E5%89%8D%E7%AB%AF%E9%9D%A2%E8%AF%95%20%E6%89%8B%E5%86%99%E9%A2%98', '前端', '手写代码', '手写 Promise、防抖节流、深拷贝、call/apply/bind 等高频题目。');
INSERT INTO learning_resource (title, url, category, related_skill, description) VALUES
('动态规划入门到精通', 'https://search.bilibili.com/all?keyword=%E5%8A%A8%E6%80%81%E8%A7%84%E5%88%92%20%E5%85%A5%E9%97%A8%E5%88%B0%E7%B2%BE%E9%80%9A', '算法', '动态规划', '从斐波那契到背包问题，系统掌握动态规划的解题思路与常见题型。');
INSERT INTO learning_resource (title, url, category, related_skill, description) VALUES
('二叉树与图论算法精讲', 'https://search.bilibili.com/all?keyword=%E4%BA%8C%E5%8F%89%E6%A0%91%20%E5%9B%BE%E8%AE%BA%E7%AE%97%E6%B3%95', '算法', '数据结构', '二叉树遍历、图的搜索、最短路径等核心算法，配合真题演练。');
INSERT INTO learning_resource (title, url, category, related_skill, description) VALUES
('排序算法与复杂度分析', 'https://search.bilibili.com/all?keyword=%E6%8E%92%E5%BA%8F%E7%AE%97%E6%B3%95%20%E5%A4%8D%E6%9D%82%E5%BA%A6%E5%88%86%E6%9E%90', '算法', '排序算法', '快排、归并、堆排的实现与稳定性分析，以及时间/空间复杂度推导。');
INSERT INTO learning_resource (title, url, category, related_skill, description) VALUES
('哈希表与字符串算法', 'https://search.bilibili.com/all?keyword=%E5%93%88%E5%B8%8C%E8%A1%A8%20%E5%AD%97%E7%AC%A6%E4%B8%B2%E7%AE%97%E6%B3%95', '算法', '哈希表', '哈希冲突处理、字符串匹配（KMP）与高频字符串题型套路。');
INSERT INTO learning_resource (title, url, category, related_skill, description) VALUES
('滑动窗口与双指针技巧', 'https://search.bilibili.com/all?keyword=%E6%BB%91%E5%8A%A8%E7%AA%97%E5%8F%A3%20%E5%8F%8C%E6%8C%87%E9%92%88%E7%AE%97%E6%B3%95', '算法', '双指针', '子数组/子串类问题的通用解法，从暴力到 O(n) 的优化思路。');
INSERT INTO learning_resource (title, url, category, related_skill, description) VALUES
('LeetCode 高频面试题精讲', 'https://search.bilibili.com/all?keyword=LeetCode%20%E9%AB%98%E9%A2%91%E9%A2%98%E7%B2%BE%E8%AE%B2', '算法', '刷题', '按题型分类讲解大厂高频题，总结可复用的解题模板。');
INSERT INTO learning_resource (title, url, category, related_skill, description) VALUES
('STAR 法则：让回答更有结构', 'https://search.bilibili.com/all?keyword=STAR%E6%B3%95%E5%88%99%20%E9%9D%A2%E8%AF%95%E5%9B%9E%E7%AD%94', '行为面试', 'STAR法则', '情境-任务-行动-结果四步法，让行为面试回答更有逻辑和说服力。');
INSERT INTO learning_resource (title, url, category, related_skill, description) VALUES
('如何优雅地介绍你的项目经历', 'https://search.bilibili.com/all?keyword=%E9%9D%A2%E8%AF%95%20%E9%A1%B9%E7%9B%AE%E7%BB%8F%E5%8E%86%20%E4%BB%8B%E7%BB%8D', '行为面试', '项目介绍', '掌握项目介绍的黄金公式，突出个人贡献与技术难点。');
INSERT INTO learning_resource (title, url, category, related_skill, description) VALUES
('自我介绍模板与表达技巧', 'https://search.bilibili.com/all?keyword=%E9%9D%A2%E8%AF%95%20%E8%87%AA%E6%88%91%E4%BB%8B%E7%BB%8D%20%E6%A8%A1%E6%9D%BF', '行为面试', '自我介绍', '一分钟讲清背景、亮点与求职意向，避开流水账式自我介绍。');
INSERT INTO learning_resource (title, url, category, related_skill, description) VALUES
('高频行为面试题解析', 'https://search.bilibili.com/all?keyword=%E8%A1%8C%E4%B8%BA%E9%9D%A2%E8%AF%95%20%E9%AB%98%E9%A2%91%E9%97%AE%E9%A2%98', '行为面试', '行为面试', '优缺点、离职原因、团队冲突、职业规划等高频问题的回答框架。');
INSERT INTO learning_resource (title, url, category, related_skill, description) VALUES
('面试反问环节该问什么', 'https://search.bilibili.com/all?keyword=%E9%9D%A2%E8%AF%95%20%E5%8F%8D%E9%97%AE%E7%8E%AF%E8%8A%82%20%E6%8A%80%E5%B7%A7', '行为面试', '反问环节', '用高质量反问体现思考深度，同时判断岗位是否适合自己。');
INSERT INTO learning_resource (title, url, category, related_skill, description) VALUES
('谈薪与 offer 沟通技巧', 'https://search.bilibili.com/all?keyword=%E9%9D%A2%E8%AF%95%20%E8%B0%88%E8%96%AA%20offer%20%E6%B2%9F%E9%80%9A', '行为面试', '薪资沟通', '薪资构成、涨幅谈判与多 offer 权衡的表达方式。');

-- 共 24 条学习资源
