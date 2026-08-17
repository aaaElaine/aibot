import { request } from '@/utils/request'

export interface QualityCheckResult {
  id: number
  kbId: number
  kbName: string
  checkTime: string
  status: string
  score?: number
  totalDocuments: number
  passedDocuments: number
  failedDocuments: number
  issues?: QualityIssue[]
  suggestions?: string
}

export interface QualityIssue {
  documentId: number
  documentName: string
  type: string
  description: string
  severity: 'high' | 'medium' | 'low'
}

export interface KbQualityCheck {
  id: number
  kbId: number
  checkType: string
  checkResult: Record<string, any>
  score: number
  suggestions: string
  checkTime: string
}

export interface QualityCheckRequest {
  kbId: number
  checkType?: string
}

function transformCheckResult(raw: KbQualityCheck): QualityCheckResult {
  const result: Record<string, any> = raw.checkResult || {}
  const total = (result.pendingDocuments || 0) + (result.processingDocuments || 0) +
    (result.completedDocuments || 0) + (result.failedDocuments || 0)

  return {
    id: raw.id,
    kbId: raw.kbId,
    kbName: '',
    checkTime: raw.checkTime,
    status: 'completed',
    score: raw.score,
    totalDocuments: result.totalDocuments || total,
    passedDocuments: result.completedDocuments || result.vectorCompleted || 0,
    failedDocuments: result.failedDocuments || 0,
    suggestions: raw.suggestions,
    issues: []
  }
}

export const qualityApi = {
  async check(kbId: number, checkType: string = 'COMPLETENESS'): Promise<QualityCheckResult> {
    const raw = await request.post<KbQualityCheck>(`/api/quality/check/${kbId}`, null, { params: { checkType } })
    return transformCheckResult(raw)
  },

  async checkAll(kbId: number): Promise<QualityCheckResult[]> {
    const list = await request.post<KbQualityCheck[]>(`/api/quality/check-all/${kbId}`)
    return (list || []).map(transformCheckResult)
  },

  getResult(id: number) {
    return request.get<QualityCheckResult>(`/api/quality/result/${id}`)
  },

  getResults(kbId: number) {
    return request.get<QualityCheckResult[]>(`/api/quality/results/${kbId}`)
  }
}
