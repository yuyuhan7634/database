<template>
  <div class="available-houses">
    <h2 class="page-title">空闲房产</h2>
    <el-card shadow="never">
      <template #header>
        <span>当前空闲房屋（可分配）</span>
        <el-tag type="success" style="margin-left:12px">{{ list.length }} 套</el-tag>
        <el-button type="primary" size="small" @click="fetchData" style="float:right">刷新</el-button>
      </template>
      <el-table :data="list" stripe v-loading="loading" style="width:100%">
        <el-table-column prop="houseNo" label="房号" width="200" />
        <el-table-column prop="area" label="面积 (㎡)" width="150" />
        <el-table-column prop="rentPerSqm" label="每平米房租 (元)" width="160" />
        <el-table-column label="预计月租金" width="150">
          <template #default="{ row }">{{ (row.area * row.rentPerSqm).toFixed(2) }} 元</template>
        </el-table-column>
        <!-- 管理员可删除空闲房屋 -->
        <el-table-column v-if="isAdmin" label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && list.length === 0" description="暂无空闲房屋" />
    </el-card>

    <!-- 操作结果弹窗 -->
    <ResultDialog
      v-model="resultVisible"
      :success="resultSuccess"
      :message="resultMsg"
      @confirm="resultVisible = false; fetchData()"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessageBox } from 'element-plus'
import { useUser } from '../../composables/useUser'
import { getAvailableHouses } from '../../api/house'
import { deleteHouse } from '../../api/admin'
import ResultDialog from '../../components/ResultDialog.vue'

const { isAdmin } = useUser()

const list = ref([])
const loading = ref(false)

const resultVisible = ref(false)
const resultSuccess = ref(false)
const resultMsg = ref('')

async function fetchData() {
  loading.value = true
  try {
    const res = await getAvailableHouses()
    list.value = res.data || []
  } catch (e) {}
  loading.value = false
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确认删除房屋「${row.houseNo}」吗？该操作不可撤销。`,
      '确认删除',
      { type: 'warning', confirmButtonText: '确认删除' }
    )
    const res = await deleteHouse(row.houseNo)
    showResult(true, res.data || '删除成功')
  } catch (e) {
    if (e !== 'cancel') {
      showResult(false, e.message || '删除失败')
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
</style>
