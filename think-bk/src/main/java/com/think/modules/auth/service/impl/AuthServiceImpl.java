package com.think.modules.auth.service.impl;

import com.think.common.constant.RedisConstant;
import com.think.core.jwt.utils.JwtUtils;
import com.think.core.redis.utils.RedisUtils;
import com.think.exception.RunException;
import com.think.modules.auth.convert.AuthConvert;
import com.think.modules.auth.model.req.LoginReqVO;
import com.think.modules.auth.model.res.LoginResVO;
import com.think.modules.auth.service.AuthService;
import com.think.modules.auth.model.LoginUser;
import com.think.modules.user.entity.UserEntity;
import com.think.modules.user.model.dto.UserCreateDTO;
import com.think.modules.user.model.dto.UserDTO;
import com.think.modules.user.service.UserLoginLogService;
import com.think.modules.user.service.UserService;
import com.think.modules.wechat.model.dto.AppletLoginDTO;
import com.think.modules.wechat.service.AppletLoginService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;


/**
 * @author 好市民
 */
@Service("authService")
public class AuthServiceImpl implements AuthService {

    @Autowired
    private AppletLoginService appletLoginService;
    @Autowired
    private RedisUtils redisUtils;
    @Autowired
    private UserService userService;
    @Autowired
    private UserLoginLogService userLoginLogService;
    @Autowired
    private JwtUtils jwtUtils;

    @Transactional(rollbackFor = RunException.class)
    @Override
    public LoginResVO login(LoginReqVO reqVO) {
        AppletLoginDTO appletLoginDTO = appletLoginService.login(reqVO.getCode());
        String openid = appletLoginDTO.getOpenid();
        String unionid = appletLoginDTO.getUnionid();
        // 会话密钥：jscode2session 早就返回了它，但此前只用了 openid/unionid，本版把它随 token 存进 Redis，
        // 虚拟支付的用户态签名 signature 全靠它算（docs/v12 §2.3）
        String sessionKey = appletLoginDTO.getSession_key();

        UserDTO userDTO = userService.infoByOpenid(openid);
        Long userId = userDTO != null ? userDTO.getId() : null;
        if (userDTO == null) {
            UserCreateDTO createDTO = new UserCreateDTO()
                    .setOpenid(openid)
                    .setUnionid(unionid)
                    .setInviterId(reqVO.getInviter());
            userId = userService.create(createDTO);
            // 清除邀请人的 Redis 缓存
            if (reqVO.getInviter() != null) {
                redisUtils.delete(RedisConstant.USER + reqVO.getInviter());
            }
        }

        UserEntity userEntity = new UserEntity()
                .setId(userId)
                .setUnionid(unionid);
        userService.updateById(userEntity);
        // 登录时清除本人用户信息缓存，下次鉴权从库重建 LoginUser 快照（login 里的 updateById 绕开了 update() 的删缓存逻辑）
        redisUtils.delete(RedisConstant.USER + userId);
        // v14 登录流水：每次登录落一行，日活 DAU 按本表 COUNT(DISTINCT user_id) 精确统计
        // （同一事务内，登录失败回滚则流水也不落）
        userLoginLogService.record(userId, openid);

        String token = jwtUtils.generate(userId);
        Date expiredAt = jwtUtils.getClaims(token).getExpiration();
        LoginResVO loginResVO = new LoginResVO()
                .setUserId(userId)
                .setToken(token)
                .setExpiredAt(expiredAt.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime())
                .setSessionKey(sessionKey);
        String key = RedisConstant.TOKEN + token;
        long seconds = Duration.between(LocalDateTime.now(), loginResVO.getExpiredAt()).getSeconds();
        redisUtils.set(key, loginResVO, seconds);
        // 存完就擦：Redis 里那一份才是 /member/prepay 算签名的来源，
        // 把它回进登录响应只是多给客户端一条密钥泄露路径，而前端拿不到它也不影响任何功能
        loginResVO.setSessionKey(null);

        return loginResVO;
    }

    @Override
    public LoginUser queryUser(String token) {
        String key = RedisConstant.TOKEN + token;
        return redisUtils.get(key, LoginResVO.class)
                .map(loginResVO -> {
                    String userKey = RedisConstant.USER + loginResVO.getUserId();
                    return redisUtils.get(userKey, LoginUser.class)
                            .orElseGet(() -> {
                                LoginUser loginUser = AuthConvert.INSTANCE.convert(userService.info(loginResVO.getUserId()));
                                long seconds = Duration.between(LocalDateTime.now(), loginResVO.getExpiredAt()).getSeconds();
                                redisUtils.set(userKey, loginUser, seconds);
                                return loginUser;
                            });
                }).orElse(null);
    }

    @Override
    public boolean validateToken(String token) {
        String key = RedisConstant.TOKEN + token;
        return redisUtils.get(key, LoginResVO.class)
                .map(loginResVO -> !LocalDateTime.now().isAfter(loginResVO.getExpiredAt()))
                .orElse(false);
    }
}
