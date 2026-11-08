-- =============================================================
-- 修复：面试会话没有归属用户，导致「历史面试」永远为空
--
-- 现象：完成面试后，历史面试页看不到任何记录。
-- 原因：
--   1) 后端创建会话时从未写入 user_id（InterviewSessionServiceImpl 里没有 setUserId），
--      而 user_id 列定义是 DEFAULT NULL —— 所以每一条会话的 user_id 都是 NULL；
--   2) 前端历史页调用 GET /interview/session/list/1（写死 userId=1，项目暂无登录系统），
--      查询条件是 user_id = 1，NULL 永远匹配不上，于是列表恒为空。
--
-- 修复思路（无需改后端、无需重新编译）：
--   MyBatis-Plus 默认字段策略为 NOT_NULL，实体里 userId 为 null 时该列不会出现在
--   INSERT 语句中，因此会走数据库列默认值。把默认值设为 1 即可让新会话自动归属到 1。
--
-- 幂等：可重复执行。
-- 执行：mysql -u root -p ai_interview < sql/fix_session_user_id.sql
-- =============================================================

SET NAMES utf8mb4;
USE ai_interview;

ALTER TABLE interview_session
    MODIFY COLUMN user_id BIGINT NULL DEFAULT 1 COMMENT '用户id，关联user表（无登录系统时默认 1）';

-- 回填历史数据
UPDATE interview_session SET user_id = 1 WHERE user_id IS NULL;

-- 校验
SELECT COUNT(*) AS total,
       SUM(user_id = 1) AS belongs_to_user_1,
       SUM(user_id IS NULL) AS still_null
FROM interview_session;
