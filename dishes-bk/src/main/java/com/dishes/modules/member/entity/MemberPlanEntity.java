package com.dishes.modules.member.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dishes.common.po.BasePO;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 会员套餐（SKU 与价格的唯一真相，docs/v12 §8.3）
 * <p>
 * 落表而不是落 yml 是为了「改价不发版」——但要说清它只省掉重打 JAR 这一步，
 * 后台道具改价、等约 10 分钟、调价公示这三步一步都省不掉。
 * 因此本表是唯一真相，不做「表为空则回落 yml」的双头设计，也**不加缓存**：
 * 缓存会把唯一收益抵消掉，而旧价签名打到新道具价就是 -15013。
 * </p>
 * <p>
 * ⚠️ 不提供软删入口：下架走 {@code enabled = 0}。真软删一行会让 uk_code 被幽灵行永久占住，
 * 重新 INSERT 同 code 直接撞键（§8.3 第 5 条）。
 * </p>
 * <p>
 * ⚠️ 本表没有 env 列（2026-10-11 看的后台实页，把前稿的 env 双行设计删了）：道具管理页的「线上版本」
 * 只展示已发布道具、且注明「修改需前往开发版本修改测试后发布」——它是开发版本发布后的<b>只读镜像</b>，
 * 道具 ID 与价格都不能分环境各设一套。既然后台只有一份事实，表里存两行就只剩
 * 「改价漏改一行 → 另一边永远 -15013」的风险。环境差异（现网/沙箱）不再有任何配置项或列，
 * 而是由 profile 现算（见 {@code XpayEnv}：prod→0、其余→1），与套餐表无关。
 * </p>
 * @author 好市民
 */
@Data
@Accessors(chain = true)
@TableName(value = "member_plan", autoResultMap = true)
public class MemberPlanEntity extends BasePO {

    /**
     * 轻享：仅免广告
     */
    public static final int TIER_LITE = 1;
    /**
     * 畅厨：免广告 + 分类菜谱不限
     */
    public static final int TIER_PRO = 2;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 套餐编码，与前端 planCode、§3.2 表首列逐字同名；也是 MP 后台的道具 ID（两者故意取同一个串，
     * 所以「等后台建完再回填」这一步不复存在，也没法把行填错）
     */
    private String code;
    /**
     * 档位：1-轻享 2-畅厨，是权益规则（§6.3）的输入
     */
    private Integer tier;
    /**
     * 周期月数，发放时 plusMonths 用；合法值 {1,3,6,12}，6 为半年付预留位
     */
    private Integer months;
    /**
     * 展示名兼道具名，会原样进入用户账单；不得含「永久/终身」
     */
    private String title;
    /**
     * 售价（分），须与 MP 后台道具价一致，否则 -15013
     */
    private Integer price;
    /**
     * 首单价（分），仅 pro_month 有值；signData.activitySellingPrice 取此列
     */
    private Integer firstPrice;
    /**
     * 相对月付省多少（分），展示用。落库而不是读取时算术是为了同源，NULL 则不显示省额
     */
    private Integer savedPrice;
    /**
     * 微信道具 ID；2026-10-11 起随种子直接带入（与 code 同值），它为空时建单接口直接拒单并 log.error（§7.2），
     * 而删掉回填这一步之后，它只在新增 SKU 忘了填时才会发生
     */
    private String productId;
    /**
     * 是否上架：0-下架（不下发，前端自动少一个胶囊/档位卡，零代码退档）
     */
    private Integer enabled;
    /**
     * 下发顺序，前端周期胶囊按它排
     */
    private Integer sort;
}
