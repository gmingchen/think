package com.think.modules.tag.controller;

import com.think.common.pojo.Result;
import com.think.modules.tag.service.TagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 标签
 * <p>
 * 预置主数据（圈子雏形），仅暴露读接口。标签维护（增删改）随运营后台二期补充。
 * </p>
 *
 * @author 好市民
 */
@RestController
@RequestMapping("/tag")
public class TagController {

    @Autowired
    private TagService tagService;

    /**
     * 启用中的标签列表
     * @return 标签列表
     */
    @GetMapping("/list")
    public Result<?> list() {
        return Result.success(tagService.listEnabled());
    }
}
