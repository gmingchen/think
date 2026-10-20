package com.think.core.mybatisplus.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.think.common.po.BasePO;
import org.apache.ibatis.reflection.MetaObject;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 自动填充
 * @author 好市民
 */
public class AutoFillHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        if (Objects.nonNull(metaObject)) {
            // 基础字段自动填充
            if (metaObject.getOriginalObject() instanceof BasePO) {
                BasePO basePO = (BasePO) metaObject.getOriginalObject();
                LocalDateTime now = LocalDateTime.now();
                if (Objects.isNull(basePO.getCreatedAt())) {
                    basePO.setCreatedAt(now);
                }
                if (Objects.isNull(basePO.getUpdatedAt())) {
                    basePO.setUpdatedAt(now);
                }
            }
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        // 如果时间为空 则 自动填充当前时间
        Object updateAt = getFieldValByName("updatedAt", metaObject);
        if (Objects.isNull(updateAt)) {
            setFieldValByName("updatedAt", LocalDateTime.now(), metaObject);
        }
    }
}
