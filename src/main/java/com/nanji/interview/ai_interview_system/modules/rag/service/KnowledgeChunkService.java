package com.nanji.interview.ai_interview_system.modules.rag.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nanji.interview.ai_interview_system.modules.rag.entity.KnowledgeChunk;

import java.util.List;

/**
 * RAG知识库分块 Service 接口
 */
public interface KnowledgeChunkService extends IService<KnowledgeChunk> {

    /**
     * 按岗位标签查询所有分块
     */
    List<KnowledgeChunk> listByPosition(String position);

    /**
     * 按文档标题查询所有分块
     */
    List<KnowledgeChunk> listByDocTitle(String docTitle);

    /**
     * 全文检索
     */
    List<KnowledgeChunk> searchByKeyword(String keyword);

    /**
     * 查询所有不重复的文档标题
     */
    List<String> listDocTitles();

    /**
     * 删除指定文档的所有分块
     */
    boolean deleteByDocTitle(String docTitle);

    /**
     * 「基于知识库面试」的出题检索：在指定题库内检索可作为出题参考的真题。
     * 策略：关键词命中 → 同难度优先 → 关键词全部落空时退化为按序取样。
     *
     * @param docTitle   题库文档标题
     * @param keywords   检索关键词（一般来自应聘岗位与简历技能），可为空
     * @param difficulty 面试难度（EASY / MEDIUM / HARD），用于优先挑选同难度题目，可为空
     * @param limit      返回条数上限
     * @return 参考题目列表；未命中或无该题库时返回空列表
     */
    List<KnowledgeChunk> retrieveForInterview(String docTitle, List<String> keywords, String difficulty, int limit);
}
