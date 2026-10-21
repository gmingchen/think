package com.think.modules.recap.controller;

import com.think.common.pojo.Result;
import com.think.modules.recap.model.dto.RecapSaveDTO;
import com.think.modules.recap.service.RecapService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 决策复盘
 *
 * @author 好市民
 */
@RestController
@RequestMapping("/recap")
public class RecapController {

    @Autowired
    private RecapService recapService;

    /**
     * 回填/修改复盘
     */
    @PostMapping("/save")
    public Result<?> save(@RequestBody @Validated RecapSaveDTO dto) {
        return Result.success(recapService.save(dto));
    }

    /**
     * 按决策单查询复盘
     */
    @GetMapping("/detail")
    public Result<?> detail(@RequestParam Long decisionId) {
        return Result.success(recapService.infoByDecision(decisionId));
    }
}
