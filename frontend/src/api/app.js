import request from '@/utils/request'

export function addApp(data) {
  return request({
    url: '/app/add',
    method: 'post',
    data
  })
}

export function updateApp(data) {
  return request({
    url: '/app/update',
    method: 'post',
    data
  })
}

export function deleteApp(data) {
  return request({
    url: '/app/delete',
    method: 'post',
    data
  })
}

export function getAppDetail(id) {
  return request({
    url: '/app/get',
    method: 'get',
    params: { id }
  })
}

export function listMyApps(params) {
  return request({
    url: '/app/list/my',
    method: 'get',
    params
  })
}

export function listFeaturedApps(params) {
  return request({
    url: '/app/list/featured',
    method: 'get',
    params
  })
}

export function adminDeleteApp(data) {
  return request({
    url: '/app/admin/delete',
    method: 'post',
    data
  })
}

export function adminUpdateApp(data) {
  return request({
    url: '/app/admin/update',
    method: 'post',
    data
  })
}

export function adminListApps(data) {
  return request({
    url: '/app/admin/list/page',
    method: 'post',
    data
  })
}

export function adminGetAppDetail(id) {
  return request({
    url: '/app/admin/get',
    method: 'get',
    params: { id }
  })
}

export function listGoodAppVOByPage(data) {
  return request({
    url: '/app/good/list/page/vo',
    method: 'post',
    data
  })
}

export function deployApp(data) {
  return request({
    url: '/app/deploy',
    method: 'post',
    data
  })
}

export function downloadAppCode(appId) {
  return `/api/app/download/${appId}`
}

export function genCodeEventStreamUrl(appId, message) {
  return `/api/app/chat/event/code?appId=${appId}&message=${encodeURIComponent(message)}`
}
