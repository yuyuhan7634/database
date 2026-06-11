<template>
  <div class="app-submit">
    <h2 class="page-title">提交申请</h2>
    <el-row :gutter="20">
      <!-- 左侧：申请表单 -->
      <el-col :span="14">
        <el-card shadow="never">
          <template #header>
            <span>填写申请表</span>
            <el-tag :type="form.applyType === 1 ? 'primary' : form.applyType === 2 ? 'warning' : 'info'" style="margin-left:12px">
              {{ typeLabel }}
            </el-tag>
          </template>
          <el-form :model="form" label-width="120px" :rules="rules" ref="formRef">
            <el-form-item label="申请人姓名" prop="applicantName">
              <el-input v-model="form.applicantName" placeholder="请输入姓名" />
            </el-form-item>
            <el-form-item label="所属部门" prop="department">
              <el-input v-model="form.department" placeholder="请输入部门名称" />
            </el-form-item>
            <el-form-item label="申请类型" prop="applyType">
              <el-radio-group v-model="form.applyType">
                <el-radio :value="1" border>分房申请</el-radio>
                <el-radio :value="2" border>调房申请</el-radio>
                <el-radio :value="3" border>退房申请</el-radio>
              </el-radio-group>
            </el-form-item>

            <!-- 分房/调房 额外字段 -->
            <template v-if="form.applyType === 1 || form.applyType === 2">
              <el-form-item label="职称" prop="title">
                <el-select v-model="form.title" placeholder="请选择职称" style="width:100%">
                  <el-option label="教授" value="教授" />
                  <el-option label="副教授" value="副教授" />
                  <el-option label="讲师" value="讲师" />
                  <el-option label="助教" value="助教" />
                  <el-option label="处长" value="处长" />
                  <el-option label="科长" value="科长" />
                  <el-option label="研究员" value="研究员" />
                  <el-option label="工程师" value="工程师" />
                  <el-option label="实验员" value="实验员" />
                  <el-option label="干事" value="干事" />
                </el-select>
              </el-form-item>
              <el-form-item label="家庭人口" prop="familySize">
                <el-input-number v-model="form.familySize" :min="1" :max="10" />
              </el-form-item>
              <el-form-item label="期望面积" prop="reqArea">
                <el-select v-model="form.reqArea" placeholder="请选择期望面积" style="width:100%">
                  <el-option v-for="s in standards" :key="s.area" :label="s.area + ' ㎡（需 ' + s.minScore + ' 分）'" :value="s.area" />
                </el-select>
              </el-form-item>
            </template>

            <!-- 调房/退房：原房号 -->
            <template v-if="form.applyType === 2 || form.applyType === 3">
              <el-form-item label="原房号" prop="oldHouseNo">
                <el-input v-model="form.oldHouseNo" placeholder="请输入原住房号" />
              </el-form-item>
            </template>

            <!-- 仅调房：原住房面积 -->
            <template v-if="form.applyType === 2">
              <el-form-item label="原住房面积" prop="oldHouseArea">
                <el-select v-model="form.oldHouseArea" placeholder="请选择原住房面积" style="width:100%">
                  <el-option v-for="s in standards" :key="s.area" :label="s.area + ' ㎡'" :value="s.area" />
                </el-select>
              </el-form-item>
            </template>

            <el-form-item>
              <el-button type="primary" @click="handleSubmit" :loading="submitting" size="large">提交申请</el-button>
              <el-button @click="resetForm">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <!-- 右侧：提示信息 -->
      <el-col :span="10">
        <el-card shadow="never">
          <template #header><span>申请说明</span></template>
          <el-alert title="分房申请" type="primary" :closable="false" show-icon>
            <template #default>
              提交后系统根据职称和家庭人口自动计算分数，<br>
              达到阈值后进入分房队列。<br>
              每月最后一天自动批量分配。
            </template>
          </el-alert>
          <el-alert title="调房申请" type="warning" :closable="false" show-icon style="margin-top:12px">
            提供原房号和原住房面积，系统退还原房后按分房流程重新分配。<br>
            原住房面积需与系统中登记的一致，否则会被驳回。
          </el-alert>
          <el-alert title="退房申请" type="info" :closable="false" show-icon style="margin-top:12px">
            提供原房号，系统校验通过后清理住户信息和账单，回收房屋。
          </el-alert>
          <el-divider />
          <div class="score-tip">
            <h4>分数计算规则</h4>
            <p>教授/处长：100 分 &nbsp;|&nbsp; 科长/研究员：80 分</p>
            <p>讲师/工程师：60 分 &nbsp;|&nbsp; 实验员/干事：50 分</p>
            <p>其他：40 分</p>
            <p><b>最终分数 = 基础分 + 家庭人口 × 10</b></p>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 提交结果弹窗 -->
    <ResultDialog
      v-model="resultVisible"
      :success="resultSuccess"
      :message="resultMsg"
      title="提交结果"
      success-title="提交成功"
      fail-title="提交失败"
      confirm-text="知道了"
      @confirm="resultVisible = false"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { submitApplication } from '../../api/application'
