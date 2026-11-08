import axios from 'axios'

// 创建 axios 实例
const request = axios.create({
  // 统一走同源 /api：开发环境由 Vite 代理到 8080，生产环境由 Nginx 反代到 8080。
  // 不要写死 http://localhost:8080，否则部署到服务器后所有接口都会跨域失败。
  baseURL: '/api',
  // 面试问答要等大模型生成，10 秒会超时；放宽到 2 分钟（Nginx 侧 read_timeout 已配 300s）
  timeout: 120000,
})

// 请求拦截器（可以在这里统一加 token 等）
request.interceptors.request.use(
  (config) => {
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器（统一处理返回数据）
request.interceptors.response.use(
  (response) => {
    // 假设后端返回格式是 { code, message, data, requestId, timestamp }
    const res = response.data
    if (res.code === 'OK' || res.code === 200) {
      return res.data // 直接返回 data 内容
    } else {
      // 业务错误，静默处理，调用方自己捕获
      console.error('业务错误：', res.message)
      return Promise.reject(res)
    }
  },
  (error) => {
    // 提取后端返回的具体错误信息
    let errorMsg = error.message
    if (error.response?.data) {
      errorMsg = error.response.data.message || error.response.data.error || error.response.data.msg || JSON.stringify(error.response.data)
    }
    console.error('请求失败：', errorMsg)
    // 把错误信息挂到error上，方便调用方显示
    error.displayMessage = errorMsg
    return Promise.reject(error)
  }
)

export default request
