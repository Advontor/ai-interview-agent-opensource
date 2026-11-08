package com.nanji.interview.ai_interview_system.modules.knowledge;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

/** 无外部费用的本地词项向量检索。词项包括英文词和中文二元组，并非语义Embedding模型。 */
@Service
public class KnowledgeService {
 public record Chunk(String knowledgeId,String title,String content,String targetRole,String difficulty,List<String> tags,String source) {}
 public record Hit(String knowledgeId,String title,String content,String targetRole,String difficulty,List<String> tags,String source,double score) {}
 public record Context(String mode,String version,String targetRole,List<Hit> chunks,String context) {}
 public record Stats(String mode,String version,int chunkCount,List<String> roles,List<String> difficulties) {}
 private final List<Chunk> chunks;
 private final Map<String,Map<String,Double>> vectors=new HashMap<>();
 public KnowledgeService(ObjectMapper mapper) throws IOException {
  try(var in=new ClassPathResource("knowledge/week2.json").getInputStream()) {
   chunks=List.copyOf(mapper.readValue(in,new TypeReference<List<Chunk>>(){}));
  }
  Set<String> ids=new HashSet<>();
  for(Chunk chunk:chunks) {
   if(!ids.add(chunk.knowledgeId()) || chunk.content().isBlank() || !chunk.source().startsWith("https://"))
    throw new IllegalStateException("知识库数据无效："+chunk.knowledgeId());
   vectors.put(chunk.knowledgeId(),vector(chunk.title()+" "+String.join(" ",chunk.tags())+" "+chunk.content()));
  }
 }
 static Map<String,Double> vector(String text) {
  Map<String,Double> result=new HashMap<>();
  var english=Pattern.compile("[a-z0-9]+(?:[.+#][a-z0-9]+)*").matcher(text.toLowerCase(Locale.ROOT));
  while(english.find()) result.merge(english.group(),1.0,Double::sum);
  var chinese=Pattern.compile("[\\p{IsHan}]+").matcher(text);
  while(chinese.find()) {String run=chinese.group();for(int i=0;i<run.length()-1;i++) result.merge(run.substring(i,i+2),1.0,Double::sum);}
  return result;
 }
 static double cosine(Map<String,Double>a,Map<String,Double>b) {
  double dot=a.entrySet().stream().mapToDouble(e->e.getValue()*b.getOrDefault(e.getKey(),0.0)).sum();
  double normA=Math.sqrt(a.values().stream().mapToDouble(x->x*x).sum());
  double normB=Math.sqrt(b.values().stream().mapToDouble(x->x*x).sum());
  return normA==0 || normB==0 ? 0 : dot/normA/normB;
 }
 private static String role(String role) {
  if(role==null) return "";
  String r=role.toLowerCase(Locale.ROOT);
  if(r.contains("全栈") || r.equals("full_stack")) return "FULL_STACK";
  if(r.contains("前端") || r.equals("frontend")) return "FRONTEND";
  if(r.contains("java") || r.equals("java_backend")) return "JAVA_BACKEND";
  return "";
 }
 public List<Hit> retrieve(String targetRole,String difficulty,String query,int topK) {
  if(topK<1 || topK>5) throw new IllegalArgumentException("topK必须为1至5");
  String role=role(targetRole);
  if(role.isEmpty() || query==null || query.isBlank()) return List.of();
  Map<String,Double> queryVector=vector(query);
  return chunks.stream().filter(c->role.equals("FULL_STACK") || c.targetRole().equals(role))
   .filter(c->difficulty==null || difficulty.isBlank() || c.difficulty().equals(difficulty))
   .map(c->new Hit(c.knowledgeId(),c.title(),c.content(),c.targetRole(),c.difficulty(),c.tags(),c.source(),cosine(queryVector,vectors.get(c.knowledgeId()))))
   .filter(h->h.score()>=0.06).sorted(Comparator.comparingDouble(Hit::score).reversed().thenComparing(Hit::knowledgeId))
   .limit(topK).toList();
 }
 public Context context(String targetRole,String difficulty,String query,int topK) {
  List<Hit> hits=retrieve(targetRole,difficulty,query,topK);
  StringBuilder text=new StringBuilder();
  for(Hit hit:hits) text.append("[知识点 ").append(hit.knowledgeId()).append("] ").append(hit.title())
   .append("\n来源：").append(hit.source()).append("\n").append(hit.content()).append("\n");
  return new Context("LOCAL_LEXICAL_VECTOR","week2-v1",targetRole,hits,text.toString());
 }
 public Stats stats() {
  return new Stats("LOCAL_LEXICAL_VECTOR", "week2-v1", chunks.size(),
   chunks.stream().map(Chunk::targetRole).distinct().sorted().toList(),
   chunks.stream().map(Chunk::difficulty).distinct().sorted().toList());
 }
}
