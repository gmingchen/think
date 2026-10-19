package com.dishes.modules.kitchen.mapper;

import com.dishes.core.mybatisplus.mapper.BaseMapperX;
import com.dishes.modules.kitchen.entity.KitchenUserEntity;
import com.dishes.modules.kitchen.model.vo.KitchenUserVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 厨房用户关联
 * @author 好市民
 */
@Mapper
public interface KitchenUserMapper extends BaseMapperX<KitchenUserEntity> {

    List<KitchenUserVO> selectMemberList(@Param("kitchenId") Long kitchenId);

    /**
     * 查询成员记录（含逻辑删除的旧行，绕过 @TableLogic 过滤）
     * 用于加入时撞唯一索引 uk_kitchen_user 前的查重
     * @param kitchenId 厨房ID
     * @param userId 用户ID
     * @return 成员记录，不存在则为 null
     */
    KitchenUserEntity selectIncludeDeleted(@Param("kitchenId") Long kitchenId, @Param("userId") Long userId);

    /**
     * 激活逻辑删除的旧成员记录（重新加入），字段赋值绕过 @TableLogic，需在 XML 手动维护
     * 带 deleted = 1 条件做并发兜底：后到请求更新 0 行，自然落入「已是成员」分支
     * @param id 旧记录ID
     * @param role 本轮加入的角色
     * @param inviterId 本轮邀请人用户ID
     * @param now 当前时间
     * @return 影响行数，0 表示旧行已被激活
     */
    int reactivateById(@Param("id") Long id, @Param("role") Integer role,
                       @Param("inviterId") Long inviterId, @Param("now") LocalDateTime now);
}
