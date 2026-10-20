package com.think.modules.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.think.common.constant.RedisConstant;
import com.think.core.mybatisplus.service.ServiceImplX;
import com.think.core.security.utils.SecurityUtils;
import com.think.exception.RunException;
import com.think.modules.os.service.StorageService;
import com.think.modules.user.convert.UserConvert;
import com.think.modules.user.entity.UserEntity;
import com.think.modules.user.mapper.UserMapper;
import com.think.modules.user.model.dto.UserCreateDTO;
import com.think.modules.user.model.dto.UserDTO;
import com.think.modules.user.model.dto.UserUpdateDTO;
import com.think.modules.user.service.UserService;
import com.think.modules.wechat.service.AppletCodeAndLinkService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.think.core.redis.utils.RedisUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Optional;

/**
 * 用户
 * @author 拖孩
 * @email 1240235512@qq.com
 * @date 2023-07-22 17:36:32
 */
@Service("userService")
public class UserServiceImpl extends ServiceImplX<UserMapper, UserEntity> implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    private RedisUtils redisUtils;
    @Autowired
    private StorageService storageService;
    @Autowired
    private AppletCodeAndLinkService appletCodeAndLinkService;

    @Override
    public UserDTO info(Long id) {
        return Optional.ofNullable(baseMapper.selectById(id))
                .map(UserConvert.INSTANCE::convert)
                .orElse(null);
    }

    @Override
    public UserDTO infoByOpenid(String openid) {
        LambdaQueryWrapper<UserEntity> wrapper = new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getOpenid, openid);
        return Optional.ofNullable(baseMapper.selectOne(wrapper))
                .map(UserConvert.INSTANCE::convert)
                .orElse(null);
    }

    @Override
    public Long create(UserCreateDTO dto) {
        UserEntity userEntity = new UserEntity()
                .setOpenid(dto.getOpenid())
                .setUnionid(dto.getUnionid())
                .setNickname("厨师长-" + LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().getEpochSecond())
                .setInviterId(dto.getInviterId());
        baseMapper.insert(userEntity);
        Long userId = userEntity.getId();
        // 邀请人计数 +1
        Long inviterId = dto.getInviterId();
        if (inviterId != null) {
            UserEntity inviterEntity = getById(inviterId);
            if (inviterEntity != null) {
                int currentCount = inviterEntity.getInviteCount() != null ? inviterEntity.getInviteCount() : 0;
                UserEntity updateEntity = new UserEntity()
                        .setId(inviterId)
                        .setInviteCount(currentCount + 1);
                updateById(updateEntity);
            }
        }
        return userId;
    }

    @Override
    public void update(UserUpdateDTO dto) {
        Long userId = SecurityUtils.getLoginUserId();
        // 对比旧头像，头像变更时删除旧文件
        if (dto.getAvatar() != null) {
            UserEntity oldEntity = validateExists(userId);
            if (oldEntity.getAvatar() != null && !oldEntity.getAvatar().equals(dto.getAvatar())) {
                storageService.deleteByUrl(oldEntity.getAvatar());
            }
        }
        UserEntity entity = UserConvert.INSTANCE.convertEntity(dto)
                .setId(userId);
        baseMapper.updateById(entity);
        // 清除用户缓存
        redisUtils.delete(RedisConstant.USER + userId);
    }

    @Override
    public String generateQrcode(String page, Long userId) {
        // scene ≤ 32 字符：仅带邀请人 ID，与全站现有 inviterId 归因机制对齐
        byte[] bytes = appletCodeAndLinkService.getUnlimitedQrcodeByte("inviterId=" + userId, page);
        if (isWechatError(bytes)) {
            String body = bytes == null ? "" : new String(bytes, StandardCharsets.UTF_8);
            log.warn("小程序码生成失败, userId={}, page={}, resp={}", userId, page, body);
            throw new RunException("小程序码生成失败");
        }
        // 直接以 dataURL 返回，不落库、不传七牛
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * 微信 getUnlimited 成功返回 PNG 二进制、失败返回 JSON 错误体，按首字节区分
     */
    private boolean isWechatError(byte[] bytes) {
        return bytes == null || bytes.length < 100 || bytes[0] == '{';
    }
}
