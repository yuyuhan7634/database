import request from './request'

// 获取所有住户
export function getResidentList() {
  return request.get('/resident/list')
}

// 获取所有房租账单
export function getBillList() {
  return request.get('/resident/bill/list')
}

// 生成指定月份的账单
export function generateBills(month) {
  return request.post('/resident/bill/generate', null, { params: { month } })
}

// 更新住户个人信息
export function updateResidentInfo(ownerName, department, title, familySize) {
  return request.post('/resident/updateInfo', null, {
    params: { ownerName, department, title, familySize }
  })
}
