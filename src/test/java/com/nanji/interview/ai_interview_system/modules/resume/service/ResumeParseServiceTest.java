package com.nanji.interview.ai_interview_system.modules.resume.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nanji.interview.ai_interview_system.common.exception.BusinessException;
import com.nanji.interview.ai_interview_system.infrastructure.llm.LlmClient;
import com.nanji.interview.ai_interview_system.modules.resume.dto.ResumeDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ResumeParseServiceTest {

    private LlmClient llmClient;
    private ResumeParseService service;

    @BeforeEach
    void setUp() {
        llmClient = mock(LlmClient.class);
        service = new ResumeParseService(llmClient, new ObjectMapper());
    }

    @Test
    void normalizesMissingCollectionsNullElementsAndBlankStrings() {
        when(llmClient.chat(anyString(), anyString())).thenReturn("""
                模型说明：
                {
                  "basicInfo": {"name": " 张三 ", "phone": "", "age": null},
                  "educationList": [null, {"school": " 学校 ", "major": null}],
                  "projectList": [null, {"projectName": "项目 A", "techStack": null}],
                  "skills": [" Java ", null, ""],
                  "internshipList": null,
                  "certificates": [null, " AWS "],
                  "warnings": null,
                  "targetRole": ""
                }
                """);

        ResumeDTO result = service.parse("张三的简历文本");

        assertEquals("张三", result.getBasicInfo().getName());
        assertNull(result.getBasicInfo().getPhone());
        assertEquals(1, result.getEducationList().size());
        assertEquals("学校", result.getEducationList().get(0).getSchool());
        assertNull(result.getEducationList().get(0).getMajor());
        assertEquals(1, result.getProjectList().size());
        assertNotNull(result.getProjectList().get(0).getTechStack());
        assertEquals(0, result.getProjectList().get(0).getTechStack().size());
        assertEquals(java.util.List.of("Java"), result.getSkills());
        assertEquals(java.util.List.of("AWS"), result.getCertificates());
        assertEquals(java.util.List.of(), result.getInternshipList());
        assertEquals(java.util.List.of(), result.getWarnings());
        assertNull(result.getTargetRole());
    }

    @Test
    void rejectsRootArrayInsteadOfExtractingAnInnerObject() {
        when(llmClient.chat(anyString(), anyString())).thenReturn("[{}]");

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.parse("简历文本"));

        assertEquals(true, error.getMessage().contains("根节点"));
    }

    @Test
    void rejectsUnrepairableFieldType() {
        when(llmClient.chat(anyString(), anyString())).thenReturn("{\"skills\":{}}");

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.parse("简历文本"));

        assertEquals(true, error.getMessage().contains("skills"));
    }

    @Test
    void rejectsEmptyAndOversizedInputBeforeCallingModel() {
        assertThrows(BusinessException.class, () -> service.parse(" \n\t"));
        assertThrows(BusinessException.class,
                () -> service.parse("x".repeat(ResumeParseService.MAX_RESUME_TEXT_LENGTH + 1)));

        verifyNoInteractions(llmClient);
    }

    @Test
    void rejectsModelResponseWithoutCompleteJsonObject() {
        when(llmClient.chat(anyString(), anyString())).thenReturn("{\"basicInfo\":");

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.parse("简历文本"));

        assertEquals(true, error.getMessage().contains("JSON"));
    }
}
