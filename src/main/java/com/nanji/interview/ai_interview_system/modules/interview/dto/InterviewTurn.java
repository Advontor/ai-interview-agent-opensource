package com.nanji.interview.ai_interview_system.modules.interview.dto;

/**
 * 一轮完整问答（题目 + 回答 + 参考答案），用于生成评价、报告页展示与 PDF 报告。
 * <p>
 * referenceAnswer 仅在「基于知识库面试」模式下从知识库命中后填入；普通面试为 null。
 */
public class InterviewTurn {

    private String question;
    private String answer;
    private String referenceAnswer;

    public InterviewTurn() {
    }

    public InterviewTurn(String question, String answer) {
        this.question = question;
        this.answer = answer;
    }

    public InterviewTurn(String question, String answer, String referenceAnswer) {
        this.question = question;
        this.answer = answer;
        this.referenceAnswer = referenceAnswer;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getReferenceAnswer() {
        return referenceAnswer;
    }

    public void setReferenceAnswer(String referenceAnswer) {
        this.referenceAnswer = referenceAnswer;
    }
}
