import { ref, computed } from 'vue'

// ==================== 存储键名 ====================
const USERS_KEY = 'estate_users'       // localStorage：所有注册用户
const SESSION_KEY = 'estate_session'   // sessionStorage：当前登录会话

// ==================== 初始化预置管理员 ====================
function initUsers() {
  try {
    const raw = localStorage.getItem(USERS_KEY)
    if (raw) return JSON.parse(raw)
  } catch {}
  // 首次使用：预置管理员账户
  const defaultUsers = [
    { username: 'admin', password: '123456', role: 'admin' }
  ]
  localStorage.setItem(USERS_KEY, JSON.stringify(defaultUsers))
  return defaultUsers
}

/** 读取全部注册用户 */
function loadUsers() {
  try {
    const raw = localStorage.getItem(USERS_KEY)
    return raw ? JSON.parse(raw) : initUsers()
  } catch {
    return initUsers()
  }
}

/** 保存用户列表 */
function saveUsers(users) {
  localStorage.setItem(USERS_KEY, JSON.stringify(users))
}

/** 从 sessionStorage 恢复当前会话 */
function loadSession() {
  try {
    const raw = sessionStorage.getItem(SESSION_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

// ==================== 全局响应式状态 ====================
const user = ref(loadSession())

export function useUser() {
  const isLoggedIn = computed(() => !!user.value)
  const isAdmin = computed(() => user.value?.role === 'admin')
  const username = computed(() => user.value?.username || '')

  // ==================== 注册 ====================
  /**
   * 注册新用户
   * @returns {{ success: boolean, message: string }}
   */
  function register(usernameInput, passwordInput) {
    if (!usernameInput || !usernameInput.trim()) {
      return { success: false, message: '用户名不能为空' }
    }
    if (!passwordInput || !passwordInput.trim()) {
      return { success: false, message: '密码不能为空' }
    }
    if (passwordInput.length < 3) {
      return { success: false, message: '密码长度至少 3 位' }
    }

    const users = loadUsers()
    const exists = users.find(u => u.username === usernameInput)
    if (exists) {
      return { success: false, message: '该用户名已被注册，请换一个' }
    }

    users.push({ username: usernameInput, password: passwordInput, role: 'resident' })
    saveUsers(users)

    // 注册成功后自动登录
    const session = { username: usernameInput, role: 'resident' }
    user.value = session
    sessionStorage.setItem(SESSION_KEY, JSON.stringify(session))

    return { success: true, message: '注册成功' }
  }

  // ==================== 登录 ====================
  /**
   * 登录：校验用户名和密码
   * @returns {{ success: boolean, message: string, role?: string }}
   */
  function login(usernameInput, passwordInput) {
    if (!usernameInput || !usernameInput.trim()) {
      return { success: false, message: '请输入用户名' }
    }
    if (!passwordInput || !passwordInput.trim()) {
      return { success: false, message: '请输入密码' }
    }

    const users = loadUsers()
    const found = users.find(u => u.username === usernameInput)

    if (!found) {
      return { success: false, message: '该用户未注册，请先注册' }
    }
    if (found.password !== passwordInput) {
      return { success: false, message: '密码错误，请重新输入' }
    }

    // 登录成功：写入 sessionStorage（关闭浏览器后自动清除）
    const session = { username: found.username, role: found.role }
    user.value = session
    sessionStorage.setItem(SESSION_KEY, JSON.stringify(session))

    return { success: true, message: '登录成功', role: found.role }
  }

  // ==================== 退出登录 ====================
  function logout() {
    user.value = null
    sessionStorage.removeItem(SESSION_KEY)
  }

  return { user, isLoggedIn, isAdmin, username, login, register, logout }
}
