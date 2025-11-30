import { createRouter, createWebHistory } from 'vue-router'

// 定义路由规则
const routes = [
    {
        path: '/login',
        name: 'Login',
        component: () => import('../views/Login.vue') // 懒加载登录页
    },
    {
        path: '/',
        name: 'Home',
        component: () => import('../views/Home.vue'), // 登录后的主页
        // 路由守卫：如果没有 Token，强行跳转登录页
        beforeEnter: (to, from, next) => {
            if (!localStorage.getItem('jwt_token')) {
                next('/login')
            } else {
                next()
            }
        }
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

export default router