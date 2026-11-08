# 面试问答数据集

本目录保存项目维护者提供的结构化面试题库，包含 20,000 条问答。题库为指定题库面试、知识浏览和本地资料推荐提供数据；它的存在不代表检索已经采用向量模型。

## 文件与载体

| 文件 | 用途 |
| --- | --- |
| `interview_rag_structured_v1.csv` | UTF-8 CSV 源文件，保留 16 个字段 |
| `../../src/main/resources/knowledge/interview-rag-v1.jsonl.gz` | 后端启动时加载的压缩本地资源 |
| `../../src/main/resources/knowledge/interview-rag-v1.jsonl.meta.json` | 资源版本、CSV SHA-256、条目数与规范文件名 |
| `../../src/main/resources/knowledge/week2.json` | 另一路基础知识服务使用的知识条目 |

## 字段定义

| 字段 | 说明 |
| --- | --- |
| `row_id` | 问答条目的唯一来源标识，用于追溯检索结果。 |
| `source_file` | 来源文件名，当前记录为原始工作簿名称，不等同于公开来源 URL 或许可证。 |
| `source_sheet` | 原始工作表标识。 |
| `created_at` | 源数据中的生成或记录时间。 |
| `question` | 面试问题正文。 |
| `answer` | 参考回答，供练习和检索参考，不代表经过权威审核的标准答案。 |
| `primary_job_tag` | 原始主要岗位分类，共 18 类。 |
| `job_tags` | 岗位标签集合。 |
| `skill_tags` | 能力与技能标签集合。 |
| `scene_tags` | 面试考察场景标签集合。 |
| `question_type` | 题型标记，例如 how、why、behavior_case。 |
| `difficulty` | basic、intermediate、advanced 三种难度标记。 |
| `question_len` | 源数据记录的问题长度。 |
| `answer_len` | 源数据记录的回答长度。 |
| `keywords` | 原始关键词字段；本地资源生成器使用整理后的标签重建检索文本。 |
| `retrieval_text` | 源数据的结构化检索文本；SQL 题库使用该字段。 |

## 数据覆盖

以下为发布前从保留 CSV 实际统计的结果。

| 原始岗位分类 | 条目数 |
| --- | ---: |
| 通用面试 | 5,209 |
| 后端开发 | 3,707 |
| 产品经理 | 3,303 |
| 数据分析 | 1,647 |
| 运维/SRE/DevOps | 924 |
| 设计/UIUX | 907 |
| 测试开发/QA | 808 |
| 运营/市场 | 737 |
| 算法/机器学习 | 577 |
| 前端开发 | 543 |
| 项目经理 | 512 |
| 移动开发 | 260 |
| 安全 | 205 |
| 人力资源 | 201 |
| 销售/商务 | 200 |
| 财务/审计 | 178 |
| 嵌入式/硬件 | 71 |
| 全栈开发 | 11 |
| **合计** | **20,000** |

| 难度 | 条目数 |
| --- | ---: |
| basic | 6,250 |
| intermediate | 11,124 |
| advanced | 2,626 |

行号和问题分别有 20,000 个唯一值，问题与回答字段无空值。去重和非空校验不等于内容事实正确性验证。

## 两条数据链路

1. **MySQL 题库**：保留原始岗位分类，按 `primary_job_tag` 组织文档；导入 `retrieval_text` 为检索片段。`embedding` 是预留字段，当前未参与检索。
2. **本地词法题库**：生成器校验必填字段、重复行号和重复问题，清洗空白，合并岗位、能力、场景标签，重建有长度限制的检索文本。岗位归一化为后端开发、前端开发、产品经理、通用面试和其他岗位；难度转为 EASY、MEDIUM、HARD。

本地检索按题干和检索文本命中加权评分，经过岗位与难度过滤后用优先队列选择 Top-K，低于相关性阈值时返回空结果。它不是 BM25、Embedding 或余弦相似度检索。

## 重建与导入

在项目根目录执行：

```sh
python scripts/import_interview_rag.py data/knowledge/interview_rag_structured_v1.csv
python scripts/gen_knowledge_chunk_sql.py
```

第一条重建本地 Gzip 资源及元数据；第二条生成 `sql/knowledge_chunk_data.sql`。生成脚本不连接数据库，导入方法见根目录 README。生成 SQL 含清空题库表的语句，只用于可以重新导入的演示数据。

本地资源按解压后的知识内容验证一致性，不依赖不同 Python 或压缩库环境生成的 gzip 文件字节完全相同。

仓库通过 `.gitattributes` 禁用 CSV 换行自动转换，以便在 Windows 与 Unix 环境中保留相同的源文件摘要。

## 来源与许可范围

源 CSV 的 `source_file` 记录为 `面试沟通问答数据集2万条.xlsx`。这个文件名是保留的来源线索，不能替代原始发布页面、作者信息或数据许可证。当前提供的材料未附单独的数据许可证和完整原始来源 URL。

根目录 MIT 许可证覆盖项目代码和项目文档，不将第三方题库、其派生内容或图像资源自动重新许可。使用者如需再分发或用于其他场景，应核对相应内容的来源和授权条件；参考回答也需要结合实际技术资料核验。

## 数据校验标识

保留 CSV 的 SHA-256：

`2aff033d165285f67b25cc283143e18b885d26638cdc678ee298dc05c3e79a94`

新增副本与原保留 CSV 的差别仅在 `retrieval_text` 的换行编码。本次发布统一保留维护者新增的源文件内容，规范为本目录的文件名，没有重复上传根目录带副本后缀的 CSV。
