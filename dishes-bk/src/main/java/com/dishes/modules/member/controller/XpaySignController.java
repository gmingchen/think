package com.dishes.modules.member.controller;

import com.dishes.common.pojo.Result;
import com.dishes.modules.member.model.dto.MemberPaySignDTO;
import com.dishes.modules.member.service.MemberService;
import com.dishes.modules.wechat.xpay.model.vo.XpayPrepayVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 取支付签名（docs/v12 §7.2 第二段）
 * <p>
 * ⚠️ URL 挂在 {@code /xpay} 下而实现留在会员模块，这是 2026-10-11 定下的形态（§16 第 13 条）。
 * 之所以用通道前缀：本接口产出的是「一份能直提 wx.requestVirtualPayment 的参数」，
 * 它与卖什么无关，第二个虚拟产品也会调同名能力；之所以代码不搬进 {@code modules/wechat/xpay}：
 * 它要回读 {@code member_order} 那一行快照并做「归属 + 状态 + 有效期 + 环境」四道锁，
 * 搬过去就等于让支付通道反过来认识会员表——依赖方向得倒过来（§9.1）。
 * </p>
 * <p>
 * 所以本类的定位是「通道命名的业务接口」：它只有一行转发，业务判断全在
 * {@code MemberServiceImpl#paySign}，通道能力全在 {@code XpaySignService}。
 * </p>
 * <p>
 * ⚠️ 这里<b>不加</b> {@code @Repeat}：本接口存在的理由就是「签名失败可以重签同一单号」，
 * 上幂等锁会把这条路堵死（拆分前只能重新下单，§12.1 的 -15007 就靠这一步消化）。
 * 而它和 {@code /member/order/create} 一样走默认鉴权，不在 {@code SecurityConfig} 的 permitAll 白名单里。
 * </p>
 *
 * @author 好市民
 */
@RestController
@RequestMapping("/xpay")
public class XpaySignController {

    @Autowired
    private MemberService memberService;

    /**
     * 按业务单号换一份支付签名
     *
     * @param dto 业务单号（须是本人、仍待支付且未过 expire_at 的单）
     * @return 前端透传给 wx.requestVirtualPayment 的出参
     */
    @PostMapping("/sign")
    public Result<XpayPrepayVO> sign(@RequestBody @Validated MemberPaySignDTO dto) {
        return Result.success(memberService.paySign(dto));
    }
}
