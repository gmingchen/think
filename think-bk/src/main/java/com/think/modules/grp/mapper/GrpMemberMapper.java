package com.think.modules.grp.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.think.core.mybatisplus.mapper.BaseMapperX;
import com.think.modules.grp.entity.GrpMemberEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 群成员
 *
 * @author 好市民
 */
@InterceptorIgnore(dataPermission = "true")
@Mapper
public interface GrpMemberMapper extends BaseMapperX<GrpMemberEntity> {

    /**
     * 忽略逻辑删除查询成员行（用于退群再入群复用唯一键行）
     * @param grpId 群ID
     * @param userId 用户ID
     * @return 成员行（含已软删），无则 null
     */
    @Select("SELECT id, grp_id, user_id, role, deleted FROM grp_member " +
            "WHERE grp_id = #{grpId} AND user_id = #{userId} LIMIT 1")
    GrpMemberEntity selectAny(@Param("grpId") Long grpId, @Param("userId") Long userId);

    /**
     * 复活已软删的成员行（重新入群）
     * @param grpId 群ID
     * @param userId 用户ID
     * @param role 角色
     * @return 影响行数
     */
    @Update("UPDATE grp_member SET deleted = 0, deleted_at = NULL, role = #{role}, updated_at = NOW() " +
            "WHERE grp_id = #{grpId} AND user_id = #{userId}")
    int revive(@Param("grpId") Long grpId, @Param("userId") Long userId, @Param("role") Integer role);
}
