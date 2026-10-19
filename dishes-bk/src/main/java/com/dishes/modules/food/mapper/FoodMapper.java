package com.dishes.modules.food.mapper;

import com.dishes.core.mybatisplus.mapper.BaseMapperX;
import com.dishes.modules.food.entity.FoodEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 食材
 * @author 好市民
 */
@Mapper
public interface FoodMapper extends BaseMapperX<FoodEntity> {

    /**
     * 查询厨房全部食材（含软删幽灵行），绕过 @TableLogic 过滤供名字匹配使用。
     * 唯一索引 uk_kitchen_name 覆盖软删行，若只匹配活跃行会导致"删过又再次录入"新建时撞键。
     */
    @Select("SELECT * FROM food WHERE kitchen_id = #{kitchenId} ORDER BY usage_count DESC")
    List<FoodEntity> selectAllIncludeDeleted(@Param("kitchenId") Long kitchenId);

    /**
     * 复活软删食材（撤销软删并使用次数 +1）；普通 update 会被 @TableLogic 过滤掉软删行，故走原生 SQL。
     */
    @Update("UPDATE food SET deleted = 0, usage_count = usage_count + 1, updated_at = NOW() WHERE id = #{id}")
    int reviveById(@Param("id") Long id);
}
