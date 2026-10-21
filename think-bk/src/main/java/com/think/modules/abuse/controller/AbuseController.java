package com.think.modules.abuse.controller;

import com.think.common.pojo.Result;
import com.think.modules.abuse.model.dto.AbuseSubmitDTO;
import com.think.modules.abuse.service.AbuseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 举报
 * <p>
 * MVP 无运营后台：仅落库留痕，处置延后。举报入口面向公开档内容。
 * </p>
 *
 * @author 好市民
 */
@RestController
@RequestMapping("/abuse")
public class AbuseController {

    @Autowired
    private AbuseService abuseService;

    /**
     * 提交举报
     * @param dto 举报参数
     * @return 举报ID
     */
    @PostMapping("/submit")
    public Result<?> submit(@RequestBody @Validated AbuseSubmitDTO dto) {
        return Result.success(abuseService.submit(dto));
    }
}
