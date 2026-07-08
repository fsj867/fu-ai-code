import request from '@/utils/request'

export function login(data) {
  return request({
    url: '/user/login',
    method: 'post',
    data
  })
}

export function register(data) {
  return request({
    url: '/user/register',
    method: 'post',
    data
  })
}

export function getLoginUser() {
  return request({
    url: '/user/get/login',
    method: 'get'
  })
}

export function logout() {
  return request({
    url: '/user/logout',
    method: 'post'
  })
}

export function addUser(data) {
  return request({
    url: '/user/add',
    method: 'post',
    data
  })
}

export function getUserById(id) {
  return request({
    url: '/user/get',
    method: 'get',
    params: { id }
  })
}

export function deleteUser(data) {
  return request({
    url: '/user/delete',
    method: 'post',
    data
  })
}

export function updateUser(data) {
  return request({
    url: '/user/update',
    method: 'post',
    data
  })
}

export function listUserVOByPage(data) {
  return request({
    url: '/user/list/page/vo',
    method: 'post',
    data
  })
}
