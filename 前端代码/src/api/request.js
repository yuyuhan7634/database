import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' }
})

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    const res = response.data
    // 后端统一返回 { code: 200, message: "...", data: ... }
    if (res.code === 200) {
      return res
    }
    ElMessage.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message || '请求失败'))
  },
  (error) => {
    let msg = '网络错误'
    if (error.response) {
      switch (error.response.status) {
        case 404: msg = '请求的接口不存在'; break
        case 500: msg = '服务器内部错误'; break
        case 502: msg = '网关错误'; break
        default: msg = `请求错误 (${error.response.status})`
      }
    } else if (error.message.includes('timeout')) {
      msg = '请求超时，请检查网络'
    }
    ElMessage.error(msg)
    return Promise.reject(error)
  }
)

export default request
