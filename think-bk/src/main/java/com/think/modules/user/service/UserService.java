package com.think.modules.user.service;

import com.think.core.mybatisplus.service.IServiceX;
import com.think.modules.user.entity.UserEntity;
import com.think.modules.user.model.dto.UserCreateDTO;
import com.think.modules.user.model.dto.UserDTO;
import com.think.modules.user.model.dto.UserUpdateDTO;

/**
 * 用户
 * @author 拖孩
 * @email 1240235512@qq.com
 * @date 2023-07-22 17:36:32
 */
public interface UserService extends IServiceX<UserEntity> {

    /**
     * 信息
     * @param id ID
     * @return 信息
     */
    UserDTO info(Long id);

    /**
     * 通过openid查询用户
     * @param openid 微信Openid
     * @return
     */
    UserDTO infoByOpenid(String openid);

    /**
     * 用户注册
     * @param dto 参数
     * @return 用户ID
     */
    Long create(UserCreateDTO dto);

    /**
     * 编辑用户信息
     * @param dto 参数
     */
    void update(UserUpdateDTO dto);

    /**
     * 生成带当前用户 inviterId 参数的小程序码
     * @param page 扫码落地页路径
     * @param userId 邀请人用户 ID
     * @return 小程序码图片 base64 dataURL
     */
    String generateQrcode(String page, Long userId);
}

