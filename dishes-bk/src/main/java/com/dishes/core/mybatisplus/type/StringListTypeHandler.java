package com.dishes.core.mybatisplus.type;

import com.baomidou.mybatisplus.extension.handlers.AbstractJsonTypeHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

import java.util.List;

/**
 * @author 好市民
 */
public class StringListTypeHandler extends AbstractJsonTypeHandler<Object> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final TypeReference<List<String>> TYPE_REFERENCE = new TypeReference<List<String>>(){};

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
