package com.think.modules.decision.type;

import com.baomidou.mybatisplus.extension.handlers.AbstractJsonTypeHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.think.modules.decision.model.dto.DecisionOption;
import lombok.SneakyThrows;

import java.util.List;

/**
 * decision.options JSON 列 ↔ List&lt;DecisionOption&gt; 编解码
 *
 * @author 好市民
 */
public class DecisionOptionListTypeHandler extends AbstractJsonTypeHandler<Object> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final TypeReference<List<DecisionOption>> TYPE_REFERENCE = new TypeReference<List<DecisionOption>>() {};

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
