<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-header">
        <h1>房产管理系统</h1>
      </div>

      <!-- 登录 / 注册 切换 -->
      <div class="mode-tabs">
        <span
          :class="['mode-tab', { active: mode === 'login' }]"
          @click="switchMode('login')"
        >登 录</span>
        <span
          :class="['mode-tab', { active: mode === 'register' }]"
          @click="switchMode('register')"
        >注 册</span>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        @keyup.enter="handleSubmit"
      >
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            prefix-icon="User"
            size="large"
          />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            prefix-icon="Lock"
            show-password
            size="large"
          />
        </el-form-item>

        <!-- 注册模式：确认密码 -->
        <el-form-item v-if="mode === 'register'" label="确认密码" prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="请再次输入密码"
            prefix-icon="Lock"
            show-password
            size="large"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            @click="handleSubmit"
            class="submit-btn"
          >
            {{ loading ? '处理中...' : (mode === 'login' ? '登 录' : '注 册') }}
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUser } from '../../composables/useUser'

const router = useRouter()
const { login, register } = useUser()

const mode = ref('login')   // 'login' | 'register'
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  confirmPassword: ''
})

// 基础校验规则
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== form.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

function switchMode(m) {
  mode.value = m
  form.username = ''
  form.password = ''
  form.confirmPassword = ''
  formRef.value?.clearValidate()
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true

  // 模拟短暂延迟
  await new Promise(r => setTimeout(r, 300))

  if (mode.value === 'login') {
    const result = login(form.username, form.password)
    loading.value = false

    if (result.success) {
      const roleLabel = result.role === 'admin' ? '管理员' : '普通住户'
      ElMessage.success(`欢迎回来，${form.username}（${roleLabel}）`)
      router.push('/dashboard')
    } else {
      ElMessage.error(result.message)
    }
  } else {
    // 注册模式
    const result = register(form.username, form.password)
    loading.value = false

    if (result.success) {
      ElMessage.success('注册成功，已自动登录')
      router.push('/dashboard')
    } else {
      ElMessage.error(result.message)
    }
  }
}
</script>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0f2f5;
}

.login-card {
  width: 420px;
  background: #fff;
  border-radius: 12px;
  padding: 40px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.login-header {
  text-align: center;
  margin-bottom: 28px;
}

.login-header h1 {
  margin: 0;
  font-size: 24px;
  color: #303133;
  font-weight: 600;
}

/* 登录/注册切换标签 */
.mode-tabs {
  display: flex;
  margin-bottom: 24px;
  border-bottom: 2px solid #ebeef5;
}

.mode-tab {
  flex: 1;
  text-align: center;
  padding: 10px 0;
  font-size: 15px;
  color: #909399;
  cursor: pointer;
  transition: all 0.3s;
  position: relative;
}

.mode-tab.active {
  color: #409eff;
  font-weight: 600;
}

.mode-tab.active::after {
  content: '';
  position: absolute;
  bottom: -2px;
  left: 50%;
  transform: translateX(-50%);
  width: 40px;
  height: 2px;
  background: #409eff;
}

.submit-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
  letter-spacing: 4px;
}
</style>
