<template>
  <div class="standard-list">
    <h2 class="page-title">住房标准</h2>
    <el-card shadow="never">
      <template #header>
        <span>面积对应的最低分数要求</span>
        <el-button type="primary" size="small" @click="fetchData" style="float:right">刷新</el-button>
      </template>
      <el-table :data="list" stripe v-loading="loading" style="width:100%">
        <el-table-column prop="area" label="面积 (㎡)" width="200" />
        <el-table-column prop="minScore" label="最低住房分数" width="200" />
        <el-table-column label="说明">
          <template #default="{ row }">
            申请 {{ row.area }} ㎡ 房屋需达到 <b>{{ row.minScore }}</b> 分
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && list.length === 0" description="暂无住房标准数据" />
    </el-card>

    <!-- 分数计算规则 -->
    <el-card shadow="never" style="margin-top:20px">
      <template #header><span>分数计算规则</span></template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="教授 / 处长">100 分</el-descriptions-item>
        <el-descriptions-item label="副教授">80 分</el-descriptions-item>
        <el-descriptions-item label="科长 / 研究员">80 分</el-descriptions-item>
        <el-descriptions-item label="讲师 / 工程师">60 分</el-descriptions-item>
        <el-descriptions-item label="实验员 / 干事">50 分</el-descriptions-item>
        <el-descriptions-item label="助教">40 分</el-descriptions-item>
        <el-descriptions-item label="其他职称">40 分</el-descriptions-item>
        <el-descriptions-item label="家庭人口加成">每多 1 人 + 10 分</el-descriptions-item>
      </el-descriptions>
      <el-alert title="计算公式" type="info" :closable="false" show-icon style="margin-top:12px">
        最终分数 = 职称基础分 + 家庭人口 × 10
      </el-alert>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getStandardList } from '../../api/house'

const list = ref([])
const loading = ref(false)

async function fetchData() {
  loading.value = true
  try {
    const res = await getStandardList()
    list.value = res.data || []
  } catch (e) {}
  loading.value = false
}

onMounted(fetchData)
</script>

<style scoped>
</style>
