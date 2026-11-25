import { reactive } from 'vue'
import api from '../api'

export const auth = reactive({
  user: null,
  loaded: false
})

export async function ensureUser() {
  if (auth.loaded) {
    return auth.user
  }
  try {
    const res = await api.get('/api/user/profile')
    auth.user = res.data
  } catch (e) {
    auth.user = null
  }
  auth.loaded = true
  return auth.user
}

export function setUser(user) {
  auth.user = user
  auth.loaded = true
}

export async function logout() {
  try {
    await api.post('/api/logout')
  } catch (e) {
    // 忽略退出异常，前端仍然清理登录态
  }
  auth.user = null
  auth.loaded = true
}
