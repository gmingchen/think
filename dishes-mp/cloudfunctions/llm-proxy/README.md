# llm-proxy 云函数（AI 荐菜 LLM 中转）

## 一、为什么需要它

小程序成长计划赠送的 10 亿 token AI 额度**仅限「小程序 SDK / 云函数」通道消耗，Java 后端直连不可用**。
因此调用链为：

```
小程序 → Java 后端(/ai/dish/recommend) → 本云函数(HTTP 访问) → 混元 hy3
```

本函数只做三件事：校验共享密钥 → 用 `wx-server-sdk` 调 `hy3` → 把结果转成 OpenAI chat/completions 结构回传。

## 二、目录

```
llm-proxy/
├── index.js       # 函数入口
└── package.json   # 依赖 wx-server-sdk
```

## 三、部署步骤（微信开发者工具）

1. **前置**：小程序已开通「云开发」并创建环境；在云开发控制台「AI 能力 / 模型开关」中启用混元 `hy3`（成长计划额度到账后）。
2. 将本 `llm-proxy` 文件夹放入小程序云函数目录（`project.config.json` 的 `cloudfunctionRoot` 指向的目录，如 `cloudfunctions/`）。
3. 开发者工具中右键该云函数 → **上传并部署：云端安装依赖**。
4. 右键 → **云端测试 / 配置** 设置环境变量：
   - `PROXY_KEY` = 一串随机长字符串（务必复杂，如 32 位以上随机串）。
5. 为该云函数**开启 HTTP 访问 / 云接入**，得到公网访问地址，例如
   `https://xxxx.ap-shanghai.run.tcloudbase.com/llm-proxy`。
   - 若走「云接入」自定义路径，路径需与后端配置的 url 一致。
   - 记录该地址，下一步填进后端配置。

## 四、后端对接

把 HTTP 访问地址与共享密钥回填到服务器侧 `application-prod.yml`（不进 Git）：

```yaml
ai-llm:
  url: https://xxxx.ap-shanghai.run.tcloudbase.com/llm-proxy
  proxyKey: <与云函数 PROXY_KEY 完全一致>
```

后端 `LlmClient` 会以 `x-proxy-key` 请求头携带该密钥，POST OpenAI 兼容体 `{model, messages, temperature, stream}`。

## 五、请求 / 响应约定

请求头：`x-proxy-key: <密钥>`，`content-type: application/json`

请求体：
```json
{
  "model": "hy3",
  "messages": [ { "role": "system", "content": "..." }, { "role": "user", "content": "..." } ],
  "temperature": 0.7
}
```

响应体（OpenAI 兼容，后端只取 `choices[0].message.content`）：
```json
{ "choices": [ { "message": { "role": "assistant", "content": "..." } } ], "usage": { } }
```

失败：401 未授权 / 400 缺 messages / 500 调用异常。

## 六、联调自测（部署后可用 PowerShell 直接打）

```powershell
$body = @{ model="hy3"; messages=@(@{role="user"; content="只输出 JSON：{\""ok\"":1}"}) } | ConvertTo-Json -Depth 5
Invoke-RestMethod -Uri "https://xxxx.run.tcloudbase.com/llm-proxy" -Method Post -Headers @{ "x-proxy-key"="<PROXY_KEY>" } -ContentType "application/json" -Body $body
```

返回里能看到 `choices[0].message.content` 即打通。

## 七、合规提醒

- 使用深度合成（AI 生成内容）能力，上线提审需在 CloudBase 控制台获取「算法合作协议」（混元备案主体为腾讯），建议 9 月内先拿到备用。
- 荐菜结果页需展示「内容由 AI 生成，仅供参考」声明。
