package com.think.modules.grp.service;

import com.think.core.mybatisplus.service.IServiceX;
import com.think.modules.grp.entity.GrpMemberEntity;
import com.think.modules.grp.model.dto.GrpMemberDTO;

import java.util.List;

/**
 * 群成员
 *
 * @author 好市民
 */
public interface GrpMemberService extends IServiceX<GrpMemberEntity> {

    /**
     * 查询群成员列表
     * @param grpId 群ID
     * @return 成员列表
     */
    List<GrpMemberDTO> listByGrp(Long grpId);

    /**
     * 查询某用户所在的全部群ID
     * @param userId 用户ID
     * @return 群ID列表
     */
    List<Long> listGrpIdByUser(Long userId);

    /**
     * 添加群成员（复用退群软删行，幂等）
     * @param grpId 群ID
     * @param userId 用户ID
     * @param role 角色（GrpMemberRoleEnum）
     */
    void addMember(Long grpId, Long userId, Integer role);

    /**
     * 移除群成员（逻辑删除）
     * @param grpId 群ID
     * @param userId 用户ID
     */
    void removeMember(Long grpId, Long userId);

    /**
     * 判断用户是否为群成员
     * @param grpId 群ID
     * @param userId 用户ID
     * @return 是否成员
     */
    boolean isMember(Long grpId, Long userId);

    /**
     * 查询用户在群内的角色
     * @param grpId 群ID
     * @param userId 用户ID
     * @return 角色，非成员返回 null
     */
    Integer getRole(Long grpId, Long userId);

    /**
     * 统计群成员数（实时）
     * @param grpId 群ID
     * @return 成员数
     */
    int countByGrp(Long grpId);
}