import { getStandardList } from '../../api/house'
import ResultDialog from '../../components/ResultDialog.vue'

const formRef = ref(null)
const standards = ref([])
const submitting = ref(false)
const resultVisible = ref(false)
const resultSuccess = ref(false)
const resultMsg = ref('')

const form = ref({
  applicantName: '',
  department: '',
  applyType: 1,
  title: '',
  familySize: 1,
  reqArea: null,
  oldHouseNo: '',
  oldHouseArea: null
})

const typeLabel = computed(() => {
  const map = { 1: '分房', 2: '调房', 3: '退房' }
  return map[form.value.applyType] || ''
})

const rules = {
  applicantName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  department: [{ required: true, message: '请输入部门', trigger: 'blur' }],
  applyType: [{ required: true, message: '请选择申请类型', trigger: 'change' }],
  title: [{ required: true, message: '请选择职称', trigger: 'change' }],
  familySize: [{ required: true, message: '请输入家庭人口', trigger: 'blur' }],
  reqArea: [{ required: true, message: '请选择期望面积', trigger: 'change' }],
  oldHouseNo: [{ required: true, message: '请输入原房号', trigger: 'blur' }],
  oldHouseArea: [{ required: true, message: '请选择原住房面积', trigger: 'change' }]
}

onMounted(async () => {
  try {
    const res = await getStandardList()
    standards.value = res.data || []
  } catch (e) {}
})

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const payload = {
      applicantName: form.value.applicantName,
      department: form.value.department,
      applyType: form.value.applyType,
      title: form.value.applyType !== 3 ? form.value.title : null,
      familySize: form.value.applyType !== 3 ? form.value.familySize : null,
      reqArea: form.value.applyType !== 3 ? form.value.reqArea : null,
      oldHouseNo: form.value.applyType !== 1 ? form.value.oldHouseNo : null,
      oldHouseArea: form.value.applyType === 2 ? form.value.oldHouseArea : null
    }
    const res = await submitApplication(payload)
    resultSuccess.value = true
    resultMsg.value = res.data || '提交成功！'
  } catch (e) {
    resultSuccess.value = false
    resultMsg.value = e.message || '提交失败'
  } finally {
    submitting.value = false
    resultVisible.value = true
  }
}

function resetForm() {
  form.value = {
    applicantName: '', department: '', applyType: 1,
    title: '', familySize: 1, reqArea: null,
    oldHouseNo: '', oldHouseArea: null
  }
  formRef.value?.resetFields()
}
</script>

<style scoped>
.el-alert { margin-bottom: 4px; }
.score-tip { font-size: 14px; color: #606266; }
.score-tip p { margin: 6px 0; }
</style>
