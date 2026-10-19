package com.dishes.modules.kitchen.service;

import com.dishes.core.mybatisplus.service.IServiceX;
import com.dishes.modules.kitchen.entity.KitchenUserEntity;
import com.dishes.modules.kitchen.model.dto.KitchenUserCreateDTO;
import com.dishes.modules.kitchen.model.dto.KitchenUserDeleteDTO;
import com.dishes.modules.kitchen.model.dto.KitchenUserInviteDTO;
import com.dishes.modules.kitchen.model.dto.KitchenUserJoinDTO;
import com.dishes.modules.kitchen.model.dto.KitchenUserUpdateRoleDTO;
import com.dishes.modules.kitchen.model.vo.KitchenInviteInfoVO;
import com.dishes.modules.kitchen.model.vo.KitchenUserVO;

import java.util.List;
import java.util.Set;

/**
 * 厨房用户关联
 * @author 好市民
 */
public interface KitchenUserService extends IServiceX<KitchenUserEntity> {
  
    /**
     * 查询厨房成员列表
     * @param kitchenId 厨房ID
     * @return 成员列表
     */
    List<KitchenUserVO> list(Long kitchenId);

    /**
     * 新增厨房成员
     * @param dto 成员信息
     */
    void create(KitchenUserCreateDTO dto);

    /**
     * 修改成员角色
     * @param dto 参数
     */
    void updateRole(KitchenUserUpdateRoleDTO dto);

    /**
     * 删除厨房成员
     * @param dto 参数
     */
    void delete(KitchenUserDeleteDTO dto);

    /**
     * 生成邀请码
     * @param dto 参数
     * @return 邀请码
     */
    String invite(KitchenUserInviteDTO dto);

    /**
     * 查询邀请信息
     * @param kitchenId 厨房ID
     * @param code 邀请码
     * @return 邀请信息
     */
    KitchenInviteInfoVO getInviteInfo(Long kitchenId, String code);

    /**
     * 通过邀请码加入厨房
     * @param dto 参数
     */
    void join(KitchenUserJoinDTO dto);

    /**
     * 退出厨房
     * @param kitchenId 厨房ID
     */
    void leave(Long kitchenId);

    /**
     * 校验是否为厨房成员，不是则抛异常
     * @param kitchenId 厨房ID
     * @param userId 用户ID
     * @return 成员实体
     */
    KitchenUserEntity checkMember(Long kitchenId, Long userId);

    /**
     * 查询用户已加入的全部厨房ID（批量验权场景，替代逐单 checkMember 的 1+N 查询）
     * @param userId 用户ID
     * @return 厨房ID集合
     */
    Set<Long> listKitchenIdsByUserId(Long userId);

    /**
     * 校验编辑权限（仅厨师长/管厨可操作）
     * @param kitchenId 厨房ID
     */
    KitchenUserEntity checkPermission(Long kitchenId);

}
