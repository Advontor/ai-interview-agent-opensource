package com.nanji.interview.ai_interview_system.modules.practice.dto;

/**
 * 刷题评分结果：得分 + AI 点评 + 参考答案 + 题目原文。
 */
public class PracticeGradeResult {

    /** 该题得分 0~100 */
    private Integer score;

    /** AI 点评（指出答对了什么、缺了什么） */
    private String comment;

    /** 参考答案（来自题库原文） */
    private String referenceAnswer;

    /** 题目原文（回显用） */
    private String question;

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getReferenceAnswer() {
        return referenceAnswer;
    }

    public void setReferenceAnswer(String referenceAnswer) {
        this.referenceAnswer = referenceAnswer;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }
}
