package com.think.common.utils;

import com.think.common.enums.EnvEnum;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * 获取 bean 工具
 * @author 好市民
 */
@Component
public class ApplicationContextUtils implements ApplicationContextAware {
    public static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        ApplicationContextUtils.applicationContext = applicationContext;
    }

    public static <T> T getBean(Class<T> clazz) {
        return applicationContext != null ? applicationContext.getBean(clazz) : null;
    }

    public static <T> T getBean(String bean, Class<T> clazz) {
        return applicationContext != null ? applicationContext.getBean(bean, clazz) : null;
    }

    /**
     * 是否是生产环境
     * @return
     */
    public static boolean isProduction() {
        Environment environment = applicationContext.getEnvironment();
        String[] activeProfiles = environment.getActiveProfiles();
        for (String activeProfile : activeProfiles) {
            if (activeProfile.equals(EnvEnum.PROD.getCode())) {
                return true;
            }
        }
        return false;
    }
}
