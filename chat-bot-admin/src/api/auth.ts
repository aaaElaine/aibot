import { request } from '@/utils/request'

export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  token: string
}

export interface UserVO {
  id: number
  username: string
  email?: string
  phone?: string
  role?: string
}

export const authApi = {
  login(data: LoginRequest) {
    return request.post<LoginResponse>('/api/auth/login', data)
  },

  getCurrentUser() {
    return request.get<UserVO>('/api/auth/current')
  }
}