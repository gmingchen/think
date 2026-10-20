// 会员（v12）：这里只放「与钱无关」的展示常量。
// ⚠️ 价格、档位名、卡片文案一律由 /member/info 下发（§7.1「前端不硬编码价格」），
//    所以放开付费（member.enabled 置 true）只改后端配置、不发版；本文件出现任何价格数字都是写错了。

// 会员中心页路径：会员卡、4005 引导行、宫格入口都跳这里
export const VIP_PAGE = '/subpackages/member/pages/vip/index'

// 会员到期提醒的订阅消息模板 ID（MP 后台申请后填入）：
// ⚠️ 必须与后端 subscribe_template 表 MEMBER_EXPIRE 行的 template_id 同值——
//    前端 requestSubscribeMessage 授权用的模板 ID 经 /subscribe/sync 同步后，后端才 攒得到一次性配额；留空则不弹授权
export const MEMBER_EXPIRE_TMPL_ID = 'Ygt4aV5WN7l4Tqig-msU7uIfSRPkpBCXpHp33IQNYKQ'

// 档位名：权益对比表列头、头像 👑 角标用（§3.1 定稿「轻享会员 / 畅厨会员」）
// 0 是「非会员」的出口值（后端已按 expireAt 归零），留空串让前端 wx:if 直接跳过渲染
export const MEMBER_TIER_TEXT = {
  0: '',
  1: '轻享会员',
  2: '畅厨会员',
}

// 权益对比表的列头用短名（三列窄格放不下「会员」两字，§5.1）；与上一表同源，不改一起改
export const MEMBER_TIER_SHORT_TEXT = {
  1: '轻享',
  2: '畅厨',
}

// 周期文案由 months 推导（§3.2：代码里不得有 if (月/季/年) 分支，后端同理）：
// 加半年付、两年付只在表里补一行 member_plan，前端只在这里补一个 months 键，拿不到就回退到套餐名
export const MEMBER_PERIOD_TEXT = {
  1: '月付',
  3: '季付',
  6: '半年付',
  12: '年付',
}

// 开通记录行只下发 planCode（§7.1 MemberRecordVO），而历史 SKU 可能已从 member_plan 下架，
// 不能靠 plans 列表反查。这两个映射读的是 code 的**结构**（`lite_month` / `pro_year`），
// §17 第 3 条已约束「tier 数字与 code 前缀不随文案改」，所以它不会与价格表漂移
export const MEMBER_PLAN_TIER = { lite: 1, pro: 2 }
export const MEMBER_PLAN_SUFFIX_TEXT = { month: '月卡', half_year: '半年卡', quarter: '季卡', year: '年卡' }

// 临期阈值：只把「剩余 N 天」转成 --color-warning，不加横幅（§1.3 否决项、§4.1 口径 3）
export const MEMBER_EXPIRE_WARN_DAYS = 7

// confirm 轮询：支付 success 后先调 1 次，未发放再按这个节奏重试（§7.3 前端策略）
// 2s × 5 次 = 10s，跑满仍没结果就交给发货通知兜底，不再让用户干等
export const MEMBER_QUERY_RETRY = { interval: 2000, max: 5 }

// 权益对比表（§1.2 定稿，三列 = 免费 / 轻享 / 畅厨）
// ⚠️ AI 行已接后端分层：畅厨 20/30/10、免费与轻享 3/5/2（轻享不加量，§1.2 / §6.5）。
//    本行数字是展示常量、与后端 ai.llm/image 的 daily-limit 与 pro-*-limit 同源——改后端额度必须同步改这里（与「10 个 / 50 道」容量行同样的人工对齐义务）。
// ⚠️ 「不限」只作用于本人创建的厨房（§1.4 决策 1），这句话由 §5.4 购买须知第一条承担，
//    表格里不重复写限定语（三列窄格放不下，且读者已在同一页读过条款）。
// ⚠️ 轻享那一列容量仍是 10/50：它免的是「展示型广告位」，不免「看视频换容量」这笔交易（§6.1 三分法）。
export const MEMBER_BENEFITS = [
  { label: '页面 Banner 广告', free: '展示', lite: '隐藏', pro: '隐藏' },
  { label: '插屏广告', free: '展示', lite: '隐藏', pro: '隐藏' },
  { label: '菜谱分类上限', free: '10 个', lite: '10 个', pro: '不限' },
  { label: '菜谱数量上限', free: '50 道', lite: '50 道', pro: '不限' },
  { label: 'AI 荐菜/识别/生图 日限', free: '3/5/2', lite: '3/5/2', pro: '20/30/10' },
  { label: '👑 会员标识', free: '—', lite: '有', pro: '有' },
  { label: '📺 看视频 +1 扩容', free: '可用', lite: '可用', pro: '无需' },
]

// 开通记录行的状态文案（MemberRecordVO.status，取 MemberOrderEntity 的四个状态值）
// 只可能下发 1/2/3：待支付（0）不进「最近 5 条已发放/已退款订单」这个列表
export const MEMBER_RECORD_STATUS_TEXT = {
  1: '已到账',
  2: '已退款',
  3: '已关闭',
}
