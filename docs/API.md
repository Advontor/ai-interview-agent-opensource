# 主要 API

默认前缀：`http://localhost:8080/api`。大多数 REST 接口使用 `Result<T>` 包装；SSE 与 PDF 不使用普通 JSON 响应格式。

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| POST | `/resume/upload` | 上传文字型 PDF 或 DOCX 并解析 |
| POST | `/resume/parse` | 解析简历纯文本 |
| POST | `/interview/session` | 创建会话 |
| POST | `/interview/answer` | 同步提交回答 |
| POST | `/interview/stream/answer/{sessionId}` | 提交回答并接收 SSE |
| POST | `/interview/finish` | 结束并生成评价 |
| GET | `/interview/session/{sessionId}` | 恢复会话 |
| GET | `/report/session/{sessionId}` | 查询报告 |
| GET | `/report/pdf/{sessionId}` | 导出 PDF |
| GET | `/rag/doc/list` | 查询 MySQL 题库文档标题 |
| GET | `/rag/search?keyword=Java` | MySQL 关键词检索 |
| GET | `/knowledge/interview-rag/stats` | 本地题库统计 |
| GET | `/knowledge/interview-rag/search` | 本地词法检索 |
| GET | `/knowledge/interview-rag/recommend` | 推荐资料 |
| GET | `/agent/tools` | 知识检索工具描述 |
| GET | `/agent/tools/search-interview-knowledge` | 调用检索工具 |
| GET | `/interview/media/capabilities` | 实际媒体能力开关 |

## 创建面试

`POST /interview/session` 请求体：

```json
{
  "jobTitle": "AI应用开发实习生",
  "difficulty": "MEDIUM",
  "questionCount": 5,
  "resume": {"skills": ["Python", "RAG"], "targetRole": "AI应用开发实习生"}
}
```

可选 `knowledgeBase` 使用 `/rag/doc/list` 返回的实际文档标题。创建后先展示自我介绍，再进入技术问题。

## SSE

`POST /interview/stream/answer/{sessionId}`，请求体示例：

```json
{"answer":"我会先明确知识库来源，再校验数据并评估检索结果。"}
```

SSE 消息内 JSON 类型：

```json
{"type":"token","text":"题目增量内容"}
{"type":"done","data":{}}
{"type":"error","message":"错误说明"}
```

`done.data` 实际为完整 `InterviewSessionResponse`。前端用 `fetch` 读取流，因为原生 `EventSource` 无法发送这个 POST 请求。

## 本地词法检索

`GET /knowledge/interview-rag/search` 参数：

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| `query` | 是 | 问题或知识点，最长 500 字符 |
| `targetRole` | 否 | 岗位，服务端归一化 |
| `difficulty` | 否 | 建议使用 `EASY`、`MEDIUM`、`HARD` |
| `topK` | 否 | 默认 5，范围 1—5 |

结果包含条目、来源行号、岗位、难度、规则相关性分数及上下文。分数不是概率或余弦相似度。低相关查询可以返回空列表。
