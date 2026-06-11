<template>
  <div class="dashboard">
    <h2 class="page-title">{{ isAdmin ? '系统概览' : '个人信息' }}</h2>

    <!-- ==================== 管理员视图 ==================== -->
    <template v-if="isAdmin">
      <!-- 统计卡片 -->
      <el-row :gutter="20" class="stat-cards">
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-value">{{ stats.totalHouses || 0 }}</div>
            <div class="stat-label">房屋总数</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-value" style="color:#67c23a">{{ stats.emptyHouses || 0 }}</div>
            <div class="stat-label">空闲房屋</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-value" style="color:#409eff">{{ stats.occupiedHouses || 0 }}</div>
            <div class="stat-label">已分配房屋</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-value" style="color:#e6a23c">{{ stats.occupancyRate || '0%' }}</div>
            <div class="stat-label">入住率</div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 快捷操作 -->
      <el-row :gutter="20" class="quick-actions">
        <el-col :span="24">
          <el-card shadow="never">
            <template #header><span>快捷操作</span></template>
            <el-row :gutter="20">
              <el-col :span="4" v-for="item in adminQuickActions" :key="item.path">
                <el-card shadow="hover" class="quick-card" @click="$router.push(item.path)">
                  <el-icon :size="32" :color="item.color"><component :is="item.icon" /></el-icon>
                  <div class="quick-name">{{ item.name }}</div>
                </el-card>
              </el-col>
            </el-row>
          </el-card>
        </el-col>
      </el-row>

      <!-- 住房标准阈值 -->
      <el-card shadow="never" class="section-card">
        <template #header><span>住房标准阈值</span></template>
        <el-table :data="standards" stripe style="width:100%">
          <el-table-column prop="area" label="面积 (㎡)" width="200" />
          <el-table-column prop="minScore" label="最低分数" />
        </el-table>
      </el-card>
    </template>

    <!-- ==================== 住户视图 ==================== -->
    <template v-else>
      <!-- 个人概览 -->
      <el-row :gutter="20" class="stat-cards">
        <el-col :span="8">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-value" style="color:#409eff">{{ personalStats.applicationCount || 0 }}</div>
            <div class="stat-label">我的申请数</div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-value" style="color:#67c23a">{{ personalStats.approvedCount || 0 }}</div>
            <div class="stat-label">已通过</div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-value" style="color:#e6a23c">{{ personalStats.pendingCount || 0 }}</div>
            <div class="stat-label">待处理</div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 当前住房信息（可编辑） -->
      <el-card shadow="never" class="section-card" v-if="personalStats.houseNo">
        <template #header>
          <span>我的住房信息</span>
          <el-button
            v-if="!editing"
            type="primary" size="small" plain
            style="float:right"
            @click="startEdit"
          >
            <el-icon><Edit /></el-icon> 修改信息
          </el-button>
          <div v-else style="float:right;display:flex;gap:8px">
            <el-button type="primary" size="small" @click="saveEdit" :loading="saving">
              保存
            </el-button>
            <el-button size="small" @click="cancelEdit">取消</el-button>
          </div>
        </template>

        <!-- 只读模式 -->
        <el-descriptions v-if="!editing" :column="2" border>
          <el-descriptions-item label="房号">{{ personalStats.houseNo }}</el-descriptions-item>
          <el-descriptions-item label="面积">{{ personalStats.houseArea }} ㎡</el-descriptions-item>
          <el-descriptions-item label="住房分数">
            <el-tag type="warning">{{ personalStats.score }} 分</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="家庭人口">{{ personalStats.familySize }} 人</el-descriptions-item>
          <el-descriptions-item label="部门">{{ personalStats.department }}</el-descriptions-item>
          <el-descriptions-item label="职称">{{ personalStats.title }}</el-descriptions-item>
        </el-descriptions>

        <!-- 编辑模式 -->
        <el-form v-else :model="editForm" label-width="100px">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="房号">
                <el-input :value="personalStats.houseNo" disabled />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="面积">
                <el-input :value="personalStats.houseArea + ' ㎡'" disabled />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="部门">
                <el-input v-model="editForm.department" placeholder="请输入部门" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="职称">
                <el-select v-model="editForm.title" placeholder="请选择职称" style="width:100%">
                  <el-option label="教授" value="教授" />
                  <el-option label="副教授" value="副教授" />
                  <el-option label="讲师" value="讲师" />
                  <el-option label="助教" value="助教" />
                  <el-option label="处长" value="处长" />
                  <el-option label="科长" value="科长" />
                  <el-option label="研究员" value="研究员" />
                  <el-option label="工程师" value="工程师" />
                  <el-option label="实验员" value="实验员" />
                  <el-option label="干事" value="干事" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="家庭人口">
                <el-input-number v-model="editForm.familySize" :min="1" :max="10" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="当前分数">
                <el-tag type="warning">{{ personalStats.score }} 分</el-tag>
                <span style="margin-left:8px;font-size:12px;color:#909399">（保存后自动重算）</span>
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>
      </el-card>
      <el-empty v-else description="暂无住房信息，您可以提交分房申请" :image-size="80" />

      <!-- 快捷操作 -->
      <el-row :gutter="20" class="quick-actions" style="margin-top:20px">
        <el-col :span="24">
          <el-card shadow="never">
            <template #header><span>快捷操作</span></template>
            <el-row :gutter="20">
              <el-col :span="6" v-for="item in residentQuickActions" :key="item.path">
                <el-card shadow="hover" class="quick-card" @click="$router.push(item.path)">
                  <el-icon :size="32" :color="item.color"><component :is="item.icon" /></el-icon>
                  <div class="quick-name">{{ item.name }}</div>
                </el-card>
              </el-col>
            </el-row>
          </el-card>
        </el-col>
      </el-row>

      <!-- 住房标准 -->
      <el-card shadow="never" class="section-card">
        <template #header><span>住房标准参考</span></template>
        <el-table :data="standards" stripe style="width:100%">
          <el-table-column prop="area" label="面积 (㎡)" width="200" />
          <el-table-column prop="minScore" label="最低分数" />
        </el-table>
      </el-card>
    </template>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUser } from '../../composables/useUser'
