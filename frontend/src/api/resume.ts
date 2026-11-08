import request from '../utils/request'

// 与后端 modules/resume/dto 下的 DTO 字段保持一致
export interface BasicInfo {
  name?: string
  phone?: string
  email?: string
  gender?: string
  age?: string
  city?: string
}

export interface Education {
  school?: string
  major?: string
  degree?: string
  startTime?: string
  endTime?: string
  gpa?: string
}

export interface Internship {
  company?: string
  position?: string
  startTime?: string
  endTime?: string
  description?: string
}

export interface Project {
  projectName?: string
  role?: string
  startTime?: string
  endTime?: string
  description?: string
  techStack?: string[]
}

// 简历解析结果（/resume/upload 的返回体）
export interface ResumeDTO {
  basicInfo?: BasicInfo
  educationList?: Education[]
  projectList?: Project[]
  skills?: string[]
  internshipList?: Internship[]
  certificates?: string[]
  warnings?: string[]
  targetRole?: string
}

// 上传简历文件（PDF/Word），后端解析为结构化简历
export const uploadResume = (file: File): Promise<ResumeDTO> => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/resume/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    // 上传要先把整个文件送到服务端（公网弱网下可能耗时很久），
    // 之后后端还要调大模型做结构化解析；Nginx 侧 read_timeout 为 300s，
    // 这里同步放宽到 5 分钟，避免弱网用户在文件上传阶段就被前端判超时。
    timeout: 300000
  }) as unknown as Promise<ResumeDTO>
}

// 直接提交简历纯文本进行解析（联调/调试用）
export const parseResumeText = (text: string) => {
  return request.post('/resume/parse', text, {
    headers: {
      'Content-Type': 'text/plain'
    }
  })
}

// 新增简历（保存到数据库）
export const addResume = (resume: any) => {
  return request.post('/resume', resume)
}

// 根据用户id查询所有简历
export const getResumeList = (userId: number) => {
  return request.get(`/resume/list/${userId}`)
}

// 根据简历id查询详情
export const getResumeDetail = (id: number) => {
  return request.get(`/resume/${id}`)
}

// 删除简历
export const deleteResume = (id: number) => {
  return request.delete(`/resume/${id}`)
}
