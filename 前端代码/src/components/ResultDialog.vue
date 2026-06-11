<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    :title="title"
    width="480px"
  >
    <el-result
      :icon="success ? 'success' : 'error'"
      :title="success ? successTitle : failTitle"
    >
      <template #extra>
        <div class="result-msg">{{ message }}</div>
        <el-button type="primary" @click="handleConfirm" style="margin-top:12px">
          {{ confirmText }}
        </el-button>
      </template>
    </el-result>
  </el-dialog>
</template>

<script setup>
defineProps({
  modelValue: { type: Boolean, default: false },
  success: { type: Boolean, default: true },
  message: { type: String, default: '' },
  title: { type: String, default: '操作结果' },
  successTitle: { type: String, default: '操作成功' },
  failTitle: { type: String, default: '操作失败' },
  confirmText: { type: String, default: '确定' }
})
const emit = defineEmits(['update:modelValue', 'confirm'])
function handleConfirm() {
  emit('confirm')
}
</script>
<style scoped>
.result-msg {
  white-space: pre-wrap;
  text-align: left;
  background: #f5f7fa;
  padding: 12px;
  border-radius: 4px;
}
</style>
