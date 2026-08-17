import { request, PageResult } from '@/utils/request'

export interface DocumentVO {
  id: number
  kbId: number
  categoryId?: number
  categoryName?: string
  title: string
  content?: string
  fileType?: string
  fileSize?: number
  filePath?: string
  vectorStatus?: string
  qualityScore?: number
  version?: number
  createTime?: string
  updateTime?: string
}

export interface DocumentPageRequest {
  pageNum?: number
  pageSize?: number
  kbId?: number
  name?: string
  status?: string
  categoryId?: number
}

export interface DocumentCategoryVO {
  id: number
  name: string
  parentId?: number
  description?: string
}

export const documentApi = {
  getPage(params: DocumentPageRequest) {
    return request.get<PageResult<DocumentVO>>('/api/document/page', { params })
  },

  upload(file: File, kbId: number, categoryId?: number) {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('kbId', String(kbId))
    if (categoryId) {
      formData.append('categoryId', String(categoryId))
    }
    return request.post<DocumentVO>('/api/document/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      },
      timeout: 120000
    })
  },

  delete(id: number) {
    return request.delete<void>(`/api/document/${id}`)
  },

  downloadUrl(id: number) {
    return `/api/document/download/${id}`
  },

  getCategories(kbId: number) {
    return request.get<DocumentCategoryVO[]>(`/api/document/categories/${kbId}`)
  }
}
