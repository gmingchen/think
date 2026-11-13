package com.think.modules.oss.controller;

import com.think.common.pojo.Result;
import com.think.modules.oss.model.vo.StorageTokenVO;
import com.think.modules.oss.service.OsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 对象存储
 * @author 好市民
 */
@RestController
@RequestMapping("/oss")
public class OsController {

    @Autowired
    private OsService osService;

    /**
     * 获取上传凭证
     * @return 上传凭证
     */
    @GetMapping("/uploadToken")
    public Result<StorageTokenVO> getUploadToken() {
        return Result.success(osService.getUploadToken());
    }
}
