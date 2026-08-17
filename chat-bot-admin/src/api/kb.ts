import { request, PageResult } from '@/utils/request'

export interface KnowledgeBaseVO {
  id: number
  name: string
  description?: string
  type?: string
  status?: string
  documentCount?: number
  createTime?: string
  updateTime?: string
}

export interface KnowledgeBaseCreateRequest {
  name: string
  description?: string
  type?: string
}

export interface KnowledgeBaseUpdateRequest {
  id: number
  name: string
  description?: string
  type?: string
  status?: string
}

export interface KnowledgeBasePageRequest {
  pageNum?: number
  pageSize?: number
  name?: string
  status?: string
}

export const kbApi = {
  getPage(params: KnowledgeBasePageRequest) {
    return request.get<PageResult<KnowledgeBaseVO>>('/api/kb/page', { params })
  },

  getById(id: number) {
    return request.get<KnowledgeBaseVO>(`/api/kb/${id}`)
  },

  create(data: KnowledgeBaseCreateRequest) {
    return request.post<KnowledgeBaseVO>('/api/kb/create', data)
  },

  update(data: KnowledgeBaseUpdateRequest) {
    return request.put<KnowledgeBaseVO>('/api/kb/update', data)
  },

  delete(id: number) {
    return request.delete<void>(`/api/kb/${id}`)
  }
}