import { getStatistics } from '../../api/admin'
import { getStandardList } from '../../api/house'
import { getApplicationList } from '../../api/application'
import { getResidentList, updateResidentInfo } from '../../api/resident'

const { isAdmin, username } = useUser()

const stats = ref({})
const standards = ref([])
const personalStats = reactive({
  applicationCount: 0,
  approvedCount: 0,
  pendingCount: 0,
  houseNo: null,
  houseArea: null,
  score: null,
  familySize: null,
  department: '',
  title: ''
})

// 编辑状态
const editing = ref(false)
const saving = ref(false)
const editForm = reactive({
  department: '',
  title: '',
  familySize: 1
})

const adminQuickActions = [
  { path: '/application/queue', name: '分房队列', icon: 'Sort', color: '#67c23a' },
  { path: '/house/available', name: '空闲房产', icon: 'HomeFilled', color: '#e6a23c' },
  { path: '/resident/list', name: '住户列表', icon: 'User', color: '#409eff' },
  { path: '/resident/bill', name: '房租账单', icon: 'Money', color: '#f56c6c' },
  { path: '/admin/statistics', name: '统计报表', icon: 'DataBoard', color: '#909399' },
]

const residentQuickActions = [
  { path: '/application/submit', name: '提交申请', icon: 'Edit', color: '#409eff' },
  { path: '/application/list', name: '我的申请', icon: 'List', color: '#67c23a' },
  { path: '/house/available', name: '空闲房产', icon: 'HomeFilled', color: '#e6a23c' },
  { path: '/house/standard', name: '住房标准', icon: 'Tickets', color: '#909399' },
]

// ========== 编辑住房信息 ==========
function startEdit() {
  editForm.department = personalStats.department || ''
  editForm.title = personalStats.title || ''
  editForm.familySize = personalStats.familySize || 1
  editing.value = true
}

function cancelEdit() {
  editing.value = false
}

async function saveEdit() {
  try {
    await ElMessageBox.confirm(
      '确认更新个人信息吗？修改职称或家庭人口后，住房分数将自动重新计算。',
      '确认更新',
      { type: 'warning', confirmButtonText: '确认' }
    )
  } catch {
    return
  }

  saving.value = true
  try {
    const res = await updateResidentInfo(
      username.value,
      editForm.department,
      editForm.title,
      editForm.familySize
    )
    // 更新本地显示
    personalStats.department = editForm.department
    personalStats.title = editForm.title
    personalStats.familySize = editForm.familySize
    if (res.data && res.data.score !== undefined) {
      personalStats.score = res.data.score
    }
    editing.value = false
    ElMessage.success(res.message || '更新成功')
  } catch (e) {
    ElMessage.error(e.message || '更新失败')
  } finally {
    saving.value = false
  }
}

// ========== 加载数据 ==========
onMounted(async () => {
  // 加载住房标准（两个角色都需要）
  try {
    const stdRes = await getStandardList()
    standards.value = stdRes.data || []
  } catch (e) {}

  if (isAdmin.value) {
    // 管理员：加载系统统计
    try {
      const statsRes = await getStatistics()
      stats.value = statsRes.data || {}
    } catch (e) {}
  } else {
    // 住户：加载个人统计
    try {
      const appRes = await getApplicationList()
      const allApps = appRes.data || []
      const myApps = allApps.filter(a => a.applicantName === username.value)
      personalStats.applicationCount = myApps.length
      personalStats.approvedCount = myApps.filter(a => a.applyStatus === 1).length
      personalStats.pendingCount = myApps.filter(a => a.applyStatus === 0).length

      // 从住户列表查找住房信息
      try {
        const residentRes = await getResidentList()
        const residents = residentRes.data || []
        const me = residents.find(r => r.ownerName === username.value)
        if (me) {
          personalStats.houseNo = me.houseNo
          personalStats.houseArea = me.houseArea
          personalStats.score = me.score
          personalStats.familySize = me.familySize
          personalStats.department = me.department
          personalStats.title = me.title
        }
      } catch (e) {}
    } catch (e) {}
  }
})
</script>

<style scoped>
.stat-cards { margin-bottom: 20px; }
</style>
