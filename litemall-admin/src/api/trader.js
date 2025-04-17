import request from '@/utils/request'

export function listTrader(query) {
  return request({
    url: '/trader/list',
    method: 'get',
    params: query
  })
}

export function createTrader(data) {
  return request({
    url: '/trader/create',
    method: 'post',
    data
  })
}

export function readTrader(data) {
  return request({
    url: '/trader/read',
    method: 'get',
    data
  })
}

export function updateTrader(data) {
  return request({
    url: '/trader/update',
    method: 'post',
    data
  })
}

export function deleteTrader(data) {
  return request({
    url: '/trader/delete',
    method: 'post',
    data
  })
}

export function traderOptions(query) {
  return request({
    url: '/trader/options',
    method: 'get',
    params: query
  })
}
