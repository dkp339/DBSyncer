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
                component: () => import('../views/Welcome.vue')
            },
            {
                // 数据源管理页面
                path: 'datasource',
                name: 'DataSource',
                component: () => import('../views/data/DataSource.vue')
            },
            {
                path: 'query',
                name: 'DataQuery',
                component: () => import('../views/data/DataQuery.vue'),
                meta: { title: 'SQL查询台' }
            },
            {
                path: 'sys/config',
                name: 'SysConfig',
                component: () => import('../views/sys/SysConfig.vue'),
                meta: { title: '系统配置' }
            }
        ]
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

export default router