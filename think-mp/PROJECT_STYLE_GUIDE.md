## 项目代码风格与约定速查表

> 本文总结 `lucky-cat-user` 小程序的代码风格、工具函数与通用样式约定，供 AI 生成代码时参考。

---

## 一、整体架构

### 入口 `app.js`
- 启动时执行：`getSystemInfo`、`getLocationSetting`、`getLocation`、`login`
- 全局挂载到 `wx`：
  - `wx.$navigateTo` / `wx.$navigateBack` / `wx.$navigateBackTo`
  - `wx.$toast`
  - `wx.$printLog` / `wx.$printDebug` / `wx.$printWarn` / `wx.$printError`
  - `wx.$onLogined` / `wx.$onLocationSetting` / `wx.$onLocation`（Promise 阻塞信号）

### MobX Store（`store/`）
- 根 store：`export const store = observable({ ...system, ...auth, ...user, ...tabbar, ...location })`
- 模块结构：`{ xxx: { ...state }, action(function xxxAction() { ... }) }`
- 页面绑定：使用 `storePageBehavior`，调用 `this.bindStore({ fields: ['system'] })`
- 组件绑定：使用 `storeComponentBehavior(options)`

---

## 二、工具函数（`utils/`）

### 统一入口
- 所有工具从 `utils/index.js` 聚合导出
- 业务代码统一引入：`import { login, toast, getLocation } from '../../utils/index'`

### 核心工具
- **`navigateTo(target)`**：自动判断 tabbar 或普通页面跳转
- **`navigateBack()`**：有页面栈则返回，否则回首页 tabbar
- **`navigateBackTo(target)`**：返回目标页，智能处理重复入栈
- **`login()`**：Promise 化的 `wx.login`
- **`getSetting(key?)`**：获取授权设置，支持 `userLocation` 等 key
- **`getLocation(options?)`**：默认 `type: 'wgs84'`
- **`toast(title)`**：统一轻提示
- **`printLog/Debug/Warn/Error(title, ...args)`**：统一日志，格式：`时间 + route + 标题 + 数据`
- **`getValueByPath(path, json)`**：通过路径获取值，支持数组索引
- **`parseQuery(query)`**：解析页面参数，支持 scene 解码
- **`calcDistance(lng, lat, targetLng, targetLat)`**：经纬度计算距离
- **`encryptMobile(mobile)`**：手机号脱敏

---

## 三、API 封装（`apis/`）

### 统一请求 `apis/service.js`
- 自动注入 token：`store.auth.token`
- 根据环境选择 host：`HOST_ENUM[store.system.envVersion]`
- 统一错误处理：`isError` 时 toast 提示并 reject
- 超时：默认 50000ms

### API 定义
```js
export const xxxApi = params => {
  return service({
    url: '/xxx/xxx',
    method: 'POST',
    params: params
  })
}
```

---

## 四、Behavior（`behaviors/`）

### `storePageBehavior`
- 页面使用：`behaviors: [storePageBehavior]`
- 绑定：`this.bindStore({ fields: ['system'] })`
- 自动销毁：`onUnload` 时清理

### `storeComponentBehavior(options)`
- 组件使用：`behaviors: [storeComponentBehavior({ fields: ['system'] })]`

### `fileBehavior`
- 文件上传/删除处理
- `onFileComplete(e)`：通过 `data-field` 自动更新 data
- `onFileDelete(e)`：通过 `data-field` 自动删除数组项

### `fieldBehavior`
- 字段变更处理：`onFieldChange(e)`，通过 `data-field` 自动更新

### `paginationBehavior(fn)`
- 分页数据管理：`list`、`page: { current, size, total }`
- 方法：`getPageData()`、`loadPageData()`、`reloadPageData()`

---

## 五、组件规范

### `container` 组件
- 页面容器，支持自定义导航栏、滚动、footer
- Props：`title`、`back`、`customBack`、`loading`、`finished`、`scrollTop`、`useNavbarLeft/Middle/Right`、`useScroll`、`useFooter`
- Slots：`navbar-left`、`navbar-middle`、`navbar-right`、`header`、`footer`

