import request from '../utils/request'

// 学习资源实体（与后端 LearningResource 字段保持一致）
export interface LearningResource {
  id: number
  title: string
  url: string
  category: string
  relatedSkill: string
  description: string
  createdAt: string
}

// 获取学习资源列表
export const getLearningList = (): Promise<LearningResource[]> =>
  request.get('/learning/list') as unknown as Promise<LearningResource[]>

// 按分类获取学习资源
export const getLearningByCategory = (category: string): Promise<LearningResource[]> =>
  request.get(`/learning/category/${category}`) as unknown as Promise<LearningResource[]>

// 按关联技能获取学习资源（用于匹配面试薄弱点）
export const getLearningBySkill = (skill: string): Promise<LearningResource[]> =>
  request.get(`/learning/skill/${skill}`) as unknown as Promise<LearningResource[]>

// 获取单个资源详情
export const getLearningDetail = (id: number): Promise<LearningResource> =>
  request.get(`/learning/${id}`) as unknown as Promise<LearningResource>
