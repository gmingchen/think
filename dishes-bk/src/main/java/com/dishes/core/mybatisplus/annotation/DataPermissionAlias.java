package com.dishes.core.mybatisplus.annotation;

import java.lang.annotation.*;

/**
 * 数据权限 表别名注解 主要用于多表查询
 *
 * @author 好市民
 * @email 1240235512@qq.com
 * @date 1995-08-30 00:00:00
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE,ElementType.METHOD})
public @interface DataPermissionAlias {
	String value() default "";
}
