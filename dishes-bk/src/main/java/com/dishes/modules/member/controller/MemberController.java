package com.dishes.modules.member.controller;

import com.dishes.common.pojo.Result;
import com.dishes.core.repeat.annotation.Repeat;
import com.dishes.modules.member.model.dto.MemberConfirmDTO;
import com.dishes.modules.member.model.dto.MemberPrepayDTO;
import com.dishes.modules.member.model.vo.MemberConfirmVO;
import com.dishes.modules.member.model.vo.MemberInfoVO;
import com.dishes.modules.member.model.vo.MemberOrderVO;
import com.dishes.modules.member.service.MemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 会员（docs/v12 §7.1 / §7.2 / §7.3）
 * <p>
 * 本类三个接口都要登录态：info 出个性化状态与首单资格，order/create 直接绑着单号与用户，
 * confirm 更是只能给下单人查，所以它们都不在 {@code SecurityConfig} 的 permitAll 白名单里，走默认鉴权即可。
 * </p>
 * <p>
 * ⚠️ 下单在 2026-10-11 拆成了两步：先本类的 {@code /member/order/create} 落一条待支付快照拿到单号，
 * 再由 {@code /xpay/sign}（见 {@link XpaySignController}）按单号换签名。拆前是一个 {@code /member/prepay}
 * 包办两事，于是签名失败只能重新下单，每重试一次多留一条待支付单（§7.2）。
 * </p>
 *
 * @author 好市民
 */
@RestController
@RequestMapping("/member")
public class MemberController {

    @Autowired
    private MemberService memberService;

    /**
     * 会员状态 + 在售套餐 + 卡片文案 + 开通记录
     *
     * @return 会员信息
     */
    @GetMapping("/info")
    public Result<MemberInfoVO> info() {
        return Result.success(memberService.info());
    }

    /**
     * 创建订单：只落一条待支付快照，不碰签名
     * <p>
     * 加 {@code @Repeat}：连点两下会生成两条待支付单，虽然只有实际支付的那条会发放，
     * 但两笔都会被算成「首单」（同一用户两笔未支付单都判首单，§7.2 要点 3 已定接受，不为此加锁）。
     * </p>
     *
     * @param dto 套餐编码
     * @return 单号与本单金额快照
     */
    @Repeat
    @PostMapping("/order/create")
    public Result<MemberOrderVO> createOrder(@RequestBody @Validated MemberPrepayDTO dto) {
        return Result.success(memberService.createOrder(dto));
    }

    /**
     * 查单兜底：前端支付成功回调后轮询本接口确认权益是否到账
     *
     * @param dto 业务单号
     * @return 是否已到账与订单当前状态
     */
    @PostMapping("/confirm")
    public Result<MemberConfirmVO> confirm(@RequestBody @Validated MemberConfirmDTO dto) {
        return Result.success(memberService.confirm(dto));
    }
}
