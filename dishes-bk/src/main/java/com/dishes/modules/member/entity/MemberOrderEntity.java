package com.dishes.modules.member.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dishes.common.po.BasePO;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 会员订单（虚拟支付道具直购，资金凭证）
 * <p>
 * 只有状态流转 0→1→2/3，业务上没有删除动作；对账、退款、投诉处理全要留痕（docs/v12 §8.2）。
 * 基础字段四件套由 BasePO 提供，但 {@code deleted} 恒为 0——真软删了行会让对账默默漏行。
 * </p>
 * <p>
 * ⚠️ 关于 {@code status=3}（已关闭）的两类来源必须分清：校验失败关单是不可逆的（金额不符、微信侧状态不可发放），
 * 而<b>超时关单</b>不是——我们没有「取消微信侧订单」的能力，所以一单被本地按超时关掉之后，
 * 钱仍然可能真的付进来。这一类靠 {@link #FAIL_REASON_EXPIRED} 这个固定串识别（§7.4）。
 * </p>
 * @author 好市民
 */
@Data
@Accessors(chain = true)
@TableName(value = "member_order", autoResultMap = true)
public class MemberOrderEntity extends BasePO {

    /**
     * 待支付
     */
    public static final int STATUS_UNPAID = 0;
    /**
     * 已发放（权益已写入 member）
     */
    public static final int STATUS_DELIVERED = 1;
    /**
     * 已退款
     */
    public static final int STATUS_REFUNDED = 2;
    /**
     * 已关闭（金额不符 / 查单复核不通过等校验失败，或超时未支付）
     */
    public static final int STATUS_CLOSED = 3;

    /**
     * 超时未支付关单的固定原因串：关单写它、补发放按它认出「这单只是超时、不是校验失败」。
     * 两边都引用本常量，所以改文案不会把补发放那条路悄悄弄断
     */
    public static final String FAIL_REASON_EXPIRED = "超时未支付自动关闭";

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 业务订单号（唯一索引 uk_out_trade_no，8~32 位，每号只用一次）
     */
    private String outTradeNo;
    /**
     * 下单用户ID
     */
    private Long userId;
    /**
     * 套餐编码快照（lite_month … pro_year）
     */
    private String planCode;
    /**
     * 档位快照：1-轻享 2-畅厨
     */
    private Integer tier;
    /**
     * 周期月数快照（本版只用 1/3/12，6 为半年付预留），配置改了也解释得动历史订单
     */
    private Integer months;
    /**
     * 微信道具ID快照（沙箱与现网共用同一套道具 ID）
     */
    private String productId;
    /**
     * 道具原价（分），signData.goodsPrice 永远用这个值
     */
    private Integer goodsPrice;
    /**
     * 本单应实付金额（分）：首单写 first_price，否则写 goods_price。
     * ⚠️ 发放校验的唯一比对基准——拿 goods_price 校会把每一笔首单误判为异常（§7.4）
     */
    private Integer expectedPrice;
    /**
     * 支付有效期：建单时写 {@code now + 15 分钟}，之后没人再改。
     * 超时由签名校验即时拒签关单、定时任务兜底批量关单（§7.2）；单号一旦关联过成功支付就永久不可复用（§12.1）
     */
    private LocalDateTime expireAt;
    /**
     * 实付金额（分），发货通知或查单回填
     */
    private Integer actualPrice;
    /**
     * 状态：0-待支付 1-已发放 2-已退款 3-已关闭（校验失败或超时未支付）
     */
    private Integer status;
    /**
     * 微信支付交易单号（对账用）
     */
    private String transactionId;
    /**
     * 微信商户单号（对账用）
     */
    private String mchOrderNo;
    /**
     * 用户支付时间
     */
    private LocalDateTime paidAt;
    /**
     * 权益发放时间（CAS 0→1 成功那一刻）
     */
    private LocalDateTime deliverAt;
    /**
     * 退款完成时间
     */
    private LocalDateTime refundedAt;
    /**
     * 关闭/异常原因留痕（价格不符等）
     */
    private String failReason;
}
