package com.dishes.modules.cookorder.controller;

import com.dishes.common.pojo.PageResult;
import com.dishes.common.pojo.Result;
import com.dishes.modules.cookorder.model.dto.*;
import com.dishes.modules.cookorder.model.vo.CookOrderInfoVO;
import com.dishes.modules.cookorder.model.vo.CookOrderVO;
import com.dishes.modules.cookorder.service.CookOrderService;
import com.dishes.modules.cookreview.model.dto.CookReviewCreateDTO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;

/**
 * 烹饪订单
 * @author 好市民
 */
@RestController
@RequestMapping("/cook-order")
public class CookOrderController {

    @Autowired
    private CookOrderService cookOrderService;

    @GetMapping("/page")
    public Result<PageResult<CookOrderVO>> page(@Validated CookOrderPageDTO dto) {
        return Result.success(cookOrderService.page(dto));
    }

    @GetMapping("/today")
    public Result<List<CookOrderInfoVO>> today(@RequestParam Long kitchenId) {
        return Result.success(cookOrderService.today(kitchenId));
    }

    @GetMapping("/latest")
    public Result<List<CookOrderInfoVO>> latest(@RequestParam Long kitchenId) {
        return Result.success(cookOrderService.latest(kitchenId));
    }

    @GetMapping("/info")
    public Result<CookOrderInfoVO> info(@RequestParam Long id) {
        return Result.success(cookOrderService.info(id));
    }

    @PostMapping("/create")
    public Result<Long> create(@RequestBody @Validated CookOrderCreateDTO dto) {
        return Result.success(cookOrderService.create(dto));
    }

    @PostMapping("/update")
    public Result<?> update(@RequestBody @Validated CookOrderUpdateDTO dto) {
        cookOrderService.update(dto);
        return Result.success();
    }

    @DeleteMapping("/delete")
    public Result<?> delete(@RequestBody @Validated CookOrderDeleteDTO dto) {
        cookOrderService.delete(dto);
        return Result.success();
    }

    @PostMapping("/accept")
    public Result<?> accept(@RequestBody @Validated CookOrderAcceptDTO dto) {
        cookOrderService.accept(dto);
        return Result.success();
    }

    @PostMapping("/serve")
    public Result<?> serve(@RequestBody @Validated CookOrderServeDTO dto) {
        cookOrderService.serve(dto);
        return Result.success();
    }

    @PostMapping("/cancel")
    public Result<?> cancel(@RequestBody @Validated CookOrderCancelDTO dto) {
        cookOrderService.cancel(dto);
        return Result.success();
    }

    @PostMapping("/review")
    public Result<?> review(@RequestBody @Validated CookReviewCreateDTO dto) {
        cookOrderService.review(dto);
        return Result.success();
    }
}
