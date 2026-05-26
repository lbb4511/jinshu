import request from './request'
import type { Result, LoginRequest, LoginResponse, User } from '../types'

const TOKEN_KEY = 'jinshu_token'
const REFRESH_TOKEN_KEY = 'jinshu_refresh_token'

export function getStoredToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setStoredToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token)
}

export function getStoredRefreshToken(): string | null {
  return localStorage.getItem(REFRESH_TOKEN_KEY)
}

export function setStoredRefreshToken(token: string): void {
  localStorage.setItem(REFRESH_TOKEN_KEY, token)
}

export function clearStoredTokens(): void {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
  localStorage.removeItem('jinshu_user')
}

export function getStoredUser(): User | null {
  const raw = localStorage.getItem('jinshu_user')
  if (!raw) return null
  try {
    return JSON.parse(raw) as User
  } catch {
    return null
  }
}

export function setStoredUser(user: User): void {
  localStorage.setItem('jinshu_user', JSON.stringify(user))
}

export async function login(req: LoginRequest): Promise<LoginResponse> {
  const res = await request.post<Result<LoginResponse>>('/auth/login', req)
  return res.data.data
}

export async function logout(): Promise<void> {
  try {
    await request.post('/auth/logout')
  } catch {
    // ignore
  }
  clearStoredTokens()
}

export async function getCurrentUser(): Promise<User> {
  const res = await request.get<Result<User>>('/auth/me')
  return res.data.data
}

export async function refreshToken(): Promise<LoginResponse> {
  const refresh = getStoredRefreshToken()
  const res = await request.post<Result<LoginResponse>>('/auth/refresh', {
    refreshToken: refresh,
  })
  return res.data.data
}
