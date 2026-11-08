package com.nanji.interview.ai_interview_system.modules.interview.service.impl;
import com.nanji.interview.ai_interview_system.modules.interview.dto.*;
import com.nanji.interview.ai_interview_system.modules.interview.entity.*;
import com.nanji.interview.ai_interview_system.modules.interview.mapper.InterviewSessionMapper;
import com.nanji.interview.ai_interview_system.modules.interview.service.*;
import com.nanji.interview.ai_interview_system.modules.report.service.ReportService;
import com.nanji.interview.ai_interview_system.infrastructure.llm.LlmClient;
import com.nanji.interview.ai_interview_system.common.exception.BusinessException;
import com.nanji.interview.ai_interview_system.modules.knowledge.CsvInterviewKnowledgeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class InterviewSessionServiceTest {
 LlmClient llm; InterviewMessageService messages; ReportService reports;
 InterviewSessionMapper mapper; InterviewSessionServiceImpl service; InterviewSession session;
 @BeforeEach void setup() {
  TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(),"test"),InterviewSession.class);
  llm=mock(LlmClient.class); messages=mock(InterviewMessageService.class); reports=mock(ReportService.class);
  mapper=mock(InterviewSessionMapper.class);
  PlatformTransactionManager tx=mock(PlatformTransactionManager.class);
  when(tx.getTransaction(any(TransactionDefinition.class))).thenReturn(new SimpleTransactionStatus());
  service=new InterviewSessionServiceImpl(llm,new ObjectMapper(),messages,reports,tx);
  ReflectionTestUtils.setField(service,"baseMapper",mapper);
  session=new InterviewSession(); session.setId(1L); session.setStatus("ONGOING"); session.setJobTitle("Java后端");
  session.setDifficulty("HARD"); session.setResumeJson("{项目:商品系统}"); session.setAgentStateJson("{}");
  when(mapper.selectById(1L)).thenReturn(session);
  when(mapper.update(any(InterviewSession.class),any())).thenReturn(1);
  when(messages.save(any())).thenReturn(true);
  when(messages.listBySessionId(1L)).thenReturn(List.of(message("ai","请介绍项目")));
 }
 InterviewMessage message(String sender,String content) {
  InterviewMessage m=new InterviewMessage();m.setSender(sender);m.setContent(content);m.setSessionId(1L);return m;
 }
 SubmitAnswerRequest request() {
  SubmitAnswerRequest r=new SubmitAnswerRequest();r.setSessionId(1L);r.setExpectedRevision(0L);r.setAnswer("我使用Redis缓存商品详情，并设置过期时间");return r;
 }
 @Test void nextTopicUsesAlreadyGeneratedQuestionAndPersistsBothMessages() {
  when(llm.chat(anyString(),anyString())).thenReturn("{\"verdict\":\"VALID\",\"action\":\"NEXT_TOPIC\",\"nextQuestion\":\"如何测试接口？\"}");
  InterviewSessionResponse response=service.submitAnswer(request());
  assertEquals("如何测试接口？",response.getCurrentQuestion());assertEquals(1,response.getRevision());
  verify(llm,times(1)).chat(anyString(),anyString());verify(messages,times(2)).save(any());
  assertTrue(session.getAgentStateJson().contains("Redis"));
 }
 @Test void oldPageRejectedBeforeSpendingTokens() {
  SubmitAnswerRequest r=request();r.setExpectedRevision(9L);
  assertThrows(BusinessException.class,()->service.submitAnswer(r));verifyNoInteractions(llm);
  verify(messages,never()).save(any());
 }
 @Test void providerFailureDoesNotWriteAnswerOrState() {
  when(llm.chat(anyString(),anyString())).thenThrow(new IllegalStateException("timeout"));
  assertThrows(IllegalStateException.class,()->service.submitAnswer(request()));
  verify(messages,never()).save(any());verify(mapper,never()).update(any(InterviewSession.class),any());
 }
 @Test void nextQuestionFailureDoesNotWriteHalfAnswerOrState() {
  when(llm.chat(anyString(),anyString()))
    .thenReturn("{\"verdict\":\"VALID\",\"action\":\"NEXT_TOPIC\",\"nextQuestion\":\"\"}")
    .thenThrow(new IllegalStateException("next question timeout"));
  assertThrows(IllegalStateException.class,()->service.submitAnswer(request()));
  verify(messages,never()).save(any());verify(mapper,never()).update(any(InterviewSession.class),any());
 }
 @Test void concurrentSnapshotChangeRejectsBeforeMessages() {
  when(llm.chat(anyString(),anyString())).thenReturn("{\"verdict\":\"VALID\",\"action\":\"NEXT_TOPIC\",\"nextQuestion\":\"如何测试接口？\"}");
  when(mapper.update(any(InterviewSession.class),any())).thenReturn(0);
  assertThrows(BusinessException.class,()->service.submitAnswer(request()));verify(messages,never()).save(any());
 }
 @Test void duplicateQuestionGetsOneRepairThenFallback() {
  when(llm.chat(anyString(),anyString())).thenReturn("{\"verdict\":\"VALID\",\"action\":\"NEXT_TOPIC\",\"nextQuestion\":\"请介绍项目\"}","请介绍项目");
  assertNotEquals("请介绍项目",service.submitAnswer(request()).getCurrentQuestion());
  verify(llm,times(2)).chat(anyString(),anyString());
 }
 @Test void ragReferenceAnswerNeverEntersInterviewerPrompt() {
   CsvInterviewKnowledgeService knowledge=mock(CsvInterviewKnowledgeService.class);
   CsvInterviewKnowledgeService.Hit hit=new CsvInterviewKnowledgeService.Hit(
     "row-1","你如何排查接口超时？","这是知识库虚构的第一人称经历","后端开发","HARD",List.of("接口","排查"),9.0);
   when(knowledge.search(anyString(),anyString(),anyString(),anyInt())).thenReturn(
     new CsvInterviewKnowledgeService.SearchResult("后端开发","HARD","接口",List.of(hit),"unused"));
   service=new InterviewSessionServiceImpl(llm,new ObjectMapper(),messages,reports,
     mockTransactionManager(),knowledge);
   ReflectionTestUtils.setField(service,"baseMapper",mapper);
   when(llm.chat(anyString(),anyString())).thenReturn(
     "{\"verdict\":\"VALID\",\"action\":\"NEXT_TOPIC\",\"nextQuestion\":\"如何测试接口边界？\"}");
   service.submitAnswer(request());
   ArgumentCaptor<String> context=ArgumentCaptor.forClass(String.class);
   verify(llm).chat(anyString(),context.capture());
   assertTrue(context.getValue().contains("你如何排查接口超时"));
   assertFalse(context.getValue().contains("这是知识库虚构的第一人称经历"));
 }
 @Test void restoredContextIncludesEarlierAnswersRoleAndDifficulty() {
  when(messages.listBySessionId(1L)).thenReturn(List.of(message("ai","项目是什么"),message("user","用Vue开发购物车"),message("ai","如何测试")));
  when(llm.chat(anyString(),anyString())).thenReturn("{\"verdict\":\"VALID\",\"action\":\"FOLLOW_UP\",\"nextQuestion\":\"测试边界如何选择？\"}");
  service.submitAnswer(request());
  ArgumentCaptor<String> context=ArgumentCaptor.forClass(String.class);verify(llm).chat(anyString(),context.capture());
  assertTrue(context.getValue().contains("用Vue开发购物车"));assertTrue(context.getValue().contains("HARD"));assertTrue(context.getValue().contains("Java后端"));
 }
 @Test void hardLimitAppliesToEveryVerdict() {
  for(String verdict:List.of("VALID","INVALID","PARTIAL","SKIP")) {
   AgentState s=new AgentState();s.setEffectiveQuestionCount(15);AnswerAssessment a=new AnswerAssessment();a.setVerdict(verdict);
   assertEquals("FINISH",InterviewSessionServiceImpl.decide(s,a));
  }
 }
 @Test void partialCannotExceedFollowupLimitAndEarlyFinishIsRejected() {
  AgentState s=new AgentState();s.setCurrentTopicFollowUpCount(2);s.setEffectiveQuestionCount(3);
  AnswerAssessment a=new AnswerAssessment();a.setVerdict("PARTIAL");
  assertEquals("NEXT_TOPIC",InterviewSessionServiceImpl.decide(s,a));
  a.setVerdict("VALID");a.setAction("FINISH");assertEquals("NEXT_TOPIC",InterviewSessionServiceImpl.decide(s,a));
 }
 @Test void manualFinishCannotBypassMinimumQuestionLimit() {
  when(messages.listBySessionId(1L)).thenReturn(List.of(message("ai","项目是什么"),message("user","商品系统")));
  BusinessException error=assertThrows(BusinessException.class,()->service.finishInterview(1L));
  assertTrue(error.getMessage().contains("8轮"));
  verifyNoInteractions(llm);verify(reports,never()).saveEvaluation(anyLong(),any());
 }
 @Test void restoreReturnsAnswerCountAndCurrentQuestion() {
  when(messages.listBySessionId(1L)).thenReturn(List.of(message("ai","项目是什么"),message("user","商品系统"),message("ai","如何测试")));
  InterviewSessionResponse r=service.getSessionResponse(1L);
  assertEquals(1,r.getAnsweredCount());assertEquals(2,r.getCurrentQuestionIndex());assertEquals("如何测试",r.getCurrentQuestion());
 }
 private PlatformTransactionManager mockTransactionManager() {
  PlatformTransactionManager tx=mock(PlatformTransactionManager.class);
  when(tx.getTransaction(any(TransactionDefinition.class))).thenReturn(new SimpleTransactionStatus());
  return tx;
 }
}
