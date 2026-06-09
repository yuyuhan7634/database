import request from './request'

// 获取综合统计信息
export function getStatistics() {
  return request.get('/admin/statistics')
}

// 获取阈值分数（同 getStandardList）
export function getThresholds() {
  return request.get('/admin/threshold')
}

// 查询房源详情
export function getAdminHouseInfo(houseNo) {
  return request.get('/admin/house-info', { params: { houseNo } })
}

// 按面积范围查询住房条件
export function getHousingConditions(minArea, maxArea) {
  return request.get('/admin/housing-conditions', {
    params: { minArea, maxArea }
  })
}

// 修改住房标准
export function updateStandard(area, minScore) {
  return request.post('/admin/updateStandard', null, {
    params: { area, minScore }
  })
}

// 调整房屋租金
export function updateHouseRent(houseNo, newRent) {
  return request.post('/admin/updateRent', null, {
    params: { houseNo, newRent }
  })
}
export function addHouse(houseNo, area, rentPerSqm) {
  return request.post('/admin/addHouse', null, {
    params: { houseNo, area, rentPerSqm }
  })
}
export function updateHouse(houseNo, area, rentPerSqm, status) {
  return request.post('/admin/updateHouse', null, {
    params: { houseNo, area, rentPerSqm, status }
  })
}
export function deleteHouse(houseNo) {
  return request.post('/admin/deleteHouse', null, {
    params: { houseNo }
  })
}