### 全局组件（`app.json`）
- `container`、`iconfont`、`navbar`

---

## 六、样式体系（`common/style/`）

### 引入方式
```wxss
@import '../../common/style/index.wxss';
```

### CSS 变量（`app-variable.wxss`）
- 颜色：`--color-primary`、`--color-success`、`--color-danger`、`--color-warning`
- 字体：`--font-color-primary`、`--font-size`、`--font-size-small/large`、`--font-weight-5/6/bold`
- 边框：`--border`、`--border-radius`、`--border-radius-small/large`
- TabBar：`--tabbar-height: 88rpx`

### 工具类（`app-common.wxss`）
- 尺寸：`.w-f`、`.h-f`、`.w-f_i`、`.h-f_i`
- 间距：`.m-8/12/16/20/24`、`.m_v-8`、`.m_h-8`、`.m_t/r/b/l-8`、`.p-8` 等（对称）
- 文字：`.t_a-l/c/r`、`.f_w-4/5/6/b`、`.e`（单行省略）、`.e-2`（两行省略）
- 边框：`.b_t/r/b/l`
- Flex：`.f`、`.f_d-c`、`.f_j_c-c/sb/sa/fe`、`.f_a_i-c/b/fe`、`.f-i_f-1`、`.f-i_fs-0`、`.f-center`

**约定：优先使用工具类，只在无法表达时写局部样式**

---

## 七、常量与枚举（`common/`）

### 常量（`common/constants/`）
- `AUTH_KEY: 'token'`

### 枚举（`common/enums/`）
- `TABBAR_ENUM`、`TABBAR_LIST`：TabBar 配置
- `ENV_VERSION_ENUM`、`ENV_ENUM`、`HOST_ENUM`：环境配置
- `METHOD_ENUM`、`CONTENT_TYPE_ENUM`：请求相关
- `QUERY_KEY_ENUM`：页面参数 key

---

## 八、页面开发规范

### 基础结构
```js
import { storePageBehavior, fileBehavior } from '../../behaviors/index'

Page({
  behaviors: [storePageBehavior, fileBehavior],
  data: { },
  async onLoad(options) {
    await wx.$onLogined  // 等待登录完成
    this.bindStore({ fields: ['system'] })
  }
})
```

### 样式
```wxss
@import '../../common/style/index.wxss';
```

### 模板
```xml
<container title="页面标题" back>
  <!-- 内容 -->
</container>
```

---

## 九、新增模块规范

### 新增页面
1. 创建 `pages/xxx/index.{js,wxml,wxss,json}`
2. 在 `app.json` 的 `pages` 中注册
3. 使用 `storePageBehavior` 绑定 store
4. 引入 `common/style/index.wxss`

### 新增 store 模块
1. 创建 `store/modules/xxx/index.js`，导出 `{ xxx: {...}, action(function xxxAction() {...}) }`
2. 在 `store/index.js` 中 `...xxx` 混入

### 新增工具函数
1. 在 `utils/xxx.js` 中实现
2. 在 `utils/index.js` 中 `export * from './xxx'`

### 新增 API
1. 在 `apis/xxx/index.js` 中定义
2. 在 `apis/index.js` 中 `export * from './xxx'`

### 新增组件
1. 创建 `components/xxx/index.{js,wxml,wxss,json}`
2. 如需全局使用，在 `app.json` 的 `usingComponents` 中注册

---

## 十、关键约定

1. **所有微信 API 使用 `utils/wx-api` 中的 Promise 版本**
2. **所有导航使用 `utils/navigate` 中的方法**
3. **所有 API 调用使用 `apis/service` 封装**
4. **样式优先使用工具类和 CSS 变量**
5. **页面必须等待 `wx.$onLogined` 后再操作**
6. **文件上传/删除使用 `fileBehavior` 自动处理**
7. **分页使用 `paginationBehavior` 统一管理**
