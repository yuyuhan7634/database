import request from './request'

// 获取所有申请单
export function getApplicationList() {
  return request.get('/application/list')
}

// 获取分房排队队列
export function getAllocationQueue() {
  return request.get('/application/queue')
}

// 提交申请
export function submitApplication(data) {
  return request.post('/application/submit', data)
}

// 审批分房
export function approveHousing(applyNo) {
  return request.post('/application/approve', null, { params: { applyNo } })
}

// 审批退房
export function approveCheckout(applyNo) {
  return request.post('/application/checkout', null, { params: { applyNo } })
}

// 审批调房
export function approveTransfer(applyNo) {
  return request.post('/application/transfer', null, { params: { applyNo } })
}

// 驳回申请
export function rejectApplication(applyNo, reason) {
  return request.post('/application/reject', null, { params: { applyNo, reason } })
}

// 撤回申请
export function cancelApplication(applyNo, applicantName) {
  return request.post('/application/cancel', null, { params: { applyNo, applicantName } })
}

// 手动触发月底批量分房
export function manualMonthlyAllocate() {
  return request.post('/application/allocate/monthly')
}
