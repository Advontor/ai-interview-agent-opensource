# 启动与数据库说明

## 从零初始化

默认数据库为 `ai_interview`。完整新库顺序见根目录 README；`add_reference_answer.sql` 和 `add_report_question_reviews.sql` 分别补齐消息参考答案，以及报告岗位、难度和逐题评价字段，不能遗漏。

`sql/init.sql` 已含简历结构化字段，因此新库无需再运行 `phase2_resume.sql`。`week1_integration.sql`、`phase2_resume.sql` 和 `sql/migrations/` 保留供历史迁移参考，不要把所有 SQL 不加区分地重复执行。

已有数据库升级前先备份，再对照实体和实际表结构选择迁移语句。重复加列错误不代表迁移已完整成功。

## 题库资源

- `src/main/resources/knowledge/interview-rag-v1.jsonl.gz`：本地检索服务启动时加载，已随源码保留。
- `knowledge_chunk` 表：供指定题库面试、题库文档列表和题目练习使用，需要由 CSV 生成 SQL 后导入。

在项目根目录重新生成资源：

```sh
python scripts/import_interview_rag.py data/knowledge/interview_rag_structured_v1.csv
python scripts/gen_knowledge_chunk_sql.py
python scripts/gen_knowledge_graph_sql.py
python scripts/gen_learning_resources_sql.py
```

生成 SQL 和执行 SQL 是两步。生成器不连接或修改 MySQL；导入 SQL 才会修改对应表。

## 环境变量和运行

后端变量列表见 `.env.example`。根目录 `.env` 不自动加载，前端 `.env` 由 Vite 读取。不要把后端 API Key 放进 `VITE_` 变量。

PDF 中文字体可用 `REPORT_PDF_FONT_PATH` 指定；不指定时尝试常见字体路径。

```sh
mvn "-Dmaven.test.skip=true" package
java -jar target/ai_interview_system-0.0.1-SNAPSHOT.jar
```

`--spring.profiles.active=prod` 启用上传限制和日志配置，不会自动补全鉴权；仍需设置数据库与模型环境变量。

## 前端托管

前端采用 History 路由，静态托管需把未匹配路径回退到 `index.html`。代理 `/api` 时保留此前缀；SSE 需关闭代理缓冲并允许长连接，WebSocket 另需升级头。

当前项目用于学习和受控演示，部署配置不代表已达到公开运营要求。
