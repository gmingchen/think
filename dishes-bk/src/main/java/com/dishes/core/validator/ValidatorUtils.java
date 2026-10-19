package com.dishes.core.validator;

import com.dishes.common.enums.ResultCodeEnum;
import com.dishes.core.validator.config.ValidatorConfig;
import com.dishes.exception.RunException;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.util.Set;

/**
 * hibernate-validator校验工具类
 * @author 好市民
 */
public class ValidatorUtils {

    private static Validator validator;

    static {
        validator = ValidatorConfig.getValidator();
    }

    /**
     * 功能描述:校验注解参数
     */
    public static <T> void validate(T object, Class<?>... groups) throws RunException {
        Set<ConstraintViolation<Object>> constraintViolations = validator.validate(object, groups);
        if (!constraintViolations.isEmpty()) {
            StringBuilder message = new StringBuilder();
            message.append(ResultCodeEnum.VERIFICATION_ERROR.getMessage());
            String comma = "";
            for (ConstraintViolation<Object> constraint : constraintViolations) {
                message.append(comma).append(constraint.getPropertyPath()+ "-" + constraint.getMessage());
                comma = ",";
            }
            throw new RunException(ResultCodeEnum.VERIFICATION_ERROR.getCode(), message.toString());
        }
    }
}
