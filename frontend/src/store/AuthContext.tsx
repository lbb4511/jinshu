import { createContext, useState, useEffect, useCallback, type ReactNode } from 'react'
import type { User, LoginRequest } from '../types'
import * as authService from '../services/auth'

export interface AuthContextValue {
  user: User | null
  token: string | null
  loading: boolean
  isAuthenticated: boolean
  login: (req: LoginRequest) => Promise<void>
  logout: () => Promise<void>
}

export const AuthContext = createContext<AuthContextValue | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(authService.getStoredUser)
  const [token, setToken] = useState<string | null>(authService.getStoredToken)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const t = authService.getStoredToken()
    if (t) {
      setToken(t)
      authService.getCurrentUser()
        .then(u => {
          setUser(u)
          authService.setStoredUser(u)
        })
        .catch(() => {
          setUser(null)
          setToken(null)
          authService.clearStoredTokens()
        })
        .finally(() => setLoading(false))
    } else {
      setLoading(false)
    }
  }, [])

  const login = useCallback(async (req: LoginRequest) => {
    const res = await authService.login(req)
    authService.setStoredToken(res.token)
    authService.setStoredRefreshToken(res.refreshToken)
    setToken(res.token)

    const currentUser = await authService.getCurrentUser()
    authService.setStoredUser(currentUser)
    setUser(currentUser)
  }, [])

  const logout = useCallback(async () => {
    await authService.logout()
    setUser(null)
    setToken(null)
  }, [])

  const value: AuthContextValue = {
    user,
    token,
    loading,
    isAuthenticated: !!token && !!user,
    login,
    logout,
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
