package com.think.modules.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.think.common.constant.RedisConstant;
import com.think.common.enums.MediaBizTypeEnum;
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
import com.think.modules.auth.model.LoginUser;
import com.think.modules.wechat.service.AppletMediaCheckService;
import com.think.modules.wechat.service.AppletSecCheckService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
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
    @Autowired
    private AppletSecCheckService appletSecCheckService;
    @Autowired
    private AppletMediaCheckService appletMediaCheckService;

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
        try {
            baseMapper.insert(userEntity);
        } catch (DuplicateKeyException e) {
            // 并发首次登录双建档撞 uk_openid：以先落库者为准，重查返回既有用户，
            // 避免后到请求登录失败（重试才成功）伤害首次体验
            UserDTO existing = infoByOpenid(dto.getOpenid());
            if (existing != null) {
                return existing.getId();
            }
            throw e;
        }
        Long userId = userEntity.getId();
        // 邀请人计数 +1：SQL 原子自增，避免并发邀请下读改写丢失更新
        Long inviterId = dto.getInviterId();
        if (inviterId != null) {
            baseMapper.update(null, new LambdaUpdateWrapper<UserEntity>()
                    .eq(UserEntity::getId, inviterId)
                    .setSql("invite_count = COALESCE(invite_count, 0) + 1"));
        }
        return userId;
    }

    @Override
    public void update(UserUpdateDTO dto) {
        Long userId = SecurityUtils.getLoginUserId();
        // 昵称送审（scene=资料）：命中违规抛异常拒绝保存；置于头像删除前，避免删了旧头像却没保存新资料
        LoginUser loginUser = SecurityUtils.getLoginUser();
        appletSecCheckService.checkText(dto.getNickname(),
                loginUser == null ? null : loginUser.getOpenid(), AppletSecCheckService.SCENE_PROFILE);
        // 对比旧头像，头像变更时删除旧文件
        String newAvatar = null;
        if (dto.getAvatar() != null) {
            UserEntity oldEntity = validateExists(userId);
            if (oldEntity.getAvatar() != null && !oldEntity.getAvatar().equals(dto.getAvatar())) {
                storageService.deleteByUrl(oldEntity.getAvatar());
            }
            if (!dto.getAvatar().isEmpty()) {
                newAvatar = dto.getAvatar();
            }
        }
        UserEntity entity = UserConvert.INSTANCE.convertEntity(dto)
                .setId(userId);
        baseMapper.updateById(entity);
        // 清除用户缓存
        redisUtils.delete(RedisConstant.USER + userId);
        // 头像异步送审（先放行、回调命中再重置）：仅当本次新设了头像才提交
        if (newAvatar != null) {
            appletMediaCheckService.submitImageCheck(MediaBizTypeEnum.USER_AVATAR,
                    userId, null, newAvatar,
                    loginUser == null ? null : loginUser.getOpenid(), AppletSecCheckService.SCENE_PROFILE);
        }
    }
}
