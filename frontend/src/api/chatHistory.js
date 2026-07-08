import request from '@/utils/request'

export function saveUserMessage(params) {
  return request({
    url: '/chatHistory/user/save',
    method: 'post',
    params
  })
}

export function saveAiMessage(params) {
  return request({
    url: '/chatHistory/ai/save',
    method: 'post',
    params
  })
}

export function saveErrorMessage(params) {
  return request({
    url: '/chatHistory/error/save',
    method: 'post',
    params
  })
}

export function listByAppId(appId, params) {
  return request({
    url: `/chatHistory/app/${appId}`,
    method: 'get',
    params
  })
}

export function deleteByAppId(appId) {
  return request({
    url: `/chatHistory/app/${appId}`,
    method: 'delete'
  })
}

export function countByAppId(appId) {
  return request({
    url: `/chatHistory/app/${appId}/count`,
    method: 'get'
  })
}

export function listAllForAdmin(params) {
  return request({
    url: '/chatHistory/admin/list',
    method: 'get',
    params
  })
}

export function countAllForAdmin() {
  return request({
    url: '/chatHistory/admin/count',
    method: 'get'
  })
}
