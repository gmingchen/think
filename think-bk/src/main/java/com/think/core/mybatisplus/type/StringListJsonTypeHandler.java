package com.think.core.mybatisplus.type;

import com.baomidou.mybatisplus.extension.handlers.AbstractJsonTypeHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

import java.util.List;

/**
 * 字符串列表 ↔ JSON 数组字符串 编解码（形如 {@code ["a","b"]}）。
 * <p>
 * 仅经 {@code @TableField(typeHandler = StringListJsonTypeHandler.class)} 显式绑定使用，
 * 未做全局注册（不加 {@code @MappedTypes}），故不影响其它 List 字段。
 * 现用于决策单 {@code options}、{@code images} 两列（列可为 VARCHAR 或 JSON，均按 JSON 文本读写）。
 * </p>
 *
 * @author 好市民
 */
public class StringListJsonTypeHandler extends AbstractJsonTypeHandler<Object> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final TypeReference<List<String>> TYPE_REFERENCE = new TypeReference<List<String>>() {};

    @SneakyThrows
    @Override
    protected Object parse(String json) {
        // 列为 NULL/空白时直接返回 null（images 可空），避免 Jackson 对 null 入参抛异常
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        return OBJECT_MAPPER.readValue(json, TYPE_REFERENCE);
    }

    @SneakyThrows
    @Override
    protected String toJson(Object obj) {
        return OBJECT_MAPPER.writeValueAsString(obj);
    }
}
