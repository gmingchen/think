package com.think.modules.decision.service;

import com.think.common.pojo.PageResult;
import com.think.core.mybatisplus.service.IServiceX;
import com.think.modules.decision.entity.DecisionEntity;
import com.think.modules.decision.model.dto.DecisionCreateDTO;
import com.think.modules.decision.model.dto.DecisionGrpPageDTO;
import com.think.modules.decision.model.dto.DecisionIdDTO;
import com.think.modules.decision.model.dto.DecisionPageDTO;
import com.think.modules.decision.model.dto.DecisionPlazaPageDTO;
import com.think.modules.decision.model.dto.DecisionPublishDTO;
import com.think.modules.decision.model.dto.DecisionUpdateDTO;
import com.think.modules.decision.model.vo.DecisionDetailVO;
import com.think.modules.decision.model.vo.DecisionListItemVO;
import com.think.modules.decision.model.vo.UserStatsVO;

/**
 * 决策单
 *
 * @author 好市民
 */
public interface DecisionService extends IServiceX<DecisionEntity> {

    /**
     * 我发起的决策单列表（分页，可按状态/可见范围筛选）
     * @param dto 分页参数
     * @return 分页结果
     */
    PageResult<DecisionListItemVO> pageMine(DecisionPageDTO dto);

    /**
     * 我评议过的决策单列表（分页）
     * @param dto 分页参数
     * @return 分页结果
     */
    PageResult<DecisionListItemVO> pageReviewed(DecisionPageDTO dto);

    /**
     * 群内决策单列表（分页，仅群成员可见）
     * @param dto 分页参数
     * @return 分页结果
     */
    PageResult<DecisionListItemVO> pageGrp(DecisionGrpPageDTO dto);

    /**
     * 公开广场列表（仅审核通过的公开档，按发布时间倒序，可按标签筛选）
     * @param dto 分页参数
     * @return 分页结果
     */
    PageResult<DecisionListItemVO> pagePlaza(DecisionPlazaPageDTO dto);

    /**
     * 随机查看：随机取一条评议中的公开决策单（首次「去看看」）
     * @return 详情，无则 null
     */
    DecisionDetailVO randomPlaza();

    /**
     * 换一个：随机取一条公开决策单，硬规则保证不与上一条重复
     * @param excludeId 上一个正在查看的决策单ID（查询层直接排除）；排除后无剩余则返回 null
     * @return 详情，无可换时 null
     */
    DecisionDetailVO changePlaza(Long excludeId);

    /**
     * 决策单详情（含可见性判定、角色标记、先评后看评议与报告）
     * @param id 决策单ID
     * @return 详情
     */
    DecisionDetailVO detail(Long id);

    /**
     * 我的统计：发起数 / 参与评议数 / 收获评议数（实时统计）
     * @return 统计
     */
    UserStatsVO statsMine();

    /**
     * 创建决策单（存草稿：status=0，校验通过后落库，不立即生效）
     * @param dto 参数
     * @return 决策单ID
     */
    Long create(DecisionCreateDTO dto);

    /**
     * 编辑决策单（仅草稿态 / 审核退回态可改）
     * @param dto 参数
     */
    void update(DecisionUpdateDTO dto);

    /**
     * 发布决策单（群内档直接生效；公开档走审核闸门，通过后生效）
     * @param dto 参数
     */
    void publish(DecisionPublishDTO dto);

    /**
     * 提前结束评议（未满评议时限）：打 ended_early=1 标记并立即生成报告（status→已截止）
     * @param dto 参数
     */
    void endEarly(DecisionIdDTO dto);

    /**
     * 关闭评议：停止接收评议、不出报告（status→已关闭），写操作审计
     * @param dto 参数
     */
    void close(DecisionIdDTO dto);

    /**
     * 删除决策单：级联软删 review/report/recap/decision__tag（审核/举报/审计留痕不删），写操作审计
     * @param dto 参数
     */
    void delete(DecisionIdDTO dto);
}
