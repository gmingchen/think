# image-proxy 云函数（AI 封面生成中转）

## 一、为什么需要它

小程序成长计划二期（2026-07-01~12-31）赠送的 **10 万张图像生成额度** 仅限「小程序 SDK / 云函数」通道消耗，Java 后端直连不可用。因此调用链为：

```
小程序 → Java 后端(/ai/dish/image) → 本云函数(HTTP 访问) → 混元 HY-Image-3.0
```

本函数只做四件事：校验共享密钥 → `ai.createImageModel('hunyuan-image').generateImage(...)` 调混元生图 →
把返回的临时图片 URL 下载转 base64 → 统一以 `{ imageBase64 }` 回传。

> 后端只透传、不落存储；前端拿到 `imageBase64` 后写入临时文件展示，用户确认「使用这张」时才走现有 `uploadMedia` 上传。不确认则 0 副作用、无孤儿图。

## 二、目录

```
image-proxy/
├── index.js       # 函数入口
└── package.json   # 依赖 wx-server-sdk
```

## 三、部署步骤（微信开发者工具）

1. **前置**：小程序已开通云开发并绑定「成长计划」二期赠额；在云开发控制台 → **AI → 生图模型** 中开启 `HY-Image-3.0-Plus-4090-Tob-v1.0`（不开会报 `AI_MODEL_NOT_FOUND`）。
2. 将本 `image-proxy` 文件夹放入 `cloudfunctions/` 目录（与 `llm-proxy` 同级）。
3. 开发者工具中右键该云函数 → **上传并部署：云端安装依赖**。
4. 右键 → **云端测试 / 配置** 设置环境变量：
   - `PROXY_KEY` = **与 `llm-proxy` 使用同一串值**（后端配置里已经写了 `ecfeb810d12512a47d73acc72f181f3223056d1f80a62b17`，可直接复制）。
5. 为该云函数**开启 HTTP 访问 / 云接入**，得到公网访问地址，例如
   `https://ccj-xxx.ap-shanghai.app.tcloudbase.com/image-proxy`。
   - 记录该地址，下一步填进后端配置。

### ⚠️ 两个实测过的坑

| 现象 | 原因 | 处理 |
|---|---|---|
| POST 返回 **404 微信云托管页** | 云函数部署了但 **没建 HTTP 访问路由**（网关层 404，请求根本没进函数） | 控制台→云函数→HTTP 访问/云接入→新建：路径 `/image-proxy`、关联本函数、**鉴权关闭** |
| 返回 500 `model.generateImage is not a function` | 生图与生文入口不同：`createModel('cloudbase')` **只有 `generateText`** | 必须用 `ai.createImageModel('hunyuan-image')`（本函数已改） |

快速自判：同域名下 `/llm-proxy` 返回 401（未带 key）而 `/image-proxy` 返回 404 → 就是路由没建，不是代码问题。

## 四、后端对接

基础 `application.yml` 已内置 `ai.image` 块（model/size/timeout/daily-limit），各环境只需在 `application-{dev,test,prod}.yml` 补上：

```yaml
ai-image:
  url: https://ccj-xxx.ap-shanghai.app.tcloudbase.com/image-proxy
  proxyKey: <与云函数 PROXY_KEY 完全一致>
```

`ImageClient` 会以 `x-proxy-key` 请求头携带该密钥，POST `{ prompt, size, model }`。

## 五、请求 / 响应约定

请求头：`x-proxy-key: <密钥>`，`content-type: application/json`

请求体：
```json
{
  "prompt": "一张【红烧肉】的高清美食摄影图。…（菜名/分类/食材/调料 + 固定风格锚）",
  "size": "1024x1024",
  "model": "HY-Image-3.0-Plus-4090-Tob-v1.0"
}
```

官方硬约束（已写进 `AiImagePrompt` / 本函数默认值）：

