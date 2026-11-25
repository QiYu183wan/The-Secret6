import axios from 'axios'
import { ElMessage } from 'element-plus'

const api = axios.create({
  baseURL: '',
  withCredentials: true,
  timeout: 20000
})

api.interceptors.response.use(
  (response) => response.data,
  (error) => {
    const status = error.response?.status
    const message = error.response?.data?.message || error.message || '网络请求失败'
    if (status === 401) {
      ElMessage.error(message)
      if (!['/login', '/register'].includes(window.location.pathname)) {
        window.location.href = '/login'
      }
    } else {
      ElMessage.error(message)
    }
    return Promise.reject(error)
  }
)

export default api
