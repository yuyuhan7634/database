<template>
  <div class="bill-list">
    <h2 class="page-title">房租账单</h2>

    <el-row :gutter="20" style="margin-bottom:20px">
      <el-col :span="8">
        <el-card shadow="never">
          <template #header><span>生成月度账单</span></template>
          <el-form :model="billForm" inline>
            <el-form-item label="月份">
              <el-date-picker
                v-model="billForm.month"
                type="month"
                placeholder="选择月份"
                value-format="YYYY-MM"
                format="YYYY-MM"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleGenerate" :loading="genLoading">生成账单</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never">
      <template #header>
        <span>账单明细</span>
        <el-button type="primary" size="small" @click="fetchData" style="float:right">刷新</el-button>
      </template>
      <el-table :data="list" stripe v-loading="loading" style="width:100%">
        <el-table-column type="index" label="#" width="60" />
        <el-table-column prop="houseNo" label="房号" width="180" />
        <el-table-column prop="ownerName" label="户主" width="100" />
        <el-table-column prop="billMonth" label="账单月份" width="120" />
        <el-table-column prop="rentAmount" label="房租金额 (元)" width="150">
          <template #default="{ row }">{{ row.rentAmount.toFixed(2) }} 元</template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="130">
          <template #default="{ row }">{{ row.createTime || '-' }}</template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 结果弹窗 -->
    <el-dialog v-model="resultVisible" title="操作结果" width="400px">
      <el-result :icon="resultSuccess ? 'success' : 'error'" :title="resultSuccess ? '账单生成成功' : '生成失败'">
        <template #extra>
          <p>{{ resultMsg }}</p>
          <el-button type="primary" @click="resultVisible = false; fetchData()" style="margin-top:12px">确定</el-button>
        </template>
      </el-result>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getBillList, generateBills } from '../../api/resident'

const list = ref([])
const loading = ref(false)
const genLoading = ref(false)
const resultVisible = ref(false)
const resultSuccess = ref(false)
const resultMsg = ref('')
const billForm = ref({ month: '' })

async function fetchData() {
  loading.value = true
  try {
    const res = await getBillList()
    list.value = res.data || []
  } catch (e) {}
  loading.value = false
}

async function handleGenerate() {
  if (!billForm.value.month) {
    ElMessage.warning('请选择月份')
    return
  }
  genLoading.value = true
  try {
    const res = await generateBills(billForm.value.month)
    resultSuccess.value = true
    resultMsg.value = res.data || '账单生成成功'
  } catch (e) {
    resultSuccess.value = false
    resultMsg.value = e.message || '生成失败'
  } finally {
    genLoading.value = false
    resultVisible.value = true
  }
}

onMounted(fetchData)
</script>

<style scoped>
.page-title { margin: 0 0 20px; font-size: 22px; }
</style>