| 入参 | 约束 |
|---|---|
| `prompt` | **最多 500 字**，超出会被服务端拒；后端按「菜名+风格锚优先，食材/调料条数 6→0 逐步退让」保证不越上限 |
| `size` | 只认「宽x高」：`1024x1024` / `1280x720` / `720x1280` / `1280x1280`（传 `1024:1024` 会被 `normalizeSize` 自动修正） |
| `model` | 可省，默认 `HY-Image-3.0-Plus-4090-Tob-v1.0`；后端下传覆盖，换型号不必重新部署本函数 |
| `footnote` | 不由外部传入，本函数固定 `AI 生成`（≤16 字）烧入右下角合规水印 |
| `revise` | 默认关：prompt 已含风格锚与禁止项，不让模型改写避免丢约束 |

响应体（后端只取 `imageBase64`）：
```json
{ "imageBase64": "data:image/png;base64,iVBORw0KGgo..." }
```

失败：401 未授权 / 400 缺 prompt / 500 生图异常 / 503 并发超限（重试后仍失败）。

## 六、探针自测（部署后先用 PowerShell 打一次）

官方已文档化的两项结论（无需再验）：调用入口 = `ai.createImageModel('hunyuan-image')`，返回形态 = `res.data[0].url`（**临时图片链接，非 base64**，本函数下载后转 dataURL）。

探针只验一件事：**右下角 `footnote` 水印是否真的烧入**（关系到国标合规能否少做一道标识）。

```powershell
$body = @{ prompt = "一张番茄炒蛋的高清美食摄影图，45°侧拍、自然光、暖色温、原木餐桌背景、超写实家常菜质感，8k"; size = "1024x1024" } | ConvertTo-Json
$r = Invoke-RestMethod -Uri "https://ccj-xxx.tcloudbase.com/image-proxy" -Method Post -Headers @{ "x-proxy-key" = "<PROXY_KEY>" } -ContentType "application/json" -Body $body
# 转存到本地看效果（是否右下角带水印）
$bytes = [Convert]::FromBase64String($r.imageBase64.Split(',')[1])
[IO.File]::WriteAllBytes("$env:TEMP\ai-cover-test.png", $bytes)
Invoke-Item "$env:TEMP\ai-cover-test.png"
```

- 能拉到 `imageBase64` 且图片右下角有「AI 生成」字样 → 合规水印成立，无需前端额外叠加标识。
- 若水印没出现（footnote 未生效），回到后端在预览图上叠加可见角标后再上传，或接受仅弹窗内声明文案。
- 若返回结构不认识，本函数会把原始片段回带到 `error` 字段，改 `index.js:extractImage` 即可。

## 七、常见错误码

| 码 | 含义 | 处理 |
|---|---|---|
| `EXCEED_CONCURRENT_REQUEST_LIMIT` | 全局并发超限 | 本函数已自动 4s 重试 1 次；仍失败返回 503，前端 toast「AI 忙，请稍后重试」 |
| `OperationDenied.TextIllegalDetected` | 输入文本被审核拒 | 返回 500，前端提示「描述包含敏感内容，请调整菜名/食材后重试」 |
| `FailedOperation.GenerateImageFailed` | 生成图未过审 | 返回 500，同上 |
| `AI_MODEL_NOT_FOUND` | 控制台未开启该生图模型，或模型已下线 | 控制台 → AI → 生图模型 开启；或改 `application.yml` 的 `ai.image.model` 换新型号 |
| 赠额耗尽 | 10 万张用完 | 云开发套餐不可抵扣生图，只能切外部厂商（本版不预设降级） |

## 八、合规提醒

- 2025-09-01 施行的《GB 45438-2025》要求 AI 生成图片显式标识。云开发这一层**不暴露 `LogoAdd` 开关**，改为传 `footnote`（≤16 字）在右下角烧入水印，本函数固定为 `AI 生成`。
- 不要为了图片“干净”去掉 footnote；前端预览与最终上传的都是带水印的同一张图，无需额外处理。
- 使用深度合成能力需在 CloudBase 控制台获取「算法合作协议」（同 v7 AI 荐菜，已在备案）。
- 前端 add-edit 页封面区可展示一行小字提示「AI 生成内容仅供参考」（本版不强制，水印已足够显式）。
