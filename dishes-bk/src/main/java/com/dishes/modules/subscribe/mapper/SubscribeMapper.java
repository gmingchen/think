package com.dishes.modules.subscribe.mapper;

import com.dishes.core.mybatisplus.mapper.BaseMapperX;
import com.dishes.modules.subscribe.entity.SubscribeEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 订阅消息配额记录
 * <p>
 * 两个写操作都做成<b>单条原子 SQL</b>而非「先查再改」：sync 可能因用户反复授权/前端重试并发到达，
 * consume 由定时任务与未来其它发送方并发调用。靠 {@code uk_user_template} 与 {@code remaining_count > 0}
 * 条件保证幂等与不透支，原生 SQL 绕过 {@code @TableLogic}，故 consume 手写 {@code deleted = 0}。
 * </p>
 * @author 好市民
 */
@Mapper
public interface SubscribeMapper extends BaseMapperX<SubscribeEntity> {

    /**
     * 授权同步：有则累加、无则插入（靠唯一键 {@code uk_user_template} 幂等）。
     * created_at / updated_at 走库默认值，冲突时把 updated_at 顶到当前。
     *
     * @param userId     用户ID
     * @param templateId 微信模板ID
     * @param count      本次授权增加的可发次数（通常为 1）
     * @return 受影响行数（MySQL upsert：插入为 1、更新为 2，仅用于判是否执行成功，不用于业务分支）
     */
    @Insert("INSERT INTO subscribe (user_id, template_id, remaining_count) "
            + "VALUES (#{userId}, #{templateId}, #{count}) "
            + "ON DUPLICATE KEY UPDATE remaining_count = remaining_count + #{count}, updated_at = NOW()")
    int upsertAdd(@Param("userId") Long userId,
                  @Param("templateId") String templateId,
                  @Param("count") int count);

    /**
     * 发送前扣配额：仅当还剩次数时才扣（CAS，防并发透支成负数）。
     *
     * @return 1-扣到了一次发送权；0-无配额（调用方应静默跳过，不发送也不报错）
     */
    @Update("UPDATE subscribe SET remaining_count = remaining_count - 1, updated_at = NOW() "
            + "WHERE user_id = #{userId} AND template_id = #{templateId} AND remaining_count > 0 AND deleted = 0")
    int casConsume(@Param("userId") Long userId,
                   @Param("templateId") String templateId);
}
