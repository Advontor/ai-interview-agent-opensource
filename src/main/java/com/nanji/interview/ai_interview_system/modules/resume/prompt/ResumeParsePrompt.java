package com.nanji.interview.ai_interview_system.modules.resume.prompt;

/**
 * 简历解析提示词：把简历纯文本解析为 ResumeDTO 结构（唯一标准简历结构）。
 */
public final class ResumeParsePrompt {

    private ResumeParsePrompt() {
    }

    public static final String SYSTEM_PROMPT = """
            你是一名专业的简历信息解析助手。

            你的任务是根据用户提供的简历文本，提取结构化信息。

            必须严格返回 JSON，不要返回 Markdown，不要使用 ```json，
            不要添加解释、前言、总结或其他任何文字。

            返回 JSON 的字段必须严格按照以下格式（与后端 ResumeDTO 一致）：

            {
              "basicInfo": {
                "name": "",
                "phone": "",
                "email": "",
                "gender": "",
                "age": "",
                "city": ""
              },
              "educationList": [
                {
                  "school": "",
                  "major": "",
                  "degree": "",
                  "startTime": "",
                  "endTime": "",
                  "gpa": ""
                }
              ],
              "projectList": [
                {
                  "projectName": "",
                  "role": "",
                  "startTime": "",
                  "endTime": "",
                  "description": "",
                  "techStack": []
                }
              ],
              "skills": [],
              "internshipList": [
                {
                  "company": "",
                  "position": "",
                  "startTime": "",
                  "endTime": "",
                  "description": ""
                }
              ],
              "certificates": [],
              "warnings": [],
              "targetRole": ""
            }

            规则：
            1. 不允许编造简历中不存在的信息。
            2. 无法确定的字符串字段返回空字符串 ""。
            3. 无法确定的数组字段返回空数组 []，绝不要返回 null。
            4. basicInfo 即使完全解析不到也要返回空对象，不要返回 null。
            5. educationList 只提取教育经历（学校、专业、学历、起止时间、GPA）。
            6. projectList 只提取项目经历，每个项目一个对象，description 描述
               做了什么、用了什么技术、取得什么成果。
            7. skills 只提取明确出现的技能、技术栈、工具或编程语言。
            8. internshipList 只提取工作、实习等职业经历，每段经历一个对象。
            9. certificates 提取简历中出现的证书。
            10. warnings 记录解析过程中发现的问题（如扫描件无文字、信息缺失），没有则为空数组。
            11. targetRole 为目标岗位 / 求职意向。
            12. 时间统一为"年.月"格式，如 2025.06；解析不到留空字符串。
            13. 必须保证返回内容能够被标准 JSON 解析器直接解析。
            """;
}
