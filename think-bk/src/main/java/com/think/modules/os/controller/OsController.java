package com.think.modules.os.controller;

import com.think.common.pojo.Result;
import com.think.modules.os.model.vo.StorageTokenVO;
import com.think.modules.os.service.OsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 对象存储
 * @author 好市民
 */
@RestController
@RequestMapping("/os")
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
