package com.think.modules.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.think.common.constant.RedisConstant;
import com.think.core.mybatisplus.service.ServiceImplX;
import com.think.core.security.utils.SecurityUtils;
import com.think.modules.os.service.StorageService;
import com.think.modules.user.convert.UserConvert;
import com.think.modules.user.entity.UserEntity;
import com.think.modules.user.mapper.UserMapper;
import com.think.modules.user.model.dto.UserCreateDTO;
import com.think.modules.user.model.dto.UserDTO;
import com.think.modules.user.model.dto.UserUpdateDTO;
import com.think.modules.user.service.UserService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.think.core.redis.utils.RedisUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
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
                .setNickname("决策者-" + LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().getEpochSecond())
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
}
