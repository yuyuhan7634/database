<template>
  <div class="house-list">
    <h2 class="page-title">房产列表</h2>
    <el-card shadow="never">
      <template #header>
        <span>全部房屋信息</span>
        <div style="float:right;display:flex;align-items:center;gap:12px">
          <!-- 管理员可新增房屋 -->
          <el-button v-if="isAdmin" type="success" size="small" @click="openAddDialog">
            <el-icon><Plus /></el-icon> 新增房屋
          </el-button>
          <el-button type="primary" size="small" @click="fetchData">刷新</el-button>
        </div>
      </template>
      <el-table :data="list" stripe v-loading="loading" style="width:100%">
        <el-table-column prop="houseNo" label="房号" width="180" />
        <el-table-column prop="area" label="面积 (㎡)" width="120">
          <template #default="{ row }">{{ row.area }} ㎡</template>
        </el-table-column>
        <el-table-column prop="rentPerSqm" label="每平米房租 (元)" width="150">
          <template #default="{ row }">{{ row.rentPerSqm }} 元/㎡</template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'danger'" size="small">
              {{ row.status === 0 ? '空闲' : '已分配' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="月租金" width="120">
          <template #default="{ row }">
            {{ (row.area * row.rentPerSqm).toFixed(2) }} 元
          </template>
        </el-table-column>
        <!-- 操作列：管理员可见编辑/删除，普通用户仅详情 -->
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="showDetail(row)">详情</el-button>
            <template v-if="isAdmin">
              <el-button size="small" type="warning" @click="openEditDialog(row)">编辑</el-button>
              <el-button
                size="small" type="danger"
                :disabled="row.status === 1"
                @click="handleDelete(row)"
              >
                删除
              </el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 房屋详情弹窗（两个角色都可见） -->
    <el-dialog v-model="detailVisible" title="房屋详情" width="400px">
      <el-descriptions :column="1" border v-if="detail">
        <el-descriptions-item label="房号">{{ detail.houseNo }}</el-descriptions-item>
        <el-descriptions-item label="面积">{{ detail.area }} ㎡</el-descriptions-item>
        <el-descriptions-item label="每平米房租">{{ detail.rentPerSqm }} 元</el-descriptions-item>
        <el-descriptions-item label="月租金">{{ (detail.area * detail.rentPerSqm).toFixed(2) }} 元</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="detail.status === 0 ? 'success' : 'danger'" size="small">
            {{ detail.status === 0 ? '空闲' : '已分配' }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 新增 / 编辑房屋弹窗（仅管理员） -->
    <el-dialog
      v-model="formVisible"
      :title="isEdit ? '编辑房屋信息' : '新增房屋'"
      width="480px"
    >
      <el-form :model="formData" :rules="formRules" ref="formRef" label-width="130px">
        <el-form-item label="房号" prop="houseNo">
          <el-input
            v-model="formData.houseNo"
            placeholder="请输入房号（如 A-101）"
            :disabled="isEdit"
          />
        </el-form-item>
        <el-form-item label="面积 (㎡)" prop="area">
          <el-input-number
            v-model="formData.area"
            :min="10"
            :max="500"
            :step="5"
            :precision="2"
            style="width:100%"
          />
        </el-form-item>
        <el-form-item label="每平米租金 (元)" prop="rentPerSqm">
          <el-input-number
            v-model="formData.rentPerSqm"
            :min="1"
            :max="200"
            :step="1"
            :precision="2"
            style="width:100%"
          />
        </el-form-item>
        <el-form-item v-if="isEdit" label="状态">
          <el-radio-group v-model="formData.status">
            <el-radio :value="0">空闲</el-radio>
            <el-radio :value="1">已分配</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="!isEdit" label="月租金预览">
          <el-tag type="warning" size="large">
            {{ formData.area && formData.rentPerSqm ? (formData.area * formData.rentPerSqm).toFixed(2) + ' 元' : '—' }}
          </el-tag>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saveLoading">
          {{ isEdit ? '保存修改' : '确认添加' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 操作结果弹窗 -->
    <el-dialog v-model="resultVisible" title="操作结果" width="400px">
      <el-result :icon="resultSuccess ? 'success' : 'error'" :title="resultSuccess ? '操作成功' : '操作失败'">
        <template #extra>
          <p>{{ resultMsg }}</p>
          <el-button type="primary" @click="resultVisible = false; fetchData()" style="margin-top:12px">确定</el-button>
        </template>
      </el-result>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessageBox } from 'element-plus'
import { useUser } from '../../composables/useUser'
import { getHouseList } from '../../api/house'
import { addHouse, updateHouse, deleteHouse } from '../../api/admin'

const { isAdmin } = useUser()

const list = ref([])
const loading = ref(false)

// 详情
const detailVisible = ref(false)
const detail = ref(null)

// 新增/编辑表单
const formVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const saveLoading = ref(false)
const formData = reactive({
  houseNo: '',
  area: null,
  rentPerSqm: null,
  status: 0
})

const formRules = {
  houseNo: [{ required: true, message: '请输入房号', trigger: 'blur' }],
  area: [{ required: true, message: '请输入面积', trigger: 'blur' }],
  rentPerSqm: [{ required: true, message: '请输入每平米租金', trigger: 'blur' }]
}

// 结果弹窗
const resultVisible = ref(false)
const resultSuccess = ref(false)
const resultMsg = ref('')

async function fetchData() {
  loading.value = true
  try {
    const res = await getHouseList()
    list.value = res.data || []
  } catch (e) {}
  loading.value = false
}

function showDetail(row) {
  detail.value = row
  detailVisible.value = true
}

// ===== 新增 =====
function openAddDialog() {
  isEdit.value = false
  formData.houseNo = ''
  formData.area = null
  formData.rentPerSqm = null
  formData.status = 0
  formRef.value?.clearValidate()
  formVisible.value = true
}

// ===== 编辑 =====
function openEditDialog(row) {
  isEdit.value = true
  formData.houseNo = row.houseNo
  formData.area = row.area
  formData.rentPerSqm = row.rentPerSqm
  formData.status = row.status
  formRef.value?.clearValidate()
  formVisible.value = true
}

// ===== 保存（新增/编辑） =====
async function handleSave() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  saveLoading.value = true
  try {
    let res
    if (isEdit.value) {
      res = await updateHouse(
        formData.houseNo,
        formData.area,
        formData.rentPerSqm,
        formData.status
      )
    } else {
      res = await addHouse(
        formData.houseNo,
        formData.area,
        formData.rentPerSqm
      )
    }
    formVisible.value = false
    showResult(true, res.data || '操作成功')
  } catch (e) {
    formVisible.value = false
    showResult(false, e.message || '操作失败')
  } finally {
    saveLoading.value = false
  }
}

// ===== 删除 =====
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
.page-title { margin: 0 0 20px; font-size: 22px; }
</style>
