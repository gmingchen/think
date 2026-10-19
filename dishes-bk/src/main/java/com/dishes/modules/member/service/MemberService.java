package com.dishes.modules.member.service;

import com.dishes.modules.member.model.dto.MemberConfirmDTO;
import com.dishes.modules.member.model.dto.MemberPaySignDTO;
import com.dishes.modules.member.model.dto.MemberPrepayDTO;
import com.dishes.modules.member.model.vo.MemberConfirmVO;
import com.dishes.modules.member.model.vo.MemberInfoVO;
import com.dishes.modules.member.model.vo.MemberOrderVO;
import com.dishes.modules.wechat.xpay.model.vo.XpayPrepayVO;

import java.time.LocalDateTime;

/**
 * 会员（v12）
 * <p>
 * 三类职责：① 页面数据（info）；② 交易链路（createOrder / paySign / confirm）；③ 资金事件落地与权益判定（grant / revoke / 判定三件套）。
 * 判定三件套一律读登录态快照、不查库，所以它们只能在校验「当前请求者」的权益时被调用。
 * </p>
 * <p>
 * ⚠️ 交易链路是<b>两步</b>而不是一步（2026-10-11 拆）：建单只写快照、签名只读快照，
 * 中间不共享任何内存变量——价格一致性靠「签名回读订单行」保证，而不是靠两次调用真得快（§7.2）。
 * </p>
 * <p>
 * ⚠️ 拆两步就多一个失败点：建完单没走到签名（杀进程、断网、看了价又反悔）会留下待支付单。
 * 本版不等对账脚本，而是给每一单一个 15 分钟的 {@code expire_at}：签名路径过期即拒签并关单，
 * {@code #closeExpiredOrders()} 由定时任务兜底扫（§7.2 第二段末）。
 * </p>
 * <p>
 * ⚠️ 接口返回的下单出参是<b>通道结构体</b> {@link XpayPrepayVO}（不是会员专有的 VO）：那五个字段
 * 全是 wx.requestVirtualPayment 的入参，与卖什么无关（§9.1 支付通道解耦）。
 * </p>
 * @author 好市民
 */
public interface MemberService {

    /**
     * 会员状态聚合：状态 + 在售套餐 + 卡片文案 + 开通记录（docs/v12 §7.1）
     *
     * @return 会员信息
     */
    MemberInfoVO info();

    /**
     * 创建订单（docs/v12 §7.2 第一段）：把套餐快照落成一条待支付单并回金额，<b>不碰签名</b>。
     * <p>
     * 2026-10-11 把原来的一步式 {@code prepay} 拆成 createOrder + paySign 两个接口：签名失败（
     * session_key 过期、-15005/-15006）时重签同一单号即可，不再像拆前那样只能重新下单留一堆垃圾单。
     * </p>
     *
     * @param dto 套餐编码
     * @return 单号与本单金额快照（含是否按首单价成立）
     */
    MemberOrderVO createOrder(MemberPrepayDTO dto);

    /**
     * 取支付签名（docs/v12 §7.2 第二段）：只读已落库的那一条订单快照，
     * signData 组装与两把 HMAC 均委给通道侧 {@code XpaySignService}
     * <p>
     * 故意**不**重新查 {@code member_plan}：两步之间运营改价会产生「旧价建单、新价签名」→ -15013；
     * 也不加 {@code @Repeat}：重试签名正是本接口存在的理由。
     * </p>
     *
     * @param dto 业务单号
     * @return 前端透传给 wx.requestVirtualPayment 的出参
     */
    XpayPrepayVO paySign(MemberPaySignDTO dto);

    /**
     * 超时关单（docs/v12 §7.2 第二段末）：把过了 {@code expire_at} 仍未支付的单批量 0 → 3。
     * <p>
     * 供定时任务调用，不接任何对外接口。它与取签名那步的即时拒签是同一个目的两个入口：
     * 只靠签名那步，没人再访问的单会永远挂着 status=0；只靠定时任务，过期到扫描之间那几分钟仍能签出去。
     * </p>
     *
     * @return 本次关掉的行数（调用方只在 &gt; 0 时记日志）
     */
    int closeExpiredOrders();

    /**
     * 会员到期提醒（订阅消息）：给「到期时间正好落在提前 N 天当天」的在期会员各发一条续费提醒。
     * <p>
     * 供 {@code MemberExpireRemindJob} 每天 9:00 调用，不接任何对外接口。每人一年只命中一次这个窗口，
     * 天然不重复，无需去重表。发送走通用订阅消息服务（{@code SubscribeMessageService}），依赖用户此前
     * 授权经 {@code /subscribe/sync} 攒下的本地配额；无配额的人 consume 扣不到、静默跳过（发不出不该带走整轮作业）。
     * </p>
     *
     * @return 本轮成功发出的条数（模板未配置时返回 0）
     */
    int remindExpiringMembers();

    /**
     * 查单兜底 + 主动发放（docs/v12 §7.3）
     *
     * @param dto 业务单号
     * @return 是否已到账
     */
    MemberConfirmVO confirm(MemberConfirmDTO dto);

    /**
     * 发放权益：先查单复核，再 CAS 落订单状态，最后写 member（docs/v12 §7.4 / §7.5）。
     * 复核走通道侧 {@code XpayClient#queryOrder}，拿不到确证就不发（因此实现硬依赖 {@code xpay.app-key}）
     *
     * @param outTradeNo       业务单号
     * @param openid           下单用户 openid（查单必填，订单表未存，由调用方从推送报文或登录快照取）
     * @param notifyActualPrice 通知里的实付金额，可为 null；查单返回的 paid_fee 优先
     * @param transactionId    微信支付单号，可为 null
     * @param mchOrderNo       微信商户单号，可为 null
     * @param paidAt           支付时间，可为 null
     * @return true = 本事件已了结（可回 ErrCode 0，含幂等重推与关单）；false = 尚未确证，请微信重推（回 -1）
     */
    boolean grant(String outTradeNo, String openid, Integer notifyActualPrice,
                  String transactionId, String mchOrderNo, LocalDateTime paidAt);

    /**
     * 回收权益：按该单周期倒扣 expire_at（docs/v12 §7.5 末）
     *
     * @param outTradeNo 业务单号
     * @return true = 已了结；false = 订单还没找到，请重推
     */
    boolean revoke(String outTradeNo);

    /**
     * 是否在生效期：memberTier &gt; 0 且 memberExpireAt 晚于当前时间（docs/v12 §6.3）
     * <p>
     * 快照缺失（内部调用、脚本、未登录）时返回 false，宁可误限不可漏限。
     * </p>
     *
     * @param userId 用户ID，须为当前登录用户，否则一律 false
     * @return 是否有效会员
     */
    boolean isEffective(Long userId);

    /**
     * 是否免广告：一档二档都免
     *
     * @param userId 用户ID
     * @return 是否免广告
     */
    boolean isAdFree(Long userId);

    /**
     * 是否不限分类与菜谱数量：仅二档。调用方还必须自己比较 kitchen.ownerId（docs/v12 §6.2 / §1.4 决策 1）
     *
     * @param userId 用户ID
     * @return 是否不限容量
     */
    boolean hasUnlimitedCapacity(Long userId);
}
