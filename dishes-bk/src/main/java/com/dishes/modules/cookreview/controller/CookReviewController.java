package com.dishes.modules.cookreview.controller;

import com.dishes.common.pojo.PageResult;
import com.dishes.common.pojo.Result;
import com.dishes.modules.cookreview.model.dto.CookReviewPageDTO;
import com.dishes.modules.cookreview.model.vo.CookReviewVO;
import com.dishes.modules.cookreview.service.CookReviewService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * 烹饪评价
 * @author 好市民
 */
@RestController
@RequestMapping("/cook-review")
public class CookReviewController {

    @Autowired
    private CookReviewService cookReviewService;

    @GetMapping("/page")
    public Result<PageResult<CookReviewVO>> page(@Validated CookReviewPageDTO dto) {
        return Result.success(cookReviewService.page(dto));
    }
}
