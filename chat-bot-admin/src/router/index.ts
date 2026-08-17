import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'
import MainLayout from '@/layouts/MainLayout.vue'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    component: MainLayout,
    redirect: '/dashboard',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '仪表盘', icon: 'DashboardOutlined' }
      },
      {
        path: 'kb',
        name: 'KnowledgeBaseList',
        component: () => import('@/views/kb/KnowledgeBaseList.vue'),
        meta: { title: '知识库管理', icon: 'BookOutlined' }
      },
      {
        path: 'kb/create',
        name: 'KnowledgeBaseCreate',
        component: () => import('@/views/kb/KnowledgeBaseForm.vue'),
        meta: { title: '创建知识库', hidden: true }
      },
      {
        path: 'kb/edit/:id',
        name: 'KnowledgeBaseEdit',
        component: () => import('@/views/kb/KnowledgeBaseForm.vue'),
        meta: { title: '编辑知识库', hidden: true }
      },
      {
        path: 'document',
        name: 'DocumentList',
        component: () => import('@/views/document/DocumentList.vue'),
        meta: { title: '文档管理', icon: 'FileTextOutlined' }
      },
      {
        path: 'document/upload',
        name: 'DocumentUpload',
        component: () => import('@/views/document/DocumentUpload.vue'),
        meta: { title: '上传文档', hidden: true }
      },
      {
        path: 'quality',
        name: 'QualityCheck',
        component: () => import('@/views/quality/QualityCheck.vue'),
        meta: { title: '质量检测', icon: 'CheckCircleOutlined' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/dashboard'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  const isLoggedIn = userStore.isLoggedIn()

  if (to.meta.requiresAuth !== false && !isLoggedIn) {
    next({
      path: '/login',
      query: { redirect: to.fullPath }
    })
  } else if (to.path === '/login' && isLoggedIn) {
    next({ path: '/dashboard' })
  } else {
    next()
  }
})

export default router