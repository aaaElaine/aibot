import { post, get } from '@/utils/request'

export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  token: string
  user: UserInfo
}

export interface UserInfo {
  id: number
  username: string
  nickname?: string
  avatar?: string
  email?: string
  phone?: string
}

// 登录
export function login(data: LoginRequest) {
  return post<LoginResponse>('/api/auth/login', data)
}

// 获取当前用户信息
export function getCurrentUser() {
  return get<UserInfo>('/api/auth/current')
}

// 保存认证信息到本地
export function saveAuth(token: string, user: UserInfo) {
  localStorage.setItem('token', token)
  localStorage.setItem('userInfo', JSON.stringify(user))
}

// 清除认证信息
export function clearAuth() {
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
}

// 获取本地用户信息
export function getLocalUser(): UserInfo | null {
  const userStr = localStorage.getItem('userInfo')
  if (userStr) {
    try {
      return JSON.parse(userStr)
    } catch {
      return null
    }
  }
  return null
}

// 检查是否已登录
export function isAuthenticated(): boolean {
  return !!localStorage.getItem('token')
}