package com.dishes.modules.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dishes.common.constant.RedisConstant;
import com.dishes.common.enums.ResultCodeEnum;
import com.dishes.common.enums.SubscribeTypeEnum;
import com.dishes.core.redis.utils.RedisUtils;
import com.dishes.core.security.utils.SecurityUtils;
import com.dishes.exception.RunException;
import com.dishes.modules.auth.model.LoginUser;
import com.dishes.modules.auth.model.res.LoginResVO;
import com.dishes.modules.member.config.MemberProperties;
import com.dishes.modules.member.convert.MemberConvert;
import com.dishes.modules.member.entity.MemberEntity;
import com.dishes.modules.member.entity.MemberOrderEntity;
import com.dishes.modules.member.entity.MemberPlanEntity;
import com.dishes.modules.member.mapper.MemberMapper;
import com.dishes.modules.member.mapper.MemberOrderMapper;
import com.dishes.modules.member.mapper.MemberPlanMapper;
import com.dishes.modules.member.model.dto.MemberConfirmDTO;
import com.dishes.modules.member.model.dto.MemberPaySignDTO;
import com.dishes.modules.member.model.dto.MemberPrepayDTO;
import com.dishes.modules.member.model.vo.MemberConfirmVO;
import com.dishes.modules.member.model.vo.MemberExpireNoticeVO;
import com.dishes.modules.member.model.vo.MemberInfoVO;
import com.dishes.modules.member.model.vo.MemberMineCardVO;
import com.dishes.modules.member.model.vo.MemberOrderVO;
import com.dishes.modules.member.model.vo.MemberPlanVO;
import com.dishes.modules.member.model.vo.MemberRecordVO;
import com.dishes.modules.member.service.MemberService;
import com.dishes.modules.subscribe.service.SubscribeMessageService;
import com.dishes.modules.wechat.xpay.client.XpayClient;
import com.dishes.modules.wechat.xpay.model.vo.XpayPrepayVO;
import com.dishes.modules.wechat.xpay.service.XpaySignService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 会员（v12）
 * <p>
 * 三条链路都在这里：页面数据（info）、交易（createOrder / paySign / confirm）、资金事件落地与权益判定（grant / revoke / 判定三件套）。
 * </p>
 * <p>
 * ⚠️ 交易是<b>两步</b>（2026-10-11 把一步式的 prepay 拆开）：建单只写快照不碰签名，签名只读快照不查价目表，
 * 两个方法之间不共享任何内存变量。这么拆的理由在 §7.2：签名失败时重签同一单号就够，
 * 不该像拆前那样只能重新下单、每重试一次多留一条待支付单。
 * </p>
 * <p>
 * ⚠️ 拆开的代价（建完单没走到签名）同日已定：每单建单时写 15 分钟的 {@code expire_at}，
 * 签名路径过期即拒签并关单，还有 {@code MemberOrderExpireJob} 每 5 分钟兜底扫（§7.2 第二段末）。
 * 而本地关单挡不住用户照旧付钱，所以发放那一边对「超时关掉」这一类单留出补发放入口（§7.4）。
 * </p>
 * <p>
 * ⚠️ 发放口径已定为「查单复核后再发放」（docs/v12 §7.4，2026-10-11 拍板）：微信推送只当触发器，
 * 发放前必须调服务端 {@code /xpay/query_order} 拿到「这笔单真付了钱」的确证。因此
 * {@code application-prod.yml} 的 {@code xpay.app-key} 必须填现网 AppKey——缺它不是降级，是发不了货。
 * </p>
 * <p>
 * 分层（2026-10-11 支付通道解耦）：本类只管「卖什么、给谁发什么权益」；「怎么收钱」的三件事
 * ——signData 组装与双签名、查单、凭证——全部在 {@code modules/wechat/xpay}：
 * {@link XpaySignService} 出单号与签名，{@link XpayClient} 回事实，通道配置存凭证。
 * 而订单状态机、金额校验基准（expected_price）、SKU 表与权益折算留在会员这边。
 * </p>
 * <p>
 * ⚠️ 本类比常规 service 多一处取舍：{@link #doGrant} 把一次外网查单包进了事务（{@code @Transactional}），
 * 目的是让「订单置为已发放」与「写 member」要么一起成功、要么一起回滚。
 * 若没有这个事务，两步之间进程挂掉就会留下 status=1 但权益没落库的死单——重推时 CAS 抢不到，权益永久丢失。
 * 代价是一条最长 5s 的外网调用占着数据库连接，而这条链路的量级是「有人点开通」，可以接受。
 * </p>
 *
 * @author 好市民
 */
@Slf4j
@Service("memberService")
public class MemberServiceImpl implements MemberService {

    /**
     * 唯一带首单钩子的 SKU（docs/v12 §3.2），其余套餐即使填了 first_price 也不生效
     */
    private static final String PLAN_PRO_MONTH = "pro_month";
    /**
     * 业务单号前缀：mv = member virtual。生成规则在通道侧（{@link XpaySignService#nextOutTradeNo}），
     * 前缀由业务自带，对账时能一眼分出钱来自哪条产品线（§7.2 要点 1）
     */
    private static final String OUT_TRADE_NO_PREFIX = "mv";
    /**
     * 微信 paid_time 是 unix 秒（UTC），本库时间口径是 JVM 的 CST，故固定 +8 还原本地时间（§8.1 时间单源）
     */
    private static final ZoneOffset BEIJING_OFFSET = ZoneOffset.ofHours(8);
    /**
     * 一天的秒数：向上取整算「剩余天数 D」，避免 Duration.toDays() 的截断把 15.2 天算成 15 天
     */
    private static final int SECONDS_PER_DAY = 86400;
    /**
     * 折算时的「一月按 30 天」口径（§7.5 日均价公式），与 plusMonths 的真实天数不同源，是刻意取的近似值
     */
    private static final int DAYS_PER_MONTH = 30;
    /**
     * 开通记录条数上限（§7.1）
     */
    private static final int RECORD_LIMIT = 5;
    /**
     * 待支付单的存活分钟数：建单时算成 {@code expire_at} 落库，不在读时心算（§7.2）。
     * 15 分钟是微信侧拉收银台到扣款的量级上限，再短会把正常付钱的单误关（本侧关了但钱还能进来）
     */
    private static final int ORDER_EXPIRE_MINUTES = 15;
    /**
     * 到期提醒提前天数：到期时间落在「今天 + 3 天」当天的人，早上 9 点各发一条（用户口径）
     */
    private static final int EXPIRE_REMIND_AHEAD_DAYS = 3;
    /**
     * 订阅消息点击落地页（微信要求不带前导斜杠）：跳会员中心页续费
     */
    private static final String EXPIRE_NOTICE_PAGE = "subpackages/member/pages/vip/index";
    /**
     * 到期时间字段格式：微信 date 类型字段接受 yyyy-MM-dd
     */
    private static final DateTimeFormatter EXPIRE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    // ⚠️ 下面三个字段 key 必须与 MP 后台「会员到期提醒」模板内容的字段 key 逐字一致（形如 thing1 / date2 / number3）。
    //    对不上微信回 47003（参数与模板不匹配）。拿到模板后只改这三行，其余逻辑不动。
    private static final String FIELD_MEMBER_NAME = "thing1";
    private static final String FIELD_EXPIRE_DATE = "date2";
    private static final String FIELD_REMAIN_DAYS = "number6";

    @Autowired
    private MemberPlanMapper memberPlanMapper;
    @Autowired
    private MemberOrderMapper memberOrderMapper;
    @Autowired
    private MemberMapper memberMapper;
    @Autowired
    private MemberProperties memberProperties;
    /**
     * signData 组装、双签名、单号生成、凭证闸门都在通道侧
     */
    @Autowired
    private XpaySignService xpaySignService;
    @Autowired
    private XpayClient xpayClient;
    @Autowired
    private RedisUtils redisUtils;
    /**
     * 订阅消息通用发送（v9 模块）：按 type 找模板 → 扣本地配额 → 发微信 → 记流水，全在它里面；
     * 本类只负责取到人、组好 data、调一次，不再各自持有模板 ID 与微信发送通道
     */
    @Autowired
    private SubscribeMessageService subscribeMessageService;

    @Override
    public MemberInfoVO info() {
        Long userId = currentUserId();
        LocalDateTime now = LocalDateTime.now();
        MemberEntity row = selectMember(userId);
        // ⚠️ tier 必须先按 expire_at 归零再透出：库里那一列的语义是「最近一次发放的档位」，
        // 不归零就会给出 tier=2 / effective=false 这种自相矛盾的入参，过期会员还会永久免广告（§8.1）
        int tier = effectiveTier(row, now);
        boolean effective = tier > 0;
        LocalDateTime expireAt = row != null ? row.getExpireAt() : null;
        int remainDays = effective ? ceilDays(now, expireAt) : 0;
        List<MemberPlanVO> plans = listPlans();
        return new MemberInfoVO()
                // 开关原值透出：会员中心页要靠它把 CTA 置灰（§5.2）。前端不许拿 tone / action 文案反推
                .setEnabled(Boolean.TRUE.equals(memberProperties.getEnabled()))
                .setTier(tier)
                .setEffective(effective)
                .setExpireAt(expireAt)
                .setRemainDays(remainDays)
                .setAdFree(effective)
                .setUnlimitedCapacity(effective && tier >= MemberPlanEntity.TIER_PRO)
                .setFirstOrder(isProFirstOrder(userId))
                .setPlans(plans)
                .setMineCard(buildMineCard(tier, effective, expireAt, plans))
                .setRecords(listRecords(userId));
    }

    @Override
    public MemberOrderVO createOrder(MemberPrepayDTO dto) {
        // 总开关先行：前端置灰不是防线，这里必须自己拦住（§7.2 要点 0）。
        // 这个开关只有会员有：通道层没有「关掉收款」的语义，第二个虚拟产品该带自己的开关
        if (!Boolean.TRUE.equals(memberProperties.getEnabled())) {
            throw new RunException("会员功能即将开放");
        }
        // 凭证闸门留在建单这一步，但理由与拆分前不同了（§7.2 要点 0.5）：拆成两步后签名那步必然要它，
        // 缺 Key 还照建单等于批量造「签不出名的死单」；闸门本体仍在通道侧，免得每个产品各写一份空 Key 判断
        xpaySignService.assertReady();
        Long userId = currentUserId();
        MemberPlanEntity plan = selectPlan(dto.getPlanCode());
        if (plan == null) {
            // 不是回落默认价、也不是当免费单：按 code 查不到就是配置事故
            log.error("下单拒接：member_plan 里查不到该套餐，code：{}", dto.getPlanCode());
            throw new RunException("套餐不存在");
        }
        if (!Integer.valueOf(1).equals(plan.getEnabled())) {
            // 已下架的 SKU 可能被旧版本前端拿着老缓存下单，不能只靠 info() 不下发（§12.1）
            throw new RunException("该套餐已下架，请重新进入页面");
        }
        if (StringUtils.isBlank(plan.getProductId())) {
            log.error("建单拒接：member_plan.product_id 为空，code：{}（MP 后台道具未建，§2.2 第 3 步）",
                    plan.getCode());
            throw new RunException("套餐配置不完整，请稍后再试");
        }
        // sessionKey 在建单时就先探一次：取不到意味着下一步必然签不出名，没必要落一条死单。
        // ⚠️ 但 paySign 仍要自己再取一次——两步之间不共享内存变量是这次拆分的底线（§7.2）
        if (StringUtils.isBlank(currentSessionKey())) {
            // 旧版本登录留下的 Redis token: 值里没有 sessionKey，也算这一类，前端重新登录后重试即可（§2.3）
            // ⚠️ 报 4006 而不是默认的 500：前端得按码判「该重登再试」，比对文案会随改字静默失效（§12.1 的 -15007）
            throw new RunException(ResultCodeEnum.SESSION_KEY_EXPIRED);
        }

        boolean firstOrder = PLAN_PRO_MONTH.equals(plan.getCode())
                && plan.getFirstPrice() != null && plan.getFirstPrice() > 0
                && isProFirstOrder(userId);
        int expectedPrice = firstOrder ? plan.getFirstPrice() : plan.getPrice();
        String outTradeNo = xpaySignService.nextOutTradeNo(OUT_TRADE_NO_PREFIX);
        LocalDateTime now = LocalDateTime.now();

        // 先把单落库：签名那一步只认这个单号，发货通知到达时也一定能按单号找到记录（§7.2 要点 2）
        MemberOrderEntity order = new MemberOrderEntity()
                .setOutTradeNo(outTradeNo)
                .setUserId(userId)
                .setPlanCode(plan.getCode())
                .setTier(plan.getTier())
                .setMonths(plan.getMonths())
                .setProductId(plan.getProductId())
                .setGoodsPrice(plan.getPrice())
                .setExpectedPrice(expectedPrice)
                // 死单不靠“以后有人对账”消化：建单那一刻就把过期时点算好写下去（§7.2）
                .setExpireAt(now.plusMinutes(ORDER_EXPIRE_MINUTES))
                .setStatus(MemberOrderEntity.STATUS_UNPAID);
        memberOrderMapper.insert(order);

        // title 不在订单快照列里（那是一行价格快照不是文案快照），所以取当前行的展示名；
        // 其余金额与 SKU 字段一律从刚插入的订单行回读，保证出参说的就是库里那一行
        return new MemberOrderVO()
                .setOutTradeNo(order.getOutTradeNo())
                .setPlanCode(order.getPlanCode())
                .setTitle(plan.getTitle())
                .setTier(order.getTier())
                .setMonths(order.getMonths())
                .setGoodsPrice(order.getGoodsPrice())
                .setExpectedPrice(order.getExpectedPrice())
                .setExpectedPriceText(priceText(order.getExpectedPrice()))
                .setExpireAt(order.getExpireAt())
                .setFirstOrder(firstOrder);
    }

    @Override
    public XpayPrepayVO paySign(MemberPaySignDTO dto) {
        // 签名也是收款链路的一环：置灰版即使前端拿不到单号，也不该能签出一份支付参数（§7.2 要点 0）
        if (!Boolean.TRUE.equals(memberProperties.getEnabled())) {
            throw new RunException("会员功能即将开放");
        }
        Long userId = currentUserId();
        MemberOrderEntity order = selectOrder(dto.getOutTradeNo());
        // 单号外人可猜，别人的单也回「订单不存在」：本接口不能沦为单号探测工具（与 §7.3 同口径）
        if (order == null || !userId.equals(order.getUserId())) {
            throw new RunException("订单不存在");
        }
        if (MemberOrderEntity.STATUS_UNPAID != order.getStatus()) {
            // 已发放 / 已退款 / 已关闭的单都不能再签出去，否则等于给同一笔钱签第二次
            throw new RunException("该订单已不可支付，请重新下单");
        }
        if (order.getExpireAt() != null && order.getExpireAt().isBefore(LocalDateTime.now())) {
            // 过期单就地关掉再拒。本步不能只靠定时任务：扫描每 5 分钟一轮，过期与下一轮之间那几分钟
            // 仍能签出去，等于过期形同虚设（而定时任务也不能省：没人再访问的单不会自己关）
            memberOrderMapper.casClose(order.getOutTradeNo(), MemberOrderEntity.FAIL_REASON_EXPIRED);
            throw new RunException("支付超时，请重新下单");
        }
        // env 已改为按 profile 现算（signData/query_order 在通道侧取 XpayEnv），订单不再存 env 快照、也不再跨环境拦签；
        // test/dev 已各自独立库，不存「沙箱单混进现网表」这个前提，旧的同表守卫随之删除
        // sessionKey 只在这里取：取不到就是拒签，前端据此重新登录然后**重签同一单号**——
        // 这正是拆分本接口的理由（拆前只能重新 prepay 换新单号，每重试一次多留一条待支付单，§12.1 的 -15007）
        String sessionKey = currentSessionKey();
        if (StringUtils.isBlank(sessionKey)) {
            // 同上：本处是前端「重签同一单号」那条重试分支的判据点，码不可换回文案（§12.1）
            throw new RunException(ResultCodeEnum.SESSION_KEY_EXPIRED);
        }
        // 活动价从金额快照还原：expected < goods 只可能是首单（first_price 必须严格小于 price，
        // 这条由 MemberPlanValidator 启动校验守住），所以不必新增一列也不会误判
        Integer activityPrice = order.getExpectedPrice() < order.getGoodsPrice()
                ? order.getExpectedPrice() : null;

        XpaySignService.Goods goods = new XpaySignService.Goods()
                // 三个值全部取订单快照而不是重查 member_plan：两步之间运营改价也不会出现「旧价建单、新价签名」（§7.2 拆分要点 3）
                .setProductId(order.getProductId())
                // goodsPrice 必须是道具原价，填活动价微信侧直接报 -15013（§7.2 要点 3）
                .setGoodsPrice(order.getGoodsPrice())
                // 不享首单时传 null，通道会整个不下发 activitySellingPrice，而不是传 0
                .setActivityPrice(activityPrice)
                .setOutTradeNo(order.getOutTradeNo())
                // attach 用订单上的 userId 而不是当前登录者：二者已由归属校验锁成同一人，
                // 但快照才是「建单那一刻是谁的单」的事实，发货时交叉校验更稳（§7.2 要点 5）
                .setAttach("userId=" + order.getUserId());
        // 凭证闸门不在这里重复调：buildGoodsPayment 内部第一件事就是 assertReady()（§2.3），
        // 而 signData 序列化与两把 HMAC 都在通道侧：同一个串既签名又下发这条铁律只能有一处实现
        return xpaySignService.buildGoodsPayment(goods, sessionKey);
    }

    @Override
    public int closeExpiredOrders() {
        // 不拿 member.enabled 当闸门：置灰只是不开新单，已有的超时死单照样得清，
        // 而且把闸门装上反而会让它们永远堆着（本作业是项目里第一个 @Scheduled）
        int closed = memberOrderMapper.casCloseExpiredBatch(LocalDateTime.now(), MemberOrderEntity.FAIL_REASON_EXPIRED);
        if (closed > 0) {
            log.info("超时未支付订单已关闭，行数：{}", closed);
        }
        return closed;
    }

    @Override
    public int remindExpiringMembers() {
        // 窗口 = 「今天 + 提前天数」那一整天：每人一年只会落进一次，天然去重，不需要提醒记录表
        LocalDate target = LocalDate.now().plusDays(EXPIRE_REMIND_AHEAD_DAYS);
        LocalDateTime start = target.atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        List<MemberExpireNoticeVO> rows = memberMapper.selectExpiringForNotice(start, end);
        int sent = 0;
        for (MemberExpireNoticeVO row : rows) {
            // userId 是扣配额的钥匙、expireAt 是渲染字段，缺任一都没法发，直接跳过（openid 空由发送服务判）
            if (row.getUserId() == null || row.getExpireAt() == null) {
                continue;
            }
            // 剩余天数按日期差算（窗口固定 = 今天 + N 天，恒等于 N），不用时长向上取整以免 9 点跑出的数飘
            long remainDays = ChronoUnit.DAYS.between(LocalDate.now(), row.getExpireAt().toLocalDate());
            Map<String, Object> data = new HashMap<>(3);
            data.put(FIELD_MEMBER_NAME, noticeValue(tierName(row.getTier())));
            data.put(FIELD_EXPIRE_DATE, noticeValue(row.getExpireAt().format(EXPIRE_DATE_FORMATTER)));
            data.put(FIELD_REMAIN_DAYS, noticeValue(String.valueOf(remainDays)));
            // 找模板、扣配额、发微信、记流水全部交给通用发送服务：
            // 无配额/无模板/发送失败它都只返回 false 并留痕，绝不打断这一轮其余人的提醒
            if (subscribeMessageService.send(row.getUserId(), row.getOpenid(),
                    SubscribeTypeEnum.MEMBER_EXPIRE, data, EXPIRE_NOTICE_PAGE)) {
                sent++;
            }
        }
        if (sent > 0) {
            log.info("会员到期提醒已发送，命中 {} 人，成功 {} 条", rows.size(), sent);
        }
        return sent;
    }

    /**
     * 订阅消息字段值包装：微信要求每个字段是 { "value": "xxx" } 结构
     */
    private Map<String, Object> noticeValue(String value) {
        Map<String, Object> map = new HashMap<>(1);
        map.put("value", value);
        return map;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public MemberConfirmVO confirm(MemberConfirmDTO dto) {
        Long userId = currentUserId();
        MemberOrderEntity order = selectOrder(dto.getOutTradeNo());
        // 单号是外人可猜的，别人的单也回「订单不存在」，不额外区分以免变成单号探测接口
        if (order == null || !userId.equals(order.getUserId())) {
            throw new RunException("订单不存在");
        }
        if (MemberOrderEntity.STATUS_UNPAID != order.getStatus() && !isExpiredClosed(order)) {
            // 已发放/已退款/校验失败关单都不是「再等一等」能改变的，直接把真实状态给前端（§7.3）
            return new MemberConfirmVO()
                    .setDelivered(MemberOrderEntity.STATUS_DELIVERED == order.getStatus())
                    .setStatus(order.getStatus());
        }
        // 超时关掉那类单也走到这里：用户可能在关单之后才付完，而他自己的轮询比微信重推来得早，
        // openid 又就在登录快照里（拿不到 openid 就查不了单，§7.4 的超时单补发放）
        LoginUser loginUser = SecurityUtils.getLoginUser();
        String openid = loginUser != null ? loginUser.getOpenid() : null;
        doGrant(order.getOutTradeNo(), openid, null, null, null, null);
        MemberOrderEntity latest = selectOrder(dto.getOutTradeNo());
        int status = latest != null ? latest.getStatus() : MemberOrderEntity.STATUS_UNPAID;
        return new MemberConfirmVO()
                .setDelivered(MemberOrderEntity.STATUS_DELIVERED == status)
                .setStatus(status);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean grant(String outTradeNo, String openid, Integer notifyActualPrice,
                         String transactionId, String mchOrderNo, LocalDateTime paidAt) {
        return doGrant(outTradeNo, openid, notifyActualPrice, transactionId, mchOrderNo, paidAt);
    }

    @Override
    public boolean isEffective(Long userId) {
        return effectiveTierFromSnapshot(userId) > 0;
    }

    @Override
    public boolean isAdFree(Long userId) {
        return isEffective(userId);
    }

    @Override
    public boolean hasUnlimitedCapacity(Long userId) {
        return effectiveTierFromSnapshot(userId) >= MemberPlanEntity.TIER_PRO;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean revoke(String outTradeNo) {
        MemberOrderEntity order = selectOrder(outTradeNo);
        if (order == null) {
            log.error("退款事件找不到订单，请重推。outTradeNo：{}", outTradeNo);
            return false;
        }
        if (MemberOrderEntity.STATUS_REFUNDED == order.getStatus()) {
            // 幂等：微信会把退款事件也重推，已回收过就直接了结
            log.error("退款事件重复推送，权益已回收，忽略。outTradeNo：{}", outTradeNo);
            return true;
        }
        LocalDateTime now = LocalDateTime.now();
        if (memberOrderMapper.casRefund(outTradeNo, now) == 0) {
            // 抢不到就是这单从未发放（或状态已变），没有权益可扣
            log.error("退款事件到达但订单未处于已发放状态，不倒扣权益。outTradeNo：{}，当前状态：{}", outTradeNo, order.getStatus());
            return true;
        }
        MemberEntity row = selectMember(order.getUserId());
        if (row == null) {
            log.error("退款回收找不到 member 行，订单已置已退款但权益未扣。outTradeNo：{}，userId：{}",
                    outTradeNo, order.getUserId());
            return true;
        }
        LocalDateTime base = row.getExpireAt() != null && row.getExpireAt().isAfter(now) ? row.getExpireAt() : now;
        LocalDateTime deducted = base.minusMonths(monthsOf(order));
        if (deducted.isAfter(now)) {
            row.setExpireAt(deducted);
        } else {
            // 扣穿：清档（tier 归 0）。不试图「降回上一档」，因为剩余天数属于哪一档无法从一行数据里还原（§7.5 末）
            row.setExpireAt(now).setTier(0);
        }
        memberMapper.updateById(row);
        redisUtils.delete(RedisConstant.USER + order.getUserId());
        log.error("退款回收完成，outTradeNo：{}，userId：{}，倒扣月数：{}，新到期：{}，新档位：{}",
                outTradeNo, order.getUserId(), monthsOf(order), row.getExpireAt(), row.getTier());
        return true;
    }

    /**
     * 是否「超时未支付关掉」的单：唯一一类钱还可能后到的关闭单（§7.4）。
     * 按固定原因串判，所以金额不符那类关单不会被这条路救回来
     */
    private boolean isExpiredClosed(MemberOrderEntity order) {
        return MemberOrderEntity.STATUS_CLOSED == order.getStatus()
                && MemberOrderEntity.FAIL_REASON_EXPIRED.equals(order.getFailReason());
    }

    /**
     * 发放主体：查单复核 → 金额校验 → CAS 抢幂等 → 写权益 → 刷缓存
     * <p>
     * 抽成私有方法是为了让 {@link #confirm} 能直接复用而不经过 Spring 代理
     * （同类内 {@code this.grant()} 不会开启新事务，这是 self-invocation 的经典坑）。
     * </p>
     *
     * @return true = 已了结（回 ErrCode 0）；false = 未确证（回 -1 请重推）
     */
    private boolean doGrant(String outTradeNo, String openid, Integer notifyActualPrice,
                            String transactionId, String mchOrderNo, LocalDateTime paidAt) {
        MemberOrderEntity order = selectOrder(outTradeNo);
        if (order == null) {
            log.error("发放通知找不到订单，请重推。outTradeNo：{}，openid：{}", outTradeNo, openid);
            return false;
        }
        if (MemberOrderEntity.STATUS_DELIVERED == order.getStatus()) {
            log.error("订单已发放，幂等忽略重复通知。outTradeNo：{}", outTradeNo);
            return true;
        }
        // 超时关单是唯一一类「关了但钱还可能进来」的单：我们没有取消微信侧订单的能力，
        // 用户在手慢的几分钟里付了钱，那一笔照样要发（下面走同一条查单复核，但 CAS 走 3→1）
        boolean expiredClosed = isExpiredClosed(order);
        if (MemberOrderEntity.STATUS_UNPAID != order.getStatus() && !expiredClosed) {
            // 其余已关闭或已退款的单不再动它，钱的事实在订单里留着，重推也不会变
            log.error("订单状态不允许发放，忽略通知。outTradeNo：{}，当前状态：{}", outTradeNo, order.getStatus());
            return true;
        }

        // ① 查单复核：通知可以被伪造（没有真实订单的伪造通知查单必失败），也可以被链路篡改
        XpayClient.XpayOrder query = xpayClient.queryOrder(openid, outTradeNo);
        if (query == null) {
            log.error("查单未取得确证，暂不发放，等微信重推。outTradeNo：{}，openid：{}", outTradeNo, openid);
            return false;
        }
        if (!XpayClient.isPaid(query)) {
            int wxStatus = query.getStatus() == null ? -999 : query.getStatus();
            if (wxStatus == 0 || wxStatus == 1) {
                // 微信侧还没到账，重推有意义
                log.error("查单显示订单尚未支付，暂不发放，等微信重推。outTradeNo：{}，微信状态：{}", outTradeNo, wxStatus);
                return false;
            }
            // 2 以外的其他状态（5/6/7/8/9/10）都不会再变成可发放，关单留痕并了结
            memberOrderMapper.casClose(outTradeNo, "微信订单状态不可发放：" + wxStatus);
            log.error("查单确认这笔单不可发放，已关单。outTradeNo：{}，微信状态：{}", outTradeNo, wxStatus);
            return true;
        }

        // ② 金额校验：基准是 expected_price，拿 goods_price 会把每一笔首单都误判为异常（§7.4）
        Integer actualPrice = query.getPaidFee() != null && query.getPaidFee() > 0 ? query.getPaidFee() : notifyActualPrice;
        if (actualPrice == null) {
            log.error("实付金额无从确证（查单与通知都没给），暂不发放，outTradeNo：{}", outTradeNo);
            return false;
        }
        if (!actualPrice.equals(order.getExpectedPrice())) {
            // 超时单已处于 3，这条 CAS（它只做 0→3）会抢不到而空转，原因就停在「超时」而不是「金额不符」；
            // 不专为此改 fail_reason：对账看 log，而钱不发的结论两种类别是一致的
            memberOrderMapper.casClose(outTradeNo, "实付 " + actualPrice + " 与应付 " + order.getExpectedPrice() + " 不一致");
            log.error("实付金额与应付不一致，不发放已关单，需人工核对道具价。outTradeNo：{}，实付：{}，应付：{}，套餐：{}",
                    outTradeNo, actualPrice, order.getExpectedPrice(), order.getPlanCode());
            return true;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime paidTime = resolvePaidTime(paidAt, query);
        String wxTransactionId = StringUtils.isNotBlank(query.getWxpayOrderId()) ? query.getWxpayOrderId() : transactionId;
        // ③ CAS 抢发放权：重推与轮询并发时只有一方进得来（超时已关的单走 3→1 那一条）
        int won = expiredClosed
                ? memberOrderMapper.casDeliverExpired(outTradeNo, actualPrice, wxTransactionId, mchOrderNo, paidTime, now,
                        MemberOrderEntity.FAIL_REASON_EXPIRED)
                : memberOrderMapper.casDeliver(outTradeNo, actualPrice, wxTransactionId, mchOrderNo, paidTime, now);
        if (won == 0) {
            log.error("并发下发放权已被抢走，本次不重复写权益。outTradeNo：{}", outTradeNo);
            return true;
        }
        if (expiredClosed) {
            // 故意用 error 级别：这条路径应当罕见（用户从拿到签名到付完款超了 15 分钟），刷出来就该看一眼
            log.error("超时已关单但查单确证已支付，已补发放。outTradeNo：{}，过期时间：{}，实付：{}",
                    outTradeNo, order.getExpireAt(), actualPrice);
        }
        // ④ 权益落库 + ⑤ 刷缓存（不删的话他付了钱广告还在，§6.3 / §17 第 11 条）
        deliverBenefit(order, now);
        redisUtils.delete(RedisConstant.USER + order.getUserId());
        log.error("会员权益发放完成。outTradeNo：{}，userId：{}，套餐：{}，档位：{}，月数：{}，实付：{}",
                outTradeNo, order.getUserId(), order.getPlanCode(), order.getTier(), order.getMonths(), actualPrice);
        return true;
    }

    /**
     * 写 member：新开/同档续费走顺延，只有「在期 ∧ 升档」才走价值折算（docs/v12 §7.5）
     */
    private void deliverBenefit(MemberOrderEntity order, LocalDateTime now) {
        Long userId = order.getUserId();
        MemberEntity row = selectMember(userId);
        boolean isNew = row == null;
        if (isNew) {
            row = new MemberEntity().setUserId(userId).setTier(0);
        }
        int currentTier = row.getTier() == null ? 0 : row.getTier();
        LocalDateTime currentExpire = row.getExpireAt();
        boolean effective = currentTier > 0 && currentExpire != null && currentExpire.isAfter(now);
        int planTier = order.getTier() == null ? 0 : order.getTier();
        LocalDateTime newExpire;
        if (effective && planTier > currentTier) {
            long convertedDays = convertRemainDays(userId, currentTier, currentExpire, order, now);
            newExpire = now.plusMonths(monthsOf(order)).plusDays(convertedDays);
            log.error("跨档升级折算。userId：{}，{}档→{}档，剩余天折{}天，新到期：{}",
                    userId, currentTier, planTier, convertedDays, newExpire);
        } else {
            // 同档（含同档换周期）一律顺延：走折算会反向多送（§7.5 邻近场景表第三行）
            LocalDateTime base = effective ? currentExpire : now;
            newExpire = base.plusMonths(monthsOf(order));
        }
        row.setTier(Math.max(effective ? currentTier : 0, planTier))
                .setExpireAt(newExpire)
                .setLastPaidAt(now);
        if (row.getFirstPaidAt() == null) {
            row.setFirstPaidAt(now);
        }
        if (isNew) {
            memberMapper.insert(row);
        } else {
            memberMapper.updateById(row);
        }
    }

    /**
     * 低档剩余价值折算的高（F）：{@code F = ceil(D × 低档日均价 / 高档日均价)}，日均价 = 价 ÷ (月数 × 30)
     * <p>
     * 低档日均价取「该用户上一次发放的那一单」的快照，因为剩余天数就是那一单买来的；
     * 一档不设首单价，所以目录价恒等于实付价，折算不必碰 actual_price（§7.5 末）。
     * </p>
     *
     * @return 折算天数；找不到来源订单（数据被手工改过）时返回 0 并告警，宁可少给也不猜
     */
    private long convertRemainDays(Long userId, int currentTier, LocalDateTime currentExpire,
                                   MemberOrderEntity order, LocalDateTime now) {
        int remainDays = ceilDays(now, currentExpire);
        LambdaQueryWrapper<MemberOrderEntity> wrapper = new LambdaQueryWrapper<MemberOrderEntity>()
                .eq(MemberOrderEntity::getUserId, userId)
                .eq(MemberOrderEntity::getTier, currentTier)
                .eq(MemberOrderEntity::getStatus, MemberOrderEntity.STATUS_DELIVERED)
                .orderByDesc(MemberOrderEntity::getDeliverAt)
                .last("limit 1");
        MemberOrderEntity source = memberOrderMapper.selectOne(wrapper);
        if (source == null || source.getGoodsPrice() == null || source.getMonths() == null || source.getMonths() <= 0) {
            log.error("跨档升级找不到低档来源订单，本次不做剩余价值折算（只按新周期发放）。userId：{}，低档：{}", userId, currentTier);
            return 0;
        }
        if (order.getGoodsPrice() == null || monthsOf(order) <= 0) {
            log.error("本次订单缺少价格或周期快照，无法折算。outTradeNo：{}", order.getOutTradeNo());
            return 0;
        }
        double lowDaily = (double) source.getGoodsPrice() / (source.getMonths() * DAYS_PER_MONTH);
        double highDaily = (double) order.getGoodsPrice() / (monthsOf(order) * DAYS_PER_MONTH);
        if (highDaily <= 0) {
            return 0;
        }
        return (long) Math.ceil(remainDays * lowDaily / highDaily);
    }

    /**
     * 支付时间：通知带来的优先，其次查单的 paid_time，都没有就用发放时刻
     */
    private LocalDateTime resolvePaidTime(LocalDateTime notifyPaidAt, XpayClient.XpayOrder query) {
        if (notifyPaidAt != null) {
            return notifyPaidAt;
        }
        if (query.getPaidTime() != null && query.getPaidTime() > 0) {
            return LocalDateTime.ofEpochSecond(query.getPaidTime(), 0, BEIJING_OFFSET);
        }
        return LocalDateTime.now();
    }

    /**
     * 在售套餐：{@code member_plan WHERE enabled = 1 ORDER BY sort}，不走缓存（§8.3）
     */
    private List<MemberPlanVO> listPlans() {
        LambdaQueryWrapper<MemberPlanEntity> wrapper = new LambdaQueryWrapper<MemberPlanEntity>()
                .eq(MemberPlanEntity::getEnabled, 1)
                .orderByAsc(MemberPlanEntity::getSort);
        List<MemberPlanEntity> entities = memberPlanMapper.selectList(wrapper);
        List<MemberPlanVO> plans = new ArrayList<>();
        for (MemberPlanEntity entity : entities) {
            plans.add(toPlanVO(entity));
        }
        return plans;
    }

    private MemberPlanVO toPlanVO(MemberPlanEntity entity) {
        return new MemberPlanVO()
                .setCode(entity.getCode())
                .setTier(entity.getTier())
                .setMonths(entity.getMonths())
                .setName(entity.getTitle())
                .setDesc(tierDesc(entity.getTier()))
                .setPrice(entity.getPrice())
                .setPriceText(priceText(entity.getPrice()))
                .setFirstPrice(entity.getFirstPrice())
                .setFirstPriceText(priceText(entity.getFirstPrice()))
                .setSaved(entity.getSavedPrice());
    }

    /**
     * 本人最近 5 条已发放/已退款订单（§7.1）
     */
    private List<MemberRecordVO> listRecords(Long userId) {
        LambdaQueryWrapper<MemberOrderEntity> wrapper = new LambdaQueryWrapper<MemberOrderEntity>()
                .eq(MemberOrderEntity::getUserId, userId)
                .in(MemberOrderEntity::getStatus, MemberOrderEntity.STATUS_DELIVERED, MemberOrderEntity.STATUS_REFUNDED)
                .orderByDesc(MemberOrderEntity::getPaidAt)
                .last("limit " + RECORD_LIMIT);
        List<MemberOrderEntity> orders = memberOrderMapper.selectList(wrapper);
        if (orders == null || orders.isEmpty()) {
            return Collections.emptyList();
        }
        List<MemberRecordVO> records = MemberConvert.INSTANCE.convertRecordList(orders);
        for (int i = 0; i < records.size(); i++) {
            MemberRecordVO record = records.get(i);
            if (record.getAmount() == null) {
                // 极老的脏数据可能没有 actual_price，退回应付金额，别让记录页出现空价格
                record.setAmount(orders.get(i).getExpectedPrice());
            }
        }
        return records;
    }

    /**
     * 「我的」页会员卡四态文案（§4.1）
     * <p>
     * 态序即口径：金色（在期）→ 已过期 → 置灰 → 未开通。前两个是**已有履约的人**，
     * 即使 {@code member.enabled} 被回滚成 false 也不降级：付过钱的人看到「开通会员 · 即将开放」
     * 等于我们否认已经成交的订单（§4.1「别拿未开通卡兜底」那一类）。
     * </p>
     * <p>
     * 但**行动位必须随开关走**：买不到的时候不递「续费」「继续开通」，
     * 否则就是把用户推进一个灰按钮（§4.1 第一行）；价格锚同时在 !open 时置 null（不标价买不到的东西）。
     * </p>
     */
    private MemberMineCardVO buildMineCard(int tier, boolean effective, LocalDateTime expireAt, List<MemberPlanVO> plans) {
        boolean open = Boolean.TRUE.equals(memberProperties.getEnabled());
        if (effective) {
            return new MemberMineCardVO()
                    .setTitle(tierName(tier))
                    .setDesc(tierDesc(tier))
                    // 功能临时关闭时不递「续费」：点进去是灰按钮就是死按钮
                    .setAction(open ? "续费" : "即将开放")
                    .setTone("gold");
        }
        String lowestPrice = open ? lowestPriceText(plans) : null;
        if (expireAt != null) {
            // 已过期：不羞辱、也不装作没发生，这句同时回答「我上次付的钱去哪了」
            return new MemberMineCardVO()
                    .setTitle("开通会员")
                    .setDesc("已于 " + expireAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + " 到期")
                    .setPrice(lowestPrice)
                    .setAction(open ? "继续开通" : "即将开放")
                    .setTone("expired");
        }
        if (!open) {
            return new MemberMineCardVO()
                    .setTitle("开通会员")
                    .setDesc("免广告 · 分类菜谱不限数")
                    .setAction("即将开放")
                    .setTone("disabled");
        }
        return new MemberMineCardVO()
                .setTitle("开通会员")
                .setDesc("免广告 · 分类菜谱不限数")
                .setPrice(lowestPrice)
                .setAction("去开通")
                .setTone("plain");
    }

    /**
     * 最低在售套餐价：非会员卡片上的价格锚（§4.1「¥1.9 起」）
     */
    private String lowestPriceText(List<MemberPlanVO> plans) {
        Integer lowest = null;
        for (MemberPlanVO plan : plans) {
            if (plan.getPrice() != null && (lowest == null || plan.getPrice() < lowest)) {
                lowest = plan.getPrice();
            }
        }
        return lowest == null ? null : priceText(lowest) + " 起";
    }

    private String tierName(Integer tier) {
        return tier != null && tier >= MemberPlanEntity.TIER_PRO ? "畅厨会员" : "轻享会员";
    }

    /**
     * 权益摘要按档位给（§5.1 两张卡的卡内文案），轻享必须带上「容量仍受限」这一层，否则是投诉不是差评
     */
    private String tierDesc(Integer tier) {
        if (tier != null && tier >= MemberPlanEntity.TIER_PRO) {
            return "免广告 · 自建厨房的分类与菜谱不限";
        }
        return "免广告（容量仍受限，可看视频 +1）";
    }

    /**
     * 分 → 展示串：190 → ¥1.9、100 → ¥1，不用科学计数法也不用「1.90」这种补零写法
     */
    private String priceText(Integer cents) {
        if (cents == null) {
            return null;
        }
        return "¥" + BigDecimal.valueOf(cents).movePointLeft(2).stripTrailingZeros().toPlainString();
    }

    /**
     * 向上取整的天数差，用于 remainDays 与折算的 D
     */
    private int ceilDays(LocalDateTime from, LocalDateTime to) {
        long seconds = Duration.between(from, to).getSeconds();
        if (seconds <= 0) {
            return 0;
        }
        return (int) Math.ceil((double) seconds / SECONDS_PER_DAY);
    }

    private int monthsOf(MemberOrderEntity order) {
        return order.getMonths() == null ? 1 : order.getMonths();
    }

    /**
     * 首单判定：按档位不按用户——不存在 {@code tier=2 且 status=1} 的订单即仍享畅厨·月首单价（§7.2 要点 3）
     * <p>
     * 先买过 ¥1.9 轻享的人仍应看到 ¥2.9，用 first_paid_at 判会把这批最有转化价值的人的升级资格没收掉。
     * </p>
     */
    private boolean isProFirstOrder(Long userId) {
        LambdaQueryWrapper<MemberOrderEntity> wrapper = new LambdaQueryWrapper<MemberOrderEntity>()
                .eq(MemberOrderEntity::getUserId, userId)
                .eq(MemberOrderEntity::getTier, MemberPlanEntity.TIER_PRO)
                .eq(MemberOrderEntity::getStatus, MemberOrderEntity.STATUS_DELIVERED);
        return memberOrderMapper.selectCount(wrapper) == 0;
    }

    /**
     * 库里读出的档位按 expire_at 归零，与 {@code UserDTO.memberTier} 同口径（§8.1）
     */
    private int effectiveTier(MemberEntity row, LocalDateTime now) {
        // 归零判断下沉到实体自己身上：两条出口共用一份实现，不给「改一处漏一处」留空间
        return row == null ? 0 : row.effectiveTier(now);
    }

    /**
     * 判定三件套的公共实现：读登录态快照、零额外查询；快照缺失或不是本人一律按非会员处理（§12.2）
     */
    private int effectiveTierFromSnapshot(Long userId) {
        LoginUser user = SecurityUtils.getLoginUser();
        if (user == null || userId == null || !userId.equals(user.getId())) {
            // 不是当前登录人的判定不能拿他的快照回答，也不能为此去查库：宁可误限不可漏限
            return 0;
        }
        return user.memberTierAt(LocalDateTime.now());
    }

    private MemberEntity selectMember(Long userId) {
        if (userId == null) {
            return null;
        }
        return memberMapper.selectOne(new LambdaQueryWrapper<MemberEntity>()
                .eq(MemberEntity::getUserId, userId));
    }

    private MemberOrderEntity selectOrder(String outTradeNo) {
        if (StringUtils.isBlank(outTradeNo)) {
            return null;
        }
        return memberOrderMapper.selectOne(new LambdaQueryWrapper<MemberOrderEntity>()
                .eq(MemberOrderEntity::getOutTradeNo, outTradeNo));
    }

    private MemberPlanEntity selectPlan(String planCode) {
        return memberPlanMapper.selectOne(new LambdaQueryWrapper<MemberPlanEntity>()
                .eq(MemberPlanEntity::getCode, planCode));
    }

    private Long currentUserId() {
        Long userId = SecurityUtils.getLoginUserId();
        if (userId == null) {
            throw new RunException(ResultCodeEnum.NOT_LOGIN);
        }
        return userId;
    }

    /**
     * sessionKey 从 {@code token:} 的值里取（§9.3 第 1 条定稿：并入 LoginResVO，随 token 同 TTL，不另开 key）
     */
    private String currentSessionKey() {
        String token = SecurityUtils.getToken();
        if (StringUtils.isBlank(token)) {
            return null;
        }
        return redisUtils.get(RedisConstant.TOKEN + token, LoginResVO.class)
                .map(LoginResVO::getSessionKey)
                .orElse(null);
    }
}
