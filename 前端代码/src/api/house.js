import request from './request'

// 获取所有房产
export function getHouseList() {
  return request.get('/house/list')
}

// 获取空闲房产
export function getAvailableHouses() {
  return request.get('/house/available')
}

// 获取指定房号信息
export function getHouseInfo(houseNo) {
  return request.get('/house/info', { params: { houseNo } })
}

// 获取所有住房标准
export function getStandardList() {
  return request.get('/house/standard/list')
}

// 获取指定面积住房标准
export function getStandardInfo(area) {
  return request.get('/house/standard/info', { params: { area } })
}
