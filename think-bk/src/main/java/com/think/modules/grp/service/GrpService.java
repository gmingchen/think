package com.think.modules.grp.service;

import com.think.common.pojo.PageResult;
import com.think.core.mybatisplus.service.IServiceX;
import com.think.modules.grp.entity.GrpEntity;
import com.think.modules.grp.model.dto.GrpCreateDTO;
import com.think.modules.grp.model.dto.GrpIdDTO;
import com.think.modules.grp.model.dto.GrpJoinDTO;
import com.think.modules.grp.model.dto.GrpPageDTO;
import com.think.modules.grp.model.dto.GrpRemoveMemberDTO;
import com.think.modules.grp.model.dto.GrpUpdateDTO;
import com.think.modules.grp.model.vo.GrpDetailVO;
import com.think.modules.grp.model.vo.GrpMemberVO;

import java.util.List;

/**
 * 群组
 *
 * @author 好市民
 */
public interface GrpService extends IServiceX<GrpEntity> {

    /**
     * 分页查询当前用户所在/所建的群
     * @param dto 分页参数
     * @return 分页结果
     */
    PageResult<GrpDetailVO> pageMy(GrpPageDTO dto);

    /**
     * 建群（当前用户为群主）
     * @param dto 参数
     * @return 群ID
     */
    Long create(GrpCreateDTO dto);

    /**
     * 群详情（仅成员可见）
     * @param grpId 群ID
     * @return 详情
     */
    GrpDetailVO detail(Long grpId);

    /**
     * 修改群信息（仅群主）
     * @param dto 参数
     */
    void update(GrpUpdateDTO dto);

    /**
     * 解散群（仅群主，级联关闭群内决策单）
     * @param dto 参数
     */
    void dissolve(GrpIdDTO dto);

    /**
     * 生成邀请码（成员可用，短时效存 Redis，不落库）
     * @param dto 参数
     * @return 邀请码
     */
    String invite(GrpIdDTO dto);

    /**
     * 凭邀请码入群
     * @param dto 参数
     */
    void join(GrpJoinDTO dto);

    /**
     * 退出群（群主不可退，需解散）
     * @param dto 参数
     */
    void leave(GrpIdDTO dto);

    /**
     * 移除群成员（仅群主）
     * @param dto 参数
     */
    void removeMember(GrpRemoveMemberDTO dto);

    /**
     * 群成员列表（仅成员可见）
     * @param grpId 群ID
     * @return 成员列表
     */
    List<GrpMemberVO> members(Long grpId);
}
