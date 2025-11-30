import axios from 'axios'
import { ElMessage } from 'element-plus'

// 创建 axios 实例
const service = axios.create({
    baseURL: '/api', // 所有的请求都会自动加上 /api 前缀，触发 vite 的代理
    timeout: 5000    // 请求超时时间
})

// 请求拦截器：在发送请求之前做些什么
service.interceptors.request.use(
    config => {
        // 从 localStorage 获取 Token
        const token = localStorage.getItem('jwt_token')
        if (token) {
            // 如果有 Token，放入请求头 Authorization 中
            // 注意：Bearer 后有一个空格，这是标准格式
            config.headers['Authorization'] = 'Bearer ' + token
        }
        return config
    },
    error => {
        return Promise.reject(error)
    }
)

// 响应拦截器：在接收响应之后做些什么
// 响应拦截器：在接收响应之后做些什么
service.interceptors.response.use(
    response => {
        return response.data
    },
    error => {
        let msg = '系统异常'
        if (error.response) {
            const status = error.response.status
            const data = error.response.data

            if (status === 401) {
                if (data && data.error) {
                    msg = data.error // 显示 "密码错误"
                } else {
                    msg = '未授权，请重新登录' // 只有后端没说话时，才显示这个
                }
            } else if (data && data.error) {
                msg = data.error
            }
        }
        ElMessage.error(msg)
        return Promise.reject(error)
    }
)

export default service