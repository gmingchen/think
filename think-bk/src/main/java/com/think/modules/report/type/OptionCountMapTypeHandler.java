package com.think.modules.report.type;

import com.baomidou.mybatisplus.extension.handlers.AbstractJsonTypeHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

import java.util.Map;

/**
 * report.option_counts JSON 列 ↔ Map&lt;String, Integer&gt; 编解码
 * <p>
 * 形如 {@code {"0":3,"1":5,"2":1}}：下标字符串 -> 有效评议人数。
 * </p>
 *
 * @author 好市民
 */
public class OptionCountMapTypeHandler extends AbstractJsonTypeHandler<Object> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final TypeReference<Map<String, Integer>> TYPE_REFERENCE = new TypeReference<Map<String, Integer>>() {};

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
