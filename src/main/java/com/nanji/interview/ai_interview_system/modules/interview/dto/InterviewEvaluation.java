package com.nanji.interview.ai_interview_system.modules.interview.dto;

import java.util.List;

/**
 * 面试评价（迁移自 feature/interview-agent，为结束面试与报告页的统一评价结构）。
 */
public class InterviewEvaluation {

    private int score;
    private String summary;
    private String strengths;
    private String weaknesses;

    // 五维能力评分（0~100 整数）
    private int professionalAbility;
    private int knowledgeCoverage;
    private int jobMatch;
    private int communication;
    private int logicalThinking;

    // 改进建议列表
    private List<String> suggestions;

    /** 本次面试的应聘岗位（报告页展示用，随报告一起落库） */
    private String jobTitle;

    /** 本次面试难度：EASY / MEDIUM / HARD */
    private String difficulty;

    /**
     * 逐题评价：每道技术题（不含开场自我介绍）的得分与 AI 点评。
     * 由结束面试时的评价模型产出，并与问答原文合并后落库。
     */
    private List<QuestionReview> questionReviews;

    /** 单道题的评分与点评 */
    public static class QuestionReview {

        /** 第几题（从 1 开始，不含开场自我介绍） */
        private int index;

        /** 题目原文（由后端从问答记录回填，模型无需返回） */
        private String question;

        /** 候选人回答原文（由后端回填） */
        private String answer;

        /** 参考答案（仅「基于知识库面试」命中时有值） */
        private String referenceAnswer;

        /** 该题得分 0~100 */
        private Integer score;

        /** 该题的 AI 点评 */
        private String comment;

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
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
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public List<QuestionReview> getQuestionReviews() {
        return questionReviews;
    }

    public void setQuestionReviews(List<QuestionReview> questionReviews) {
        this.questionReviews = questionReviews;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getStrengths() {
        return strengths;
    }

    public void setStrengths(String strengths) {
        this.strengths = strengths;
    }

    public String getWeaknesses() {
        return weaknesses;
    }

    public void setWeaknesses(String weaknesses) {
        this.weaknesses = weaknesses;
    }

    public int getProfessionalAbility() {
        return professionalAbility;
    }

    public void setProfessionalAbility(int professionalAbility) {
        this.professionalAbility = professionalAbility;
    }

    public int getKnowledgeCoverage() {
        return knowledgeCoverage;
    }

    public void setKnowledgeCoverage(int knowledgeCoverage) {
        this.knowledgeCoverage = knowledgeCoverage;
    }

    public int getJobMatch() {
        return jobMatch;
    }

    public void setJobMatch(int jobMatch) {
        this.jobMatch = jobMatch;
    }

    public int getCommunication() {
        return communication;
    }

    public void setCommunication(int communication) {
        this.communication = communication;
    }

    public int getLogicalThinking() {
        return logicalThinking;
    }

    public void setLogicalThinking(int logicalThinking) {
        this.logicalThinking = logicalThinking;
    }

    public List<String> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<String> suggestions) {
        this.suggestions = suggestions;
    }
}
