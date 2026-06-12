<template>
  <div class="statistics" v-loading="loading">
    <h2 class="page-title">统计报表</h2>

    <!-- 综合统计 -->
    <el-row :gutter="20" style="margin-bottom:20px">
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
          <div class="stat-label">已分配</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value" style="color:#e6a23c">{{ stats.occupancyRate || '0%' }}</div>
          <div class="stat-label">入住率</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <!-- 按部门统计 -->
      <el-col :span="12">
        <el-card shadow="never">
          <template #header><span>按部门统计</span></template>
          <el-table :data="stats.departmentStats || []" stripe>
            <el-table-column prop="department" label="部门" />
            <el-table-column prop="count" label="人数" />
          </el-table>
          <el-empty v-if="!(stats.departmentStats || []).length" description="暂无数据" />
        </el-card>
      </el-col>
      <!-- 按职称统计 -->
      <el-col :span="12">
        <el-card shadow="never">
          <template #header><span>按职称统计</span></template>
          <el-table :data="stats.titleStats || []" stripe>
            <el-table-column prop="title" label="职称" />
            <el-table-column prop="count" label="人数" />
          </el-table>
          <el-empty v-if="!(stats.titleStats || []).length" description="暂无数据" />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top:20px">
      <!-- 面积区间统计 -->
      <el-col :span="12">
        <el-card shadow="never">
          <template #header><span>按住房面积统计</span></template>
          <el-table :data="areaStats" stripe>
            <el-table-column prop="label" label="面积区间" />
            <el-table-column prop="count" label="户数" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getStatistics } from '../../api/admin'

const stats = ref({})
const loading = ref(false)

const areaStats = computed(() => {
  const s = stats.value
  return [
    { label: '≤ 60 ㎡（小户型）', count: s.smallHouseCount || 0 },
    { label: '60 ~ 100 ㎡（中户型）', count: s.mediumHouseCount || 0 },
    { label: '> 100 ㎡（大户型）', count: s.largeHouseCount || 0 }
  ]
})

onMounted(async () => {
  loading.value = true
  try {
    const res = await getStatistics()
    stats.value = res.data || {}
  } catch (e) {}
  loading.value = false
})
</script>

<style scoped></style>
