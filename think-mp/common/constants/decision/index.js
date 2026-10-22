// 决策领域常量

// 冷静期到期 / 报告生成提醒的一次性订阅模板 ID（MP 后台申请后填入）：
// ⚠️ 必须与后端 subscribe_template 表对应行的 template_id 同值——
//    前端 requestSubscribeMessage 授权用的模板 ID 经 /subscribe/sync 同步后，后端才攒得到一次性配额；留空则不弹授权
export const DECISION_REMIND_TMPL_ID = ''
