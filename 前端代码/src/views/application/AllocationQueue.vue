<template>
  <div class="allocation-queue">
    <h2 class="page-title">分房队列</h2>

    <el-row :gutter="20">
      <el-col :span="24" style="margin-bottom:20px">
        <el-alert title="每月最后一天 23:00 自动执行分房活动。也可点击下方按钮手动触发。" type="info" :closable="false" show-icon />
      </el-col>
    </el-row>

    <!-- 分房队列 -->
    <el-card shadow="never">
      <template #header>
        <span>排队中的分房申请（按分数降序排列）</span>
        <div style="float:right">
          <el-button type="success" @click="handleBatchAllocate" :loading="batchLoading">
            <el-icon><Collection /></el-icon> 手动执行批量分房
          </el-button>
        </div>
      </template>
      <el-table :data="queue" stripe v-loading="loading" style="width:100%">
        <el-table-column type="index" label="#" width="60" />
        <el-table-column prop="applyNo" label="编号" width="180" />
        <el-table-column prop="applicantName" label="申请人" width="100" />
        <el-table-column prop="department" label="部门" width="140" />
        <el-table-column prop="score" label="分数" width="70">
          <template #default="{ row }">
            <el-tag type="success" size="small">{{ row.score }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reqArea" label="要求面积" width="100">
          <template #default="{ row }">{{ row.reqArea ? row.reqArea + ' ㎡' : '-' }}</template>
        </el-table-column>
        <el-table-column prop="title" label="职称" width="100" />
        <el-table-column prop="familySize" label="家庭人口" width="90" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleApprove(row.applyNo)">分房</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && queue.length === 0" description="队列为空，暂无等待分房的申请" />
    </el-card>

    <!-- 操作结果弹窗 -->
    <el-dialog v-model="resultVisible" title="操作结果" width="550px">
      <el-result :icon="resultSuccess ? 'success' : 'error'" :title="resultSuccess ? '操作成功' : '操作失败'">
        <template #extra>
          <div style="white-space:pre-wrap;text-align:left;background:#f5f7fa;padding:12px;border-radius:4px;">{{ resultMsg }}</div>
          <el-button type="primary" @click="resultVisible = false; fetchQueue()" style="margin-top:12px">确定</el-button>
        </template>
      </el-result>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getAllocationQueue, approveHousing, manualMonthlyAllocate } from '../../api/application'
import { ElMessage, ElMessageBox } from 'element-plus'

const queue = ref([])
const loading = ref(false)
const batchLoading = ref(false)
const resultVisible = ref(false)
const resultSuccess = ref(false)
const resultMsg = ref('')

async function fetchQueue() {
  loading.value = true
  try {
    const res = await getAllocationQueue()
    queue.value = res.data || []
  } catch (e) {}
  loading.value = false
}

async function handleApprove(applyNo) {
  try {
    await ElMessageBox.confirm('确认给该申请分配房源吗？', '确认操作', { type: 'warning' })
    const res = await approveHousing(applyNo)
    resultSuccess.value = true
    resultMsg.value = res.data || '分房成功'
    resultVisible.value = true
  } catch (e) {
    if (e !== 'cancel') {
      resultSuccess.value = false
      resultMsg.value = e.message || '分房失败'
      resultVisible.value = true
    }
  }
}

async function handleBatchAllocate() {
  try {
    await ElMessageBox.confirm(
      '确认执行批量分房？系统将为分房队列中所有符合条件的申请分配房源。',
      '确认批量分房',
      { type: 'warning', confirmButtonText: '执行分房' }
    )
    batchLoading.value = true
    const res = await manualMonthlyAllocate()
    batchLoading.value = false
    resultSuccess.value = true
    resultMsg.value = res.data || '批量分房执行完毕'
    resultVisible.value = true
  } catch (e) {
    batchLoading.value = false
    if (e !== 'cancel') {
      resultSuccess.value = false
      resultMsg.value = e.message || '批量分房失败'
      resultVisible.value = true
    }
  }
}

onMounted(fetchQueue)
</script>

<style scoped>
.page-title { margin: 0 0 20px; font-size: 22px; }
</style>
