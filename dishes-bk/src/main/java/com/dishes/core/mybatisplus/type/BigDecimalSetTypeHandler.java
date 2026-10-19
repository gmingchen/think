package com.dishes.core.mybatisplus.type;

import com.baomidou.mybatisplus.extension.handlers.AbstractJsonTypeHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

import java.math.BigDecimal;
import java.util.Set;

/**
 * @author 好市民
 */
public class BigDecimalSetTypeHandler extends AbstractJsonTypeHandler<Object> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final TypeReference<Set<BigDecimal>> TYPE_REFERENCE = new TypeReference<Set<BigDecimal>>(){};

    @SneakyThrows
    @Override
    protected Object parse(String json) {
        return OBJECT_MAPPER.readValue(json, TYPE_REFERENCE);
    }

    @SneakyThrows
    @Override
    protected String toJson(Object obj) {
        return OBJECT_MAPPER.writeValueAsString(obj);
    }
}
