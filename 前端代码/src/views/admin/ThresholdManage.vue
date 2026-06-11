<template>
  <div class="threshold-manage">
    <h2 class="page-title">阈值管理</h2>

    <el-row :gutter="20">
      <!-- 阈值列表 -->
      <el-col :span="14">
        <el-card shadow="never">
          <template #header><span>住房标准阈值 (面积 → 最低分数)</span></template>
          <el-table :data="list" stripe v-loading="loading" style="width:100%">
            <el-table-column prop="area" label="面积 (㎡)" width="150" />
            <el-table-column prop="minScore" label="最低住房分数" width="150" />
            <el-table-column label="操作">
              <template #default="{ row }">
                <el-button type="primary" size="small" @click="editRow(row)">修改阈值</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <!-- 查询条件 -->
      <el-col :span="10">
        <el-card shadow="never">
          <template #header><span>查询住房条件（按面积范围）</span></template>
          <el-form :model="queryForm" inline>
            <el-form-item label="最小面积">
              <el-input-number v-model="queryForm.minArea" :min="0" :step="10" />
            </el-form-item>
            <el-form-item label="最大面积">
              <el-input-number v-model="queryForm.maxArea" :min="0" :step="10" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleQuery">查询</el-button>
            </el-form-item>
          </el-form>
          <el-table :data="queryResult" stripe v-if="queryResult.length">
            <el-table-column prop="area" label="面积 (㎡)" />
            <el-table-column prop="minScore" label="最低分数" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- 修改阈值弹窗 -->
    <el-dialog v-model="editVisible" title="修改住房标准" width="400px">
      <el-form :model="editForm" label-width="120px">
        <el-form-item label="面积">
          <el-input :value="editForm.area + ' ㎡'" disabled />
        </el-form-item>
        <el-form-item label="当前最低分数">
          <el-tag>{{ editForm.oldScore }} 分</el-tag>
        </el-form-item>
        <el-form-item label="新最低分数">
          <el-input-number v-model="editForm.newScore" :min="0" :max="200" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="handleUpdate" :loading="updateLoading">确认修改</el-button>
      </template>
    </el-dialog>

    <!-- 结果弹窗 -->
    <ResultDialog
      v-model="resultVisible"
      :success="resultSuccess"
      :message="resultMsg"
      success-title="修改成功"
      fail-title="修改失败"
      @confirm="resultVisible = false; fetchData()"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getThresholds, updateStandard, getHousingConditions } from '../../api/admin'
import ResultDialog from '../../components/ResultDialog.vue'

const list = ref([])
const loading = ref(false)

const editVisible = ref(false)
const editForm = ref({ area: 0, oldScore: 0, newScore: 0 })
const updateLoading = ref(false)

const queryForm = ref({ minArea: null, maxArea: null })
const queryResult = ref([])

const resultVisible = ref(false)
const resultSuccess = ref(false)
const resultMsg = ref('')

async function fetchData() {
  loading.value = true
  try {
    const res = await getThresholds()
    list.value = res.data || []
  } catch (e) {}
  loading.value = false
}

function editRow(row) {
  editForm.value = { area: row.area, oldScore: row.minScore, newScore: row.minScore }
  editVisible.value = true
}

async function handleUpdate() {
  updateLoading.value = true
  try {
    const res = await updateStandard(editForm.value.area, editForm.value.newScore)
    resultSuccess.value = true
    resultMsg.value = res.data || '修改成功'
  } catch (e) {
    resultSuccess.value = false
    resultMsg.value = e.message || '修改失败'
  } finally {
    updateLoading.value = false
    editVisible.value = false
    resultVisible.value = true
  }
}

async function handleQuery() {
  try {
    const minArea = queryForm.value.minArea || undefined
    const maxArea = queryForm.value.maxArea || undefined
    if (!minArea && !maxArea) {
      queryResult.value = list.value
      return
    }
    const res = await getHousingConditions(minArea, maxArea)
    queryResult.value = res.data || []
  } catch (e) {}
}

onMounted(fetchData)
</script>

<style scoped>
</style>
