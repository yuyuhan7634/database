<template>
  <div class="app-list">
    <h2 class="page-title">
      申请列表
      <el-tag v-if="!isAdmin" type="success" size="small" style="margin-left:12px;vertical-align:middle">
        仅显示我的申请
      </el-tag>
    </h2>
    <el-card shadow="never">
      <template #header>
        <span>{{ isAdmin ? '所有申请单' : '我的申请单' }}</span>
        <div style="float:right;display:flex;align-items:center;gap:12px">
          <!-- 管理员可按状态筛选 -->
          <el-select
            v-if="isAdmin"
            v-model="statusFilter"
            placeholder="筛选状态"
            clearable
            size="small"
            style="width:130px"
            @change="applyFilter"
          >
            <el-option label="全部" :value="null" />
            <el-option label="待处理" :value="0" />
            <el-option label="已通过" :value="1" />
            <el-option label="已驳回" :value="2" />
          </el-select>
          <el-button type="primary" size="small" @click="fetchData">刷新</el-button>
        </div>
      </template>
      <el-table :data="displayList" stripe v-loading="loading" style="width:100%">
        <el-table-column prop="applyNo" label="申请编号" width="190" />
        <el-table-column prop="applicantName" label="申请人" width="100" />
        <el-table-column prop="department" label="部门" width="150" />
        <el-table-column label="类型" width="80">
          <template #default="{ row }">
            <el-tag :type="row.applyType === 1 ? 'primary' : row.applyType === 2 ? 'warning' : 'info'" size="small">
              {{ { 1: '分房', 2: '调房', 3: '退房' }[row.applyType] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.applyStatus === 0 ? 'warning' : row.applyStatus === 1 ? 'success' : 'danger'" size="small">
              {{ { 0: '待处理', 1: '已通过', 2: '已驳回' }[row.applyStatus] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="score" label="分数" width="70" />
        <el-table-column prop="reqArea" label="要求面积" width="100">
          <template #default="{ row }">{{ row.reqArea ? row.reqArea + ' ㎡' : '-' }}</template>
        </el-table-column>
        <el-table-column prop="allocatedHouseNo" label="分配房号" width="130" />
        <el-table-column prop="createTime" label="创建时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column prop="rejectReason" label="驳回原因" min-width="150">
          <template #default="{ row }">
            <el-tooltip v-if="row.rejectReason" :content="row.rejectReason">
              <el-text type="danger" truncated>{{ row.rejectReason }}</el-text>
            </el-tooltip>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <!-- 管理员审批操作列 -->
        <el-table-column v-if="isAdmin" label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <template v-if="row.applyStatus === 0">
              <el-button
                v-if="row.applyType === 1"
                type="success" size="small" @click="handleApprove(row.applyNo)"
              >
                分房
              </el-button>
              <el-button
                v-if="row.applyType === 2"
                type="warning" size="small" @click="handleTransfer(row.applyNo)"
              >
                调房
              </el-button>
              <el-button
                v-if="row.applyType === 3"
                type="info" size="small" @click="handleCheckout(row.applyNo)"
              >
                退房
              </el-button>
              <el-button
                type="danger" size="small" @click="handleReject(row)"
                style="margin-top:4px"
              >
                驳回
              </el-button>
            </template>
            <el-tag v-else type="info" size="small">已处理</el-tag>
          </template>
        </el-table-column>
        <!-- 住户操作列：撤回 -->
        <el-table-column v-if="!isAdmin" label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.applyStatus === 0"
              type="warning" size="small" plain
              @click="handleCancel(row)"
            >
              撤回
            </el-button>
            <el-tag v-else type="info" size="small">已处理</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && displayList.length === 0" :description="isAdmin ? '暂无申请记录' : '您还没有提交过申请'" />
    </el-card>

    <!-- 驳回原因输入弹窗 -->
    <el-dialog v-model="rejectVisible" title="驳回申请" width="450px">
      <el-form :model="rejectForm">
        <el-form-item label="驳回原因" required>
          <el-input
            v-model="rejectForm.reason"
            type="textarea"
            :rows="3"
            placeholder="请输入驳回原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmReject" :loading="rejectLoading">确认驳回</el-button>
      </template>
    </el-dialog>

    <!-- 操作结果弹窗 -->
    <el-dialog v-model="resultVisible" title="操作结果" width="500px">
      <el-result :icon="resultSuccess ? 'success' : 'error'" :title="resultSuccess ? '操作成功' : '操作失败'">
        <template #extra>
          <div style="white-space:pre-wrap;text-align:left;background:#f5f7fa;padding:12px;border-radius:4px;">{{ resultMsg }}</div>
          <el-button type="primary" @click="resultVisible = false; fetchData()" style="margin-top:12px">确定</el-button>
        </template>
      </el-result>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessageBox } from 'element-plus'
import { useUser } from '../../composables/useUser'
import { getApplicationList, approveHousing, approveTransfer, approveCheckout, rejectApplication, cancelApplication } from '../../api/application'

const { isAdmin, username } = useUser()

const list = ref([])
const loading = ref(false)
const statusFilter = ref(null)

// 驳回相关
const rejectVisible = ref(false)
const rejectForm = ref({ applyNo: '', reason: '' })
const rejectLoading = ref(false)

// 结果弹窗
const resultVisible = ref(false)
const resultSuccess = ref(false)
const resultMsg = ref('')

// 按角色和筛选条件展示
const displayList = computed(() => {
  let result = list.value

  // 住户只看自己的
  if (!isAdmin.value) {
    result = result.filter(a => a.applicantName === username.value)
  }

  // 管理员按状态筛选
  if (isAdmin.value && statusFilter.value !== null) {
    result = result.filter(a => a.applyStatus === statusFilter.value)
  }

  return result
})

function formatTime(t) {
  if (!t) return '-'
  return t.replace('T', ' ')
}

function applyFilter() {
  // computed 自动响应，无需额外操作
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getApplicationList()
    list.value = res.data || []
  } catch (e) {}
  loading.value = false
}

// ===== 管理员审批操作 =====

async function handleApprove(applyNo) {
  try {
    await ElMessageBox.confirm('确认给该申请分配房源吗？', '确认分房', { type: 'warning' })
    const res = await approveHousing(applyNo)
    showResult(true, res.data || '分房成功')
  } catch (e) {
    if (e !== 'cancel') showResult(false, e.message || '分房失败')
  }
}

async function handleTransfer(applyNo) {
  try {
    await ElMessageBox.confirm('确认执行该调房申请吗？', '确认调房', { type: 'warning' })
    const res = await approveTransfer(applyNo)
    showResult(true, res.data || '调房成功')
  } catch (e) {
    if (e !== 'cancel') showResult(false, e.message || '调房失败')
  }
}

async function handleCheckout(applyNo) {
  try {
    await ElMessageBox.confirm('确认执行该退房申请吗？', '确认退房', { type: 'warning' })
    const res = await approveCheckout(applyNo)
    showResult(true, res.data || '退房成功')
  } catch (e) {
    if (e !== 'cancel') showResult(false, e.message || '退房失败')
  }
}

function handleReject(row) {
  rejectForm.value = { applyNo: row.applyNo, reason: '' }
  rejectVisible.value = true
}

async function confirmReject() {
  if (!rejectForm.value.reason.trim()) {
    showResult(false, '请输入驳回原因')
    rejectVisible.value = false
    return
  }
  rejectLoading.value = true
  try {
    const res = await rejectApplication(rejectForm.value.applyNo, rejectForm.value.reason)
    rejectVisible.value = false
    showResult(true, res.data || '驳回成功')
  } catch (e) {
    rejectVisible.value = false
    showResult(false, e.message || '驳回失败')
  } finally {
    rejectLoading.value = false
  }
}

// ===== 撤回申请（住户） =====
async function handleCancel(row) {
  try {
    await ElMessageBox.confirm(
      `确认撤回该申请吗？编号：${row.applyNo}`,
      '确认撤回',
      { type: 'warning', confirmButtonText: '确认撤回' }
    )
    const res = await cancelApplication(row.applyNo, row.applicantName)
    showResult(true, res.data || '撤回成功')
  } catch (e) {
    if (e !== 'cancel') {
      showResult(false, e.message || '撤回失败')
    }
  }
}

function showResult(success, msg) {
  resultSuccess.value = success
  resultMsg.value = msg
  resultVisible.value = true
}

onMounted(fetchData)
</script>

<style scoped>
.page-title { margin: 0 0 20px; font-size: 22px; }
</style>
