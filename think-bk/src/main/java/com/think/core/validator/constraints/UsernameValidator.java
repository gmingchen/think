package com.think.core.validator.constraints;

import com.think.common.utils.RegexUtils;
import org.apache.commons.lang3.StringUtils;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * @author 好市民
 */
public class UsernameValidator implements ConstraintValidator<Username, String> {

    @Override
    public void initialize(Username constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (StringUtils.isBlank(value)) {
            return true;
        }
        return RegexUtils.isUsername(value);
    }
}
