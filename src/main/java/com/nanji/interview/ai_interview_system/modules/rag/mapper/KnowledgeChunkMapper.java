package com.nanji.interview.ai_interview_system.modules.rag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nanji.interview.ai_interview_system.modules.rag.entity.KnowledgeChunk;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * RAG知识库分块 Mapper
 */
@Mapper
public interface KnowledgeChunkMapper extends BaseMapper<KnowledgeChunk> {

    /**
     * 按岗位标签查询所有分块
     */
    @Select("SELECT * FROM knowledge_chunk WHERE position = #{position} ORDER BY doc_title, chunk_index")
    List<KnowledgeChunk> selectByPosition(@Param("position") String position);

    /**
     * 按文档标题查询所有分块
     */
    @Select("SELECT * FROM knowledge_chunk WHERE doc_title = #{docTitle} ORDER BY chunk_index")
    List<KnowledgeChunk> selectByDocTitle(@Param("docTitle") String docTitle);

    /**
     * 全文检索（content模糊匹配）
     */
    @Select("SELECT * FROM knowledge_chunk WHERE content LIKE CONCAT('%', #{keyword}, '%') ORDER BY position, doc_title, chunk_index")
    List<KnowledgeChunk> searchByKeyword(@Param("keyword") String keyword);

    /**
     * 查询所有不重复的文档标题
     */
    @Select("SELECT DISTINCT doc_title FROM knowledge_chunk ORDER BY doc_title")
    List<String> selectDistinctDocTitles();

    /**
     * 删除指定文档的所有分块
     */
    @Select("DELETE FROM knowledge_chunk WHERE doc_title = #{docTitle}")
    int deleteByDocTitle(@Param("docTitle") String docTitle);

    /**
     * 「基于知识库面试」的出题检索：在指定题库内按关键词命中后取样。
     * keywords 为空时按 chunk_index 顺序取样（保证内容仍然来自该题库）。
     */
    @Select("""
            <script>
            SELECT * FROM knowledge_chunk
            WHERE doc_title = #{docTitle}
            <if test="keywords != null and keywords.size() > 0">
                AND (
                <foreach collection="keywords" item="kw" separator=" OR ">
                    content LIKE CONCAT('%', #{kw}, '%')
                </foreach>
                )
            </if>
            ORDER BY chunk_index
            LIMIT #{limit}
            </script>
            """)
    List<KnowledgeChunk> selectForInterview(@Param("docTitle") String docTitle,
                                            @Param("keywords") List<String> keywords,
                                            @Param("limit") int limit);
}
