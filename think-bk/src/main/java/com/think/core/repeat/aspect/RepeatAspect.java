package com.think.core.repeat.aspect;

import com.think.common.constant.RedisConstant;
import com.think.common.enums.ResultCodeEnum;
import com.think.common.utils.HttpContextUtils;
import com.think.core.redis.utils.RedisUtils;
import com.think.common.utils.Utils;
import com.think.core.repeat.annotation.Repeat;
import com.think.core.security.utils.SecurityUtils;
import com.think.exception.RunException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import org.springframework.beans.factory.annotation.Autowired;
import java.lang.reflect.Method;

/**
 * 防止重复提交 切面处理类
 * @author 好市民
 */
@Aspect
@Component
public class RepeatAspect {

    @Autowired
    private RedisUtils redisUtils;

    @Pointcut("@annotation(com.think.core.repeat.annotation.Repeat)")
    public void repeatPointCut() {
    }

    @Around("repeatPointCut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        //获取防重复提交注解
        Repeat repeat = method.getAnnotation(Repeat.class);
        // 登录凭证
        String token = SecurityUtils.getToken();
        // 请求URL
        String url = HttpContextUtils.getRequestUrl();
        // 存储Redis的Key
        String key = RedisConstant.REPEAT + url + "-" + token;
        // 获取请求参数
        String params = Utils.getParams(point.getArgs());
        if (params == null) {
            params = key;
        }

        String finalParams = params;
        // setIfAbsent 原子判重：原 get+set 两步在并发下双双读到空、双双放行，防重窗口形同虚设。
        // 未抢占到时再回读比对：同参数=重复提交拒绝；不同参数=新内容放行（保持原语义）
        boolean acquired = Boolean.TRUE.equals(redisUtils.setIfAbsent(key, finalParams, repeat.value()));
        if (acquired) {
            return point.proceed();
        }
        boolean same = redisUtils.get(key, String.class)
                .map(finalParams::equals)
                .orElse(false);
        if (same) {
            throw new RunException(ResultCodeEnum.RESUBMIT);
        }
        return point.proceed();
    }
}
