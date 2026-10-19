package com.dishes.modules.member.mapper;

import com.dishes.core.mybatisplus.mapper.BaseMapperX;
import com.dishes.modules.member.entity.MemberOrderEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * 会员订单
 * <p>
 * 四个状态流转都做成 CAS（带旧状态条件的 UPDATE），靠受影响行数判成败，而不是「先查再改」：
 * 发货通知最多会被微信重推 15 次，加上前端轮询 /member/confirm，同一单一定并发。
 * 原生 SQL 绕过了 {@code @TableLogic}，所以每条都手写 {@code deleted = 0}（本表该列恒为 0）。
 * </p>
 * @author 好市民
 */
@Mapper
public interface MemberOrderMapper extends BaseMapperX<MemberOrderEntity> {

    /**
     * 发放：待支付 0 → 已发放 1，并把通知/查单带来的资金字段一次落齐
     *
     * @return 1 表示本次调用抢到了发放权；0 表示此前已发放过（幂等出口，直接回成功即可）
     */
    @Update("UPDATE member_order SET status = 1, actual_price = #{actualPrice}, transaction_id = #{transactionId}, "
            + "mch_order_no = #{mchOrderNo}, paid_at = #{paidAt}, deliver_at = #{deliverAt}, fail_reason = NULL "
            + "WHERE out_trade_no = #{outTradeNo} AND status = 0 AND deleted = 0")
    int casDeliver(@Param("outTradeNo") String outTradeNo,
                   @Param("actualPrice") Integer actualPrice,
                   @Param("transactionId") String transactionId,
                   @Param("mchOrderNo") String mchOrderNo,
                   @Param("paidAt") LocalDateTime paidAt,
                   @Param("deliverAt") LocalDateTime deliverAt);

    /**
     * 退款：已发放 1 → 已退款 2（只有抢到的一方才去倒扣权益天数，避免重复退款事件扣多次）
     *
     * @return 1-本次抢到；0-未发放过或已回收入账
     */
    @Update("UPDATE member_order SET status = 2, refunded_at = #{refundedAt} "
            + "WHERE out_trade_no = #{outTradeNo} AND status = 1 AND deleted = 0")
    int casRefund(@Param("outTradeNo") String outTradeNo,
                  @Param("refundedAt") LocalDateTime refundedAt);

    /**
     * 校验失败关单：待支付 0 → 已关闭 3 并留痕原因。
     * 只关未发放的单——已发放的单出现金额不符时留着原状态更好排查，不在这条 SQL 里改资金凭证
     *
     * @return
     */
    @Update("UPDATE member_order SET status = 3, fail_reason = #{failReason} "
            + "WHERE out_trade_no = #{outTradeNo} AND status = 0 AND deleted = 0")
    int casClose(@Param("outTradeNo") String outTradeNo,
                 @Param("failReason") String failReason);

    /**
     * 超时关单（定时任务每 5 分钟一次）：把过了 expire_at 仍未支付的单批量 0 → 3。
     * <p>
     * 本方法不接受单号参数（它压根不区分单号），但它不怕并发：条件里带 {@code status = 0}，
     * 所以同一时刻正在发放的单会抢先进入 1，不会被这条扫到。
     * </p>
     *
     * @return 本次关掉的行数（为 0 时任务不记日志，避免刷屏）
     */
    @Update("UPDATE member_order SET status = 3, fail_reason = #{failReason} "
            + "WHERE status = 0 AND expire_at < #{now} AND deleted = 0")
    int casCloseExpiredBatch(@Param("now") LocalDateTime now,
                             @Param("failReason") String failReason);

    /**
     * 超时单补发放：因超时被关（status=3 且原因＝{@link MemberOrderEntity#FAIL_REASON_EXPIRED}）
     * 却又收到真实支付的单，3 → 1。
     * <p>
     * ⚠️ 它只认固定原因串，所以「金额不符 / 微信侧状态不可发放」那类关单绝不会被这条路救回来——
     * 那一类本来就不该发。微信侧没有「取消订单」能力，本地关单挡不住用户付钱，这就是它存在的理由（§7.4）。
     * </p>
     *
     * @return 1-本次抢到补发放权；0-已被并发方处理或原因对不上
     */
    @Update("UPDATE member_order SET status = 1, actual_price = #{actualPrice}, transaction_id = #{transactionId}, "
            + "mch_order_no = #{mchOrderNo}, paid_at = #{paidAt}, deliver_at = #{deliverAt}, fail_reason = NULL "
            + "WHERE out_trade_no = #{outTradeNo} AND status = 3 AND fail_reason = #{failReason} AND deleted = 0")
    int casDeliverExpired(@Param("outTradeNo") String outTradeNo,
                          @Param("actualPrice") Integer actualPrice,
                          @Param("transactionId") String transactionId,
                          @Param("mchOrderNo") String mchOrderNo,
                          @Param("paidAt") LocalDateTime paidAt,
                          @Param("deliverAt") LocalDateTime deliverAt,
                          @Param("failReason") String failReason);
}
