import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse, AxiosError } from 'axios'
import { message } from 'ant-design-vue'
import { useUserStore } from '@/stores/user'

const instance: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  },
  maxContentLength: Infinity,
  maxBodyLength: Infinity
})

instance.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }
    return config
  },
  (error: AxiosError) => {
    return Promise.reject(error)
  }
)

instance.interceptors.response.use(
  (response: AxiosResponse) => {
    const res = response.data
    if (res.code && res.code !== 200) {
      message.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  (error: AxiosError) => {
    let errorMessage = error.message || '请求失败'
    if (error.response) {
      const data = error.response.data as any
      // 优先显示后端返回的错误信息
      if (data?.message) {
        errorMessage = data.message
      } else {
        switch (error.response.status) {
          case 401:
            errorMessage = '未授权，请重新登录'
            const userStore = useUserStore()
            userStore.logout()
            window.location.href = '/login'
            break
          case 403:
            errorMessage = '拒绝访问'
            break
          case 404:
            errorMessage = '请求资源不存在'
            break
          case 413:
            errorMessage = '文件太大，超出限制'
            break
          case 500:
            errorMessage = '服务器内部错误'
            break
          default:
            errorMessage = `请求失败 (${error.response.status})`
        }
      }
    } else if (error.code === 'ECONNABORTED') {
      errorMessage = '请求超时，请重试'
    } else {
      errorMessage = '网络异常，请检查网络连接'
    }
    message.error(errorMessage)
    // 将错误信息附加到 error 对象，方便调用方获取
    ;(error as any).userMessage = errorMessage
    return Promise.reject(error)
  }
)

export interface Result<T = any> {
  code: number
  message: string
  data: T
}

export interface PageResult<T = any> {
  records: T[]
  total: number
  current: number
  size: number
  pages: number
}

export const request = {
  get<T = any>(url: string, config?: AxiosRequestConfig): Promise<Result<T>> {
    return instance.get<T>(url, config).then(res => res.data)
  },
  post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<Result<T>> {
    return instance.post<T>(url, data, config).then(res => res.data)
  },
  put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<Result<T>> {
    return instance.put<T>(url, data, config).then(res => res.data)
  },
  delete<T = any>(url: string, config?: AxiosRequestConfig): Promise<Result<T>> {
    return instance.delete<T>(url, config).then(res => res.data)
  },
  upload<T = any>(url: string, file: File, config?: AxiosRequestConfig): Promise<Result<T>> {
    const formData = new FormData()
    formData.append('file', file)
    return instance.post<T>(url, formData, {
      ...config,
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    }).then(res => res.data)
  }
}

export default instance