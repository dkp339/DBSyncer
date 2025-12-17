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
        redirect: '/welcome', // 访问 / 时自动跳到欢迎页
        // 路由守卫：如果没有 Token，强行跳转登录页
        beforeEnter: (to, from, next) => {
            if (!localStorage.getItem('jwt_token')) {
                next('/login')
            } else {
                next()
            }
        },
        children: [
            {
                // 欢迎页（Dashboard）
                path: 'welcome',
                name: 'Welcome',
                // 你可以新建一个简单的 Welcome.vue，或者暂时先用 DataSource 占位
                component: () => import('../views/Welcome.vue')
            },
            {
                // 数据源管理页面
                path: 'datasource',
                name: 'DataSource',
                // 假设你的文件路径是 src/views/datasource/index.vue
                component: () => import('../views/datasource/index.vue')
            }
        ]
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

export default router