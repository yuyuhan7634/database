<template>
  <div class="resident-list">
    <h2 class="page-title">住户列表</h2>
    <el-card shadow="never">
      <template #header>
        <span>当前在住人员</span>
        <el-button type="primary" size="small" @click="fetchData" style="float:right">刷新</el-button>
      </template>
      <el-table :data="list" stripe v-loading="loading" style="width:100%">
        <el-table-column prop="ownerName" label="户主" width="100" />
        <el-table-column prop="department" label="部门" width="160" />
        <el-table-column prop="title" label="职称" width="100" />
        <el-table-column prop="familySize" label="家庭人口" width="100" />
        <el-table-column prop="score" label="住房分数" width="100" />
        <el-table-column prop="houseNo" label="房号" width="180" />
        <el-table-column prop="houseArea" label="面积 (㎡)" width="100" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getResidentList } from '../../api/resident'

const list = ref([])
const loading = ref(false)

async function fetchData() {
  loading.value = true
  try {
    const res = await getResidentList()
    list.value = res.data || []
  } catch (e) {}
  loading.value = false
}

onMounted(fetchData)
</script>

<style scoped>
</style>
