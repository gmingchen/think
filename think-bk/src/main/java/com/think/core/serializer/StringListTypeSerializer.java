package com.think.core.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * 字符串 转 字符串List 序列化
 * @author 好市民
 */
public class StringListTypeSerializer extends JsonSerializer<String> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final TypeReference<List> TYPE_REFERENCE = new TypeReference<List>(){};

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (StringUtils.isNotBlank(value)) {
            gen.writeObject(OBJECT_MAPPER.readValue(value, TYPE_REFERENCE));
        } else {
            gen.writeObject(Collections.emptyList());
        }
    }
}
