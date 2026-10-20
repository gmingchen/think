import { service } from '../service'

export const dishListApi = params => {
  return service({
    url: '/dish/list',
    method: 'GET',
    params: params
  })
}

// 查询指定分类下的菜谱列表（按 sort 升序）；不传 categoryId 则查“未分类”组
export const dishListByCategoryApi = params => {
  return service({
    url: '/dish/list/category',
    method: 'GET',
    params
  })
}

// 分类内菜谱拖拽排序：{ kitchenId, categoryId, ids }，ids 为新展示顺序（categoryId 为空排“未分类”组）
export const dishSortApi = params => {
  return service({
    url: '/dish/sort',
    method: 'POST',
    params
  })
}

export const dishBestApi = params => {
  return service({
    url: '/dish/best',
    method: 'GET',
    params: params
  })
}

export const dishInfoApi = params => {
  return service({
    url: '/dish/info',
    method: 'GET',
    params
  })
}

export const dishCreateApi = params => {
  return service({
    url: '/dish/create',
    method: 'POST',
    params
  })
}

export const dishUpdateApi = params => {
  return service({
    url: '/dish/update',
    method: 'POST',
    params
  })
}

export const dishDeleteApi = params => {
  return service({
    url: '/dish/delete/batch',
    method: 'DELETE',
    params
  })
}

export const dishBatchUpdateCategoryApi = params => {
  return service({
    url: '/dish/update/category/batch',
    method: 'POST',
    params
  })
}

export const dishShareInfoApi = params => {
  return service({
    url: '/dish/share/info',
    method: 'GET',
    params
  })
}

export const dishCollectApi = params => {
  return service({
    url: '/dish/collect',
    method: 'POST',
    params
  })
}

// 采购清单：批量查多道菜的食材/调料明细（聚合归前端）
export const dishShoppingListApi = params => {
  return service({
    url: '/dish/shopping/list',
    method: 'POST',
    params
  })
}
