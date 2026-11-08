-- 旧版 report 表升级脚本。
-- 应用启动时会自动检查并补齐这些列；手工执行时只运行数据库中尚不存在的语句。

ALTER TABLE report ADD COLUMN summary TEXT COMMENT '总体评价';
ALTER TABLE report ADD COLUMN professional_ability INT DEFAULT NULL COMMENT '专业能力 0-100';
ALTER TABLE report ADD COLUMN knowledge_coverage INT DEFAULT NULL COMMENT '知识覆盖度 0-100';
ALTER TABLE report ADD COLUMN job_match INT DEFAULT NULL COMMENT '岗位匹配度 0-100';
ALTER TABLE report ADD COLUMN communication INT DEFAULT NULL COMMENT '表达沟通 0-100';
ALTER TABLE report ADD COLUMN logical_thinking INT DEFAULT NULL COMMENT '逻辑结构 0-100';
ALTER TABLE report ADD COLUMN question_reviews_json LONGTEXT COMMENT '逐题评价 JSON';
ALTER TABLE report ADD COLUMN transcript_json LONGTEXT COMMENT '完整问答 JSON';
ALTER TABLE report ADD COLUMN chart_data_json LONGTEXT COMMENT '可视化图表数据 JSON';
ALTER TABLE report ADD COLUMN resource_recommendations_json LONGTEXT COMMENT '学习资源推荐 JSON';
ALTER TABLE report ADD COLUMN emotion_timeline_json LONGTEXT COMMENT '情绪时序分析 JSON';
ALTER TABLE report ADD COLUMN star_analysis_json LONGTEXT COMMENT 'STAR 分析 JSON';
