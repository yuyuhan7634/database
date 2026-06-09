<template>
  <el-container class="layout-container">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapse ? '64px' : '220px'" class="layout-aside">
      <div class="logo-area">
        <span v-if="!isCollapse" class="logo-text">房产管理系统</span>
        <span v-else class="logo-text logo-small">房</span>
      </div>
      <el-menu
        :default-active="route.path"
        :collapse="isCollapse"
        :collapse-transition="false"
        background-color="#001529"
        text-color="#ffffffbf"
        active-text-color="#fff"
        router
      >
        <!-- ===== 系统概览（所有人） ===== -->
        <el-menu-item index="/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <span>系统概览</span>
        </el-menu-item>

        <!-- ===== 申请管理 ===== -->
        <el-sub-menu index="/application">
          <template #title>
            <el-icon><Document /></el-icon>
            <span>申请管理</span>
          </template>
          <!-- 提交申请：仅住户可见 -->
          <el-menu-item v-if="!isAdmin" index="/application/submit">
            <el-icon><Edit /></el-icon>
            <span>提交申请</span>
          </el-menu-item>
          <el-menu-item index="/application/list">
            <el-icon><List /></el-icon>
            <span>申请列表</span>
          </el-menu-item>
          <!-- 分房队列：仅管理员可见 -->
          <el-menu-item v-if="isAdmin" index="/application/queue">
            <el-icon><Sort /></el-icon>
            <span>分房队列</span>
          </el-menu-item>
        </el-sub-menu>

        <!-- ===== 房产信息 ===== -->
        <el-sub-menu index="/house">
          <template #title>
            <el-icon><HomeFilled /></el-icon>
            <span>房产信息</span>
          </template>
          <el-menu-item index="/house/list">
            <el-icon><Reading /></el-icon>
            <span>房产列表</span>
          </el-menu-item>
          <el-menu-item index="/house/available">
            <el-icon><CircleCheck /></el-icon>
            <span>空闲房产</span>
          </el-menu-item>
          <el-menu-item index="/house/standard">
            <el-icon><Tickets /></el-icon>
            <span>住房标准</span>
          </el-menu-item>
        </el-sub-menu>

        <!-- ===== 住户管理（仅管理员） ===== -->
        <el-sub-menu v-if="isAdmin" index="/resident">
          <template #title>
            <el-icon><UserFilled /></el-icon>
            <span>住户管理</span>
          </template>
          <el-menu-item index="/resident/list">
            <el-icon><User /></el-icon>
            <span>住户列表</span>
          </el-menu-item>
          <el-menu-item index="/resident/bill">
            <el-icon><Money /></el-icon>
            <span>房租账单</span>
          </el-menu-item>
        </el-sub-menu>

        <!-- ===== 管理功能（仅管理员） ===== -->
        <el-sub-menu v-if="isAdmin" index="/admin">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>管理功能</span>
          </template>
          <el-menu-item index="/admin/statistics">
            <el-icon><DataBoard /></el-icon>
            <span>统计报表</span>
          </el-menu-item>
          <el-menu-item index="/admin/threshold">
            <el-icon><TrendCharts /></el-icon>
            <span>阈值管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/rent">
            <el-icon><Coin /></el-icon>
            <span>租金管理</span>
          </el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <!-- 主内容 -->
    <el-container>
      <el-header class="layout-header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="isCollapse = !isCollapse">
            <Fold v-if="!isCollapse" />
            <Expand v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="route.meta.title">{{ route.meta.title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <span class="header-time">{{ currentTime }}</span>
          <el-divider direction="vertical" />
          <el-tag :type="isAdmin ? 'danger' : 'success'" size="small" effect="dark">
            {{ isAdmin ? '管理员' : '住户' }}
          </el-tag>
          <span class="header-user">{{ username }}</span>
          <el-button type="danger" size="small" plain @click="handleLogout" style="margin-left:12px">
            <el-icon><SwitchButton /></el-icon>
            退出登录
          </el-button>
        </div>
      </el-header>
      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useUser } from '../composables/useUser'

const route = useRoute()
const router = useRouter()
const { isAdmin, username, logout } = useUser()

const isCollapse = ref(false)
const currentTime = ref('')

let timer = null
onMounted(() => {
  const updateTime = () => {
    const now = new Date()
    currentTime.value = now.toLocaleString('zh-CN', { hour12: false })
  }
  updateTime()
  timer = setInterval(updateTime, 1000)
})
onUnmounted(() => clearInterval(timer))

async function handleLogout() {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return // 取消
  }
  logout()
  router.push('/login')
}
</script>

<style scoped>
.layout-container { height: 100vh; }
.layout-aside { background-color: #001529; overflow-y: auto; transition: width 0.3s; }
.layout-aside::-webkit-scrollbar { width: 0; }
.logo-area {
  height: 60px; display: flex; align-items: center; justify-content: center;
  border-bottom: 1px solid #ffffff1a;
}
.logo-text { color: #fff; font-size: 18px; font-weight: bold; white-space: nowrap; }
.logo-small { font-size: 24px; }
.layout-header {
  display: flex; align-items: center; justify-content: space-between;
  background: #fff; border-bottom: 1px solid #e4e7ed; padding: 0 20px; height: 60px;
}
.header-left { display: flex; align-items: center; gap: 16px; }
.collapse-btn { font-size: 20px; cursor: pointer; }
.header-right {
  display: flex; align-items: center; gap: 8px;
  font-size: 14px; color: #606266;
}
.header-time { font-family: monospace; }
.header-user { font-weight: 500; color: #303133; }
.layout-main {
  background: #f0f2f5; padding: 20px; overflow-y: auto;
}
.el-menu { border-right: none; }
</style>
