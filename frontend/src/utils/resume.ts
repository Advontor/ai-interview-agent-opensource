import type { BasicInfo, Education, Internship, Project, ResumeDTO } from '../api/resume'

/** 简历解析的可选扩展结果：后端可能返回，也可能整体缺省，因此字段全部可选。 */
export interface ResumeInterviewDefaults {
  targetRole?: string
  targetLevel?: TargetLevel
  difficulty?: Difficulty
  focusTopics?: string[]
  interviewStyles?: string[]
}

export interface ResumeScoreResult {
  totalScore?: number
  breakdown?: Record<string, number>
  explanations?: Record<string, string>
  strengths?: string[]
  risks?: string[]
  recommendedRoles?: string[]
  recommendedTopics?: string[]
  techStack?: string[]
  defaults?: ResumeInterviewDefaults
}

export interface ResumeAnalysisResult {
  profile?: ResumeDTO
  score?: ResumeScoreResult
}

/**
 * Resume data comes from an LLM and from sessionStorage, so it must be treated
 * as untrusted input. These helpers deliberately drop values with an unknown
 * shape instead of inventing a value that could be used to configure an
 * interview incorrectly.
 */
export const TARGET_LEVELS = ['应届生', '初级', '中级', '高级'] as const
export type TargetLevel = (typeof TARGET_LEVELS)[number]

export const DIFFICULTIES = ['EASY', 'MEDIUM', 'HARD', 'EXPERT'] as const
export type Difficulty = (typeof DIFFICULTIES)[number]

export const emptyBasicInfo = (): BasicInfo => ({
  name: undefined,
  phone: undefined,
  email: undefined,
  gender: undefined,
  age: undefined,
  city: undefined,
})

export const emptyResume = (targetRole: string | null | undefined = undefined): ResumeDTO => ({
  basicInfo: emptyBasicInfo(),
  educationList: [],
  projectList: [],
  skills: [],
  internshipList: [],
  certificates: [],
  warnings: [],
  targetRole: normalizeString(targetRole),
})

export function isRecord(value: unknown): value is Record<string, unknown> {
  return Boolean(value) && typeof value === 'object' && !Array.isArray(value)
}

export function normalizeString(value: unknown): string | undefined {
  if (typeof value === 'string') {
    const result = value.trim()
    return result || undefined
  }
  // Numeric years/ages occasionally appear in otherwise valid model output.
  // Converting a primitive is deterministic; objects and booleans are dropped.
  if (typeof value === 'number' && Number.isFinite(value)) return String(value)
  return undefined
}

export function normalizeStringList(value: unknown): string[] {
  if (!Array.isArray(value)) return []
  return value
    .filter((item): item is string => typeof item === 'string')
    .map(item => item.trim())
    .filter(Boolean)
    .filter((item, index, list) => list.indexOf(item) === index)
}

const normalizeBasicInfo = (value: unknown): BasicInfo => {
  const input = isRecord(value) ? value : {}
  return {
    name: normalizeString(input.name),
    phone: normalizeString(input.phone),
    email: normalizeString(input.email),
    gender: normalizeString(input.gender),
    age: normalizeString(input.age),
    city: normalizeString(input.city),
  }
}

const normalizeEducation = (value: unknown): Education => {
  const input = isRecord(value) ? value : {}
  return {
    school: normalizeString(input.school),
    major: normalizeString(input.major),
    degree: normalizeString(input.degree),
    startTime: normalizeString(input.startTime),
    endTime: normalizeString(input.endTime),
    gpa: normalizeString(input.gpa),
  }
}

const normalizeProject = (value: unknown): Project => {
  const input = isRecord(value) ? value : {}
  return {
    projectName: normalizeString(input.projectName),
    role: normalizeString(input.role),
    startTime: normalizeString(input.startTime),
    endTime: normalizeString(input.endTime),
    description: normalizeString(input.description),
    techStack: normalizeStringList(input.techStack),
  }
}

const normalizeInternship = (value: unknown): Internship => {
  const input = isRecord(value) ? value : {}
  return {
    company: normalizeString(input.company),
    position: normalizeString(input.position),
    startTime: normalizeString(input.startTime),
    endTime: normalizeString(input.endTime),
    description: normalizeString(input.description),
  }
}

