package com.think.modules.review.controller;

import com.think.common.pojo.Result;
import com.think.modules.review.model.dto.ReviewPageDTO;
import com.think.modules.review.model.dto.ReviewSubmitDTO;
import com.think.modules.review.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 评议
 * <p>
 * 仅暴露读接口（按决策单分页查询评议）。发表/修改评议涉及禁自评、先评后看、审核闸门等规则，待业务阶段补充。
 * </p>
 *
 * @author 好市民
 */
@RestController
@RequestMapping("/review")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    /**
     * 某决策单下的评议列表（分页）
     * @param dto 分页参数
     * @return 分页结果
     */
    @PostMapping("/page")
    public Result<?> page(@RequestBody @Validated ReviewPageDTO dto) {
        return Result.success(reviewService.pageByDecision(dto));
    }

    /**
     * 提交/修改评议
     */
    @PostMapping("/submit")
    public Result<?> submit(@RequestBody @Validated ReviewSubmitDTO dto) {
        return Result.success(reviewService.submit(dto));
    }
}
