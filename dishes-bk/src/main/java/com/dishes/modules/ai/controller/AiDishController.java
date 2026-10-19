package com.dishes.modules.ai.controller;

import com.dishes.common.pojo.Result;
import com.dishes.modules.ai.model.dto.AiAdoptDTO;
import com.dishes.modules.ai.model.dto.AiImageGenDTO;
import com.dishes.modules.ai.model.dto.AiParseCommitDTO;
import com.dishes.modules.ai.model.dto.AiParseDTO;
import com.dishes.modules.ai.model.dto.AiRecommendDTO;
import com.dishes.modules.ai.model.vo.AiImageVO;
import com.dishes.modules.ai.model.vo.AiParseVO;
import com.dishes.modules.ai.model.vo.AiRecommendVO;
import com.dishes.modules.ai.service.AiDishService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * AI 荐菜
 * @author 好市民
 */
@RestController
@RequestMapping("/ai/dish")
public class AiDishController {

    @Autowired
    private AiDishService aiDishService;

    @PostMapping("/recommend")
    public Result<AiRecommendVO> recommend(@RequestBody @Validated AiRecommendDTO dto) {
        return Result.success(aiDishService.recommend(dto));
    }

    @PostMapping("/adopt")
    public Result<Long> adopt(@RequestBody @Validated AiAdoptDTO dto) {
        return Result.success(aiDishService.adopt(dto));
    }

    @GetMapping("/quota")
    public Result<Integer> quota() {
        return Result.success(aiDishService.remainCount());
    }

    @GetMapping("/parse/quota")
    public Result<Integer> parseQuota() {
        return Result.success(aiDishService.parseRemainCount());
    }

    @PostMapping("/parse")
    public Result<AiParseVO> parse(@RequestBody @Validated AiParseDTO dto) {
        return Result.success(aiDishService.parse(dto));
    }

    @PostMapping("/commit")
    public Result<AiParseVO> commit(@RequestBody @Validated AiParseCommitDTO dto) {
        return Result.success(aiDishService.commit(dto));
    }

    @GetMapping("/image/quota")
    public Result<Integer> imageQuota() {
        return Result.success(aiDishService.imageRemainCount());
    }

    @PostMapping("/image")
    public Result<AiImageVO> image(@RequestBody @Validated AiImageGenDTO dto) {
        return Result.success(aiDishService.genImage(dto));
    }
}