const recordList = (value: unknown): unknown[] => Array.isArray(value) ? value : []

export function normalizeResume(input: unknown): ResumeDTO {
  const value = isRecord(input) ? input : {}
  return {
    basicInfo: normalizeBasicInfo(value.basicInfo),
    educationList: recordList(value.educationList).map(normalizeEducation),
    projectList: recordList(value.projectList).map(normalizeProject),
    skills: normalizeStringList(value.skills),
    internshipList: recordList(value.internshipList).map(normalizeInternship),
    certificates: normalizeStringList(value.certificates),
    warnings: normalizeStringList(value.warnings),
    targetRole: normalizeString(value.targetRole),
  }
}

export function clampScore(value: unknown): number {
  const number = typeof value === 'number' ? value : typeof value === 'string' && value.trim() ? Number(value) : NaN
  if (!Number.isFinite(number)) return 0
  return Math.max(0, Math.min(100, Math.round(number)))
}

function normalizeScoreRecord(value: unknown): Record<string, number> {
  if (!isRecord(value)) return {}
  return Object.fromEntries(Object.entries(value).map(([key, item]) => [key, clampScore(item)]))
}

export function normalizeTargetLevel(value: unknown, fallback: TargetLevel = '应届生'): TargetLevel {
  const text = normalizeString(value)?.toUpperCase()
  const aliases: Record<string, TargetLevel> = {
    '应届生': '应届生', ENTRY: '应届生', GRADUATE: '应届生', FRESHER: '应届生', JUNIOR: '初级',
    '初级': '初级', MID: '中级', MIDDLE: '中级', '中级': '中级', SENIOR: '高级', '高级': '高级',
  }
  return (text && aliases[text]) || fallback
}

export function normalizeDifficulty(value: unknown, fallback: Difficulty = 'MEDIUM'): Difficulty {
  const text = normalizeString(value)?.toUpperCase()
  return text && (DIFFICULTIES as readonly string[]).includes(text) ? text as Difficulty : fallback
}

export function normalizeDefaults(value: unknown): ResumeInterviewDefaults {
  const input = isRecord(value) ? value : {}
  return {
    targetRole: normalizeString(input.targetRole),
    targetLevel: normalizeTargetLevel(input.targetLevel),
    difficulty: normalizeDifficulty(input.difficulty),
    focusTopics: normalizeStringList(input.focusTopics),
    interviewStyles: normalizeStringList(input.interviewStyles),
  }
}

export function normalizeScore(input: unknown): ResumeScoreResult {
  const value = isRecord(input) ? input : {}
  const breakdown = normalizeScoreRecord(value.breakdown)
  const explanationsInput = isRecord(value.explanations) ? value.explanations : {}
  const explanations = Object.fromEntries(Object.entries(explanationsInput).flatMap(([key, item]) => {
    const text = normalizeString(item)
    return text ? [[key, text]] : []
  }))
  return {
    totalScore: clampScore(value.totalScore),
    breakdown,
    explanations,
    strengths: normalizeStringList(value.strengths),
    risks: normalizeStringList(value.risks),
    recommendedRoles: normalizeStringList(value.recommendedRoles),
    recommendedTopics: normalizeStringList(value.recommendedTopics),
    techStack: normalizeStringList(value.techStack),
    defaults: normalizeDefaults(value.defaults),
  }
}

export function normalizeAnalysis(input: unknown): ResumeAnalysisResult {
  const value = isRecord(input) ? input : {}
  return {
    profile: normalizeResume(value.profile),
    score: normalizeScore(value.score),
  }
}

/** sessionStorage may be unavailable, disabled, or full. Treat it as an
 * optional cache: malformed values are ignored and writes never fail a
 * successful API request. */
export function readSessionValue<T>(key: string, normalizer: (value: unknown) => T): T | null {
  try {
    const raw = sessionStorage.getItem(key)
    if (!raw) return null
    const parsed: unknown = JSON.parse(raw)
    return isRecord(parsed) ? normalizer(parsed) : null
  } catch {
    return null
  }
}

export function writeSessionValue(key: string, value: unknown): boolean {
  try {
    sessionStorage.setItem(key, JSON.stringify(value))
    return true
  } catch {
    return false
  }
}
