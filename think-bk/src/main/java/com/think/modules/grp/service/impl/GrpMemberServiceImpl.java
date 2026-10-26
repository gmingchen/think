package com.think.modules.grp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.think.common.pojo.PageResult;
import com.think.common.utils.CollectionUtils;
import com.think.core.mybatisplus.service.ServiceImplX;
import com.think.modules.grp.convert.GrpMemberConvert;
import com.think.modules.grp.entity.GrpMemberEntity;
import com.think.modules.grp.mapper.GrpMemberMapper;
import com.think.modules.grp.model.dto.GrpMemberDTO;
import com.think.modules.grp.model.dto.GrpMemberPageDTO;
import com.think.modules.grp.service.GrpMemberService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 群成员
 *
 * @author 好市民
 */
@Service("grpMemberService")
public class GrpMemberServiceImpl extends ServiceImplX<GrpMemberMapper, GrpMemberEntity> implements GrpMemberService {

    @Override
    public PageResult<GrpMemberDTO> pageByGrp(GrpMemberPageDTO dto) {
        LambdaQueryWrapper<GrpMemberEntity> wrapper = new LambdaQueryWrapper<GrpMemberEntity>()
                .eq(GrpMemberEntity::getGrpId, dto.getGrpId())
                .orderByAsc(GrpMemberEntity::getRole)
                .orderByAsc(GrpMemberEntity::getCreatedAt);
        PageResult<GrpMemberEntity> entityPage = baseMapper.selectPage(dto, wrapper);
        List<GrpMemberDTO> list = CollectionUtils.mapList(entityPage.getList(), GrpMemberConvert.INSTANCE::convert);
        return new PageResult<>(entityPage.getTotal(), entityPage.getPages(), list)
                .setCurrent(entityPage.getCurrent());
    }

    @Override
    public List<Long> listGrpIdByUser(Long userId) {
        LambdaQueryWrapper<GrpMemberEntity> wrapper = new LambdaQueryWrapper<GrpMemberEntity>()
                .eq(GrpMemberEntity::getUserId, userId);
        return CollectionUtils.mapList(baseMapper.selectList(wrapper), GrpMemberEntity::getGrpId);
    }

    @Override
    public void addMember(Long grpId, Long userId, Integer role) {
        GrpMemberEntity any = baseMapper.selectAny(grpId, userId);
        // 已存在有效行：仅在角色变化时更新（幂等）
        if (any != null && !Boolean.TRUE.equals(any.getDeleted())) {
            if (!role.equals(any.getRole())) {
                LambdaQueryWrapper<GrpMemberEntity> wrapper = new LambdaQueryWrapper<GrpMemberEntity>()
                        .eq(GrpMemberEntity::getGrpId, grpId)
                        .eq(GrpMemberEntity::getUserId, userId);
                baseMapper.update(new GrpMemberEntity().setRole(role), wrapper);
            }
            return;
        }
        // 存在软删行：复活复用唯一键行
        if (any != null) {
            baseMapper.revive(grpId, userId, role);
            return;
        }
        // 无行：新增；并发同刻入群撞 uk_grp_user（含软删行占键）时回退到已存在分支，按幂等处理
        try {
            baseMapper.insert(new GrpMemberEntity().setGrpId(grpId).setUserId(userId).setRole(role));
        } catch (DuplicateKeyException e) {
            GrpMemberEntity winner = baseMapper.selectAny(grpId, userId);
            if (winner != null && Boolean.TRUE.equals(winner.getDeleted())) {
                baseMapper.revive(grpId, userId, role);
            }
        }
    }

    @Override
    public void removeMember(Long grpId, Long userId) {
        LambdaQueryWrapper<GrpMemberEntity> wrapper = new LambdaQueryWrapper<GrpMemberEntity>()
                .eq(GrpMemberEntity::getGrpId, grpId)
                .eq(GrpMemberEntity::getUserId, userId);
        baseMapper.delete(wrapper);
    }

    @Override
    public boolean isMember(Long grpId, Long userId) {
        return getRole(grpId, userId) != null;
    }

    @Override
    public Integer getRole(Long grpId, Long userId) {
        GrpMemberEntity entity = baseMapper.selectOne(new LambdaQueryWrapper<GrpMemberEntity>()
                .eq(GrpMemberEntity::getGrpId, grpId)
                .eq(GrpMemberEntity::getUserId, userId));
        return entity != null ? entity.getRole() : null;
    }

    @Override
    public int countByGrp(Long grpId) {
        Long count = baseMapper.selectCount(new LambdaQueryWrapper<GrpMemberEntity>()
                .eq(GrpMemberEntity::getGrpId, grpId));
        return count == null ? 0 : count.intValue();
    }
}
