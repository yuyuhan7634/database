<template>
  <div class="rent-manage">
    <h2 class="page-title">租金管理</h2>

    <el-row :gutter="20">
      <el-col :span="12">
        <el-card shadow="never">
          <template #header><span>调整房屋每平米租金</span></template>
          <el-form :model="rentForm" label-width="120px">
            <el-form-item label="房号" required>
              <el-select v-model="rentForm.houseNo" filterable placeholder="请选择房号" style="width:100%">
                <el-option v-for="h in houses" :key="h.houseNo" :label="h.houseNo" :value="h.houseNo" />
              </el-select>
            </el-form-item>
            <el-form-item label="当前租金">
              <el-tag v-if="selectedHouse" type="info">{{ selectedHouse.rentPerSqm }} 元/㎡</el-tag>
              <span v-else class="text-gray">请先选择房号</span>
            </el-form-item>
            <el-form-item label="新租金 (元/㎡)" required>
              <el-input-number v-model="rentForm.newRent" :min="1" :max="100" :step="1" :precision="2" style="width:200px" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleUpdateRent" :loading="rentLoading">确认调整</el-button>
            </el-form-item>
          </el-form>

          <template v-if="selectedHouse">
            <el-divider />
            <p><b>房屋信息</b></p>
            <p>房号：{{ selectedHouse.houseNo }}</p>
            <p>面积：{{ selectedHouse.area }} ㎡</p>
            <p>当前月租金：{{ (selectedHouse.area * selectedHouse.rentPerSqm).toFixed(2) }} 元</p>
            <p>调整后月租金：{{ (selectedHouse.area * rentForm.newRent).toFixed(2) }} 元</p>
          </template>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card shadow="never">
          <template #header><span>全部房屋租金一览</span></template>
          <el-table :data="houses" stripe v-loading="houseLoading" style="width:100%" max-height="500">
            <el-table-column prop="houseNo" label="房号" width="160" />
            <el-table-column prop="area" label="面积" width="80" />
            <el-table-column prop="rentPerSqm" label="单价 (元/㎡)" width="120" />
            <el-table-column label="月租金" width="120">
              <template #default="{ row }">{{ (row.area * row.rentPerSqm).toFixed(2) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.status === 0 ? 'success' : 'danger'" size="small">
                  {{ row.status === 0 ? '空' : '住' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- 结果弹窗 -->
    <ResultDialog
      v-model="resultVisible"
      :success="resultSuccess"
      :message="resultMsg"
      success-title="调整成功"
      fail-title="调整失败"
      @confirm="resultVisible = false; fetchHouses()"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getHouseList } from '../../api/house'
import { updateHouseRent } from '../../api/admin'
import ResultDialog from '../../components/ResultDialog.vue'

const houses = ref([])
const houseLoading = ref(false)
const rentLoading = ref(false)
const resultVisible = ref(false)
const resultSuccess = ref(false)
const resultMsg = ref('')

const rentForm = ref({ houseNo: '', newRent: 10 })

const selectedHouse = computed(() => {
  return houses.value.find(h => h.houseNo === rentForm.value.houseNo) || null
})

async function fetchHouses() {
  houseLoading.value = true
  try {
    const res = await getHouseList()
    houses.value = res.data || []
  } catch (e) {}
  houseLoading.value = false
}

async function handleUpdateRent() {
  if (!rentForm.value.houseNo) {
    ElMessage.warning('请选择房号')
    return
  }
  rentLoading.value = true
  try {
    const res = await updateHouseRent(rentForm.value.houseNo, rentForm.value.newRent)
    resultSuccess.value = true
    resultMsg.value = res.data || '租金调整成功'
  } catch (e) {
    resultSuccess.value = false
    resultMsg.value = e.message || '调整失败'
  } finally {
    rentLoading.value = false
    resultVisible.value = true
  }
}

onMounted(fetchHouses)
</script>

<style scoped>
.text-gray { color: #909399; }
</style>
