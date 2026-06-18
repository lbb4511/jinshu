import request from './request'
import type { Result, PageResult, User } from '../types'

export interface CreateUserRequest {
  username: string
  password: string
  displayName?: string
  email?: string
  role: string
}

export interface UpdateUserRequest {
  displayName?: string
  email?: string
  role?: string
}

export interface ChangeStatusRequest {
  status: string
}

export async function listUsers(params: {
  username?: string
  role?: string
  page?: number
  pageSize?: number
}): Promise<PageResult<User>> {
  const res = await request.get<Result<PageResult<User>>>('/users', { params })
  return res.data.data
}

export async function getUser(id: number): Promise<User> {
  const res = await request.get<Result<User>>(`/users/${id}`)
  return res.data.data
}

export async function createUser(data: CreateUserRequest): Promise<User> {
  const res = await request.post<Result<User>>('/users', data)
  return res.data.data
}

export async function updateUser(id: number, data: UpdateUserRequest): Promise<User> {
  const res = await request.put<Result<User>>(`/users/${id}`, data)
  return res.data.data
}

export async function changeUserStatus(id: number, status: string): Promise<User> {
  const res = await request.patch<Result<User>>(`/users/${id}/status`, null, { params: { status } })
  return res.data.data
}

export async function resetPassword(id: number, newPassword: string): Promise<void> {
  await request.post<Result<void>>(`/users/${id}/reset-password`, null, { params: { newPassword } })
}
