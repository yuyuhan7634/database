import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/login/Login.vue'),
    meta: { title: '登录', public: true }
  },
  {
    path: '/',
    component: () => import('../views/Layout.vue'),
    redirect: '/dashboard',
    children: [
      // ==================== 系统概览 ====================
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../views/dashboard/Dashboard.vue'),
        meta: { title: '系统概览' }
      },
      // ==================== 申请管理 ====================
      {
        path: 'application/submit',
        name: 'ApplicationSubmit',
        component: () => import('../views/application/ApplicationSubmit.vue'),
        meta: { title: '提交申请' }
      },
      {
        path: 'application/list',
        name: 'ApplicationList',
        component: () => import('../views/application/ApplicationList.vue'),
        meta: { title: '申请列表' }
      },
      {
        path: 'application/queue',
        name: 'AllocationQueue',
        component: () => import('../views/application/AllocationQueue.vue'),
        meta: { title: '分房队列', requireAdmin: true }
      },
      // ==================== 房产信息 ====================
      {
        path: 'house/list',
        name: 'HouseList',
        component: () => import('../views/house/HouseList.vue'),
        meta: { title: '房产列表' }
      },
      {
        path: 'house/available',
        name: 'AvailableHouses',
        component: () => import('../views/house/AvailableHouses.vue'),
        meta: { title: '空闲房产' }
      },
      {
        path: 'house/standard',
        name: 'StandardList',
        component: () => import('../views/house/StandardList.vue'),
        meta: { title: '住房标准' }
      },
      // ==================== 住户管理（仅管理员） ====================
      {
        path: 'resident/list',
        name: 'ResidentList',
        component: () => import('../views/resident/ResidentList.vue'),
        meta: { title: '住户列表', requireAdmin: true }
      },
      {
        path: 'resident/bill',
        name: 'BillList',
        component: () => import('../views/resident/BillList.vue'),
        meta: { title: '房租账单', requireAdmin: true }
      },
      // ==================== 管理功能（仅管理员） ====================
      {
        path: 'admin/statistics',
        name: 'Statistics',
        component: () => import('../views/admin/Statistics.vue'),
        meta: { title: '统计报表', requireAdmin: true }
      },
      {
        path: 'admin/threshold',
        name: 'ThresholdManage',
        component: () => import('../views/admin/ThresholdManage.vue'),
        meta: { title: '阈值管理', requireAdmin: true }
      },
      {
        path: 'admin/rent',
        name: 'RentManage',
        component: () => import('../views/admin/RentManage.vue'),
        meta: { title: '租金管理', requireAdmin: true }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// ==================== 路由守卫：登录 & 角色权限校验 ====================
router.beforeEach((to, from, next) => {
  let user = null
  try {
    const raw = sessionStorage.getItem('estate_session')
    user = raw ? JSON.parse(raw) : null
  } catch {
    user = null
  }

  const isLoggedIn = !!user

  // 访问登录页：已登录 → 直接进概览；未登录 → 放行
  if (to.meta.public) {
    if (isLoggedIn) return next('/dashboard')
    return next()
  }

  // 访问业务页但未登录 → 跳到登录页
  if (!isLoggedIn) {
    return next('/login')
  }

  // 访问管理员专属页面但角色不是 admin → 跳回概览
  if (to.meta.requireAdmin && user.role !== 'admin') {
    return next('/dashboard')
  }

  next()
})

export default router
