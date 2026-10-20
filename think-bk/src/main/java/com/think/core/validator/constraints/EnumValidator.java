package com.think.core.validator.constraints;

import com.think.common.enums.EnumIntArray;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author 好市民
 */
public class EnumValidator implements ConstraintValidator<Enum, Integer> {

    private List<Integer> values;

    @Override
    public void initialize(Enum constraintAnnotation) {
        EnumIntArray[] enumConstants = constraintAnnotation.value().getEnumConstants();
        if (enumConstants.length ==0) {
            values = Collections.emptyList();
        } else {
            values = Arrays.stream(enumConstants[0].array()).boxed().collect(Collectors.toList());
        }
    }

    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        if (values.contains(value)) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(
                context.getDefaultConstraintMessageTemplate()
                        .replaceAll("\\{scope}", values.toString())
        ).addConstraintViolation();

        return false;
    }
}
