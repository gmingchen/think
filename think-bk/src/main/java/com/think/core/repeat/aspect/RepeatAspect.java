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
        boolean bool = redisUtils.get(key, String.class)
                .map(finalParams::equals)
                .orElse(false);

        if (bool) {
            throw new RunException(ResultCodeEnum.RESUBMIT);
        }

        redisUtils.set(key, params, repeat.value());

        return point.proceed();
    }
}
