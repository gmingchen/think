package com.dishes.modules.member.mapper;

import com.dishes.core.mybatisplus.mapper.BaseMapperX;
import com.dishes.modules.member.entity.MemberEntity;
import com.dishes.modules.member.model.vo.MemberExpireNoticeVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会员状态
 * @author 好市民
 */
@Mapper
public interface MemberMapper extends BaseMapperX<MemberEntity> {

    /**
     * 查「到期时间落在 [start, end) 且仍在会员期」的人，供到期提醒定时任务发送订阅消息。
     * <p>
     * 关联 user 取 openid：定时任务没有登录态，拿不到登录快照里的 openid，只能按 user_id 回表。
     * ⚠️ 原生 SQL 绕过了 {@code @TableLogic}，两张表的 {@code deleted = 0} 都必须手写，否则会命中软删行。
     * {@code tier > 0} 只是粗筛（库里 tier 语义是「最近一次发放档位」，过期行的 tier 不会归零），
     * 真正保证「在期」的是 {@code expire_at} 落在提醒窗口内这一条件。
     * </p>
     *
     * @param start 窗口起点（含），一般是「今天 + 提前天数」当天的 00:00
     * @param end   窗口终点（不含），即 start 的次日 00:00
     * @return 待提醒行（openid + 档位 + 到期时间）
     */
    @Select("SELECT um.user_id AS userId, u.openid AS openid, um.tier AS tier, um.expire_at AS expireAt "
            + "FROM member um "
            + "JOIN user u ON u.id = um.user_id AND u.deleted = 0 "
            + "WHERE um.deleted = 0 AND um.tier > 0 "
            + "AND um.expire_at >= #{start} AND um.expire_at < #{end}")
    List<MemberExpireNoticeVO> selectExpiringForNotice(@Param("start") LocalDateTime start,
                                                       @Param("end") LocalDateTime end);
}
