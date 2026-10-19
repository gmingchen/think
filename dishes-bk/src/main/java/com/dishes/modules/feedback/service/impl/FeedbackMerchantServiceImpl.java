package com.dishes.modules.feedback.service.impl;

import com.dishes.common.enums.FeedbackCurrentToolEnum;
import com.dishes.common.utils.CollectionUtils;
import com.dishes.common.utils.StringUtils;
import com.dishes.core.mybatisplus.service.ServiceImplX;
import com.dishes.core.security.utils.SecurityUtils;
import com.dishes.exception.RunException;
import com.dishes.modules.auth.model.LoginUser;
import com.dishes.modules.feedback.entity.FeedbackMerchantEntity;
import com.dishes.modules.feedback.mapper.FeedbackMerchantMapper;
import com.dishes.modules.feedback.model.dto.FeedbackMerchantCreateDTO;
import com.dishes.modules.feedback.service.FeedbackMerchantService;
import com.dishes.modules.wechat.service.AppletSecCheckService;
import org.springframework.stereotype.Service;

import org.springframework.beans.factory.annotation.Autowired;
import java.util.Arrays;
import java.util.List;

/**
 * 商户需求反馈（docs/v13，需求验证用）
 * @author 好市民
 */
@Service("feedbackMerchantService")
public class FeedbackMerchantServiceImpl extends ServiceImplX<FeedbackMerchantMapper, FeedbackMerchantEntity> implements FeedbackMerchantService {

    @Autowired
    private AppletSecCheckService appletSecCheckService;

    @Override
    public void create(FeedbackMerchantCreateDTO dto) {
        // 多选「现用管理方式」取值范围校验：@Enum 注解只能校验单个 Integer，这里是 List，需手动逐项核对
        List<Integer> currentTool = dto.getCurrentTool();
        checkCurrentTool(currentTool);
        // 自由文本内容安全闸门，对齐 v10 AI 文案识别链路：命中违规直接拒收、不落库
        String openid = currentOpenid();
        appletSecCheckService.checkText(dto.getPainPoint(), openid);
        appletSecCheckService.checkText(dto.getExpectFeature(), openid);
        FeedbackMerchantEntity entity = new FeedbackMerchantEntity()
                .setUserId(SecurityUtils.getLoginUserId())
                .setOpenid(openid)
                .setBusinessType(dto.getBusinessType())
                .setCurrentTool(CollectionUtils.isNotEmpty(currentTool) ? CollectionUtils.join(currentTool, ",") : null)
                .setDishScale(dto.getDishScale())
                .setPainPoint(StringUtils.trim(dto.getPainPoint()))
                .setExpectFeature(StringUtils.trim(dto.getExpectFeature()))
                .setWillingness(dto.getWillingness())
                .setContactWx(StringUtils.trim(dto.getContactWx()))
                .setContactPhone(StringUtils.trim(dto.getContactPhone()));
        baseMapper.insert(entity);
    }

    /**
     * 校验多选管理方式的每一项都是 {@link FeedbackCurrentToolEnum} 合法 code，
     * 避免脏数据（如前端传了不存在的选项值）落库后没法按选项统计；
     * 该字段已改为选填（docs/v13 §4 定稿调整），为空时直接跳过校验
     */
    private void checkCurrentTool(List<Integer> currentTool) {
        if (CollectionUtils.isEmpty(currentTool)) {
            return;
        }
        List<Integer> invalid = CollectionUtils.filterList(currentTool,
                code -> code == null || Arrays.stream(FeedbackCurrentToolEnum.ARRAY).noneMatch(it -> it == code));
        if (CollectionUtils.isNotEmpty(invalid)) {
            throw new RunException("现用管理方式选项不合法");
        }
    }

    /**
     * 取当前登录用户 openid：审核接口需要发布者身份定位，缺失不阻断主流程（与生图链路的兜底策略一致），
     * 仅落库时该列可能为空，回访定位会退化成 userId
     */
    private String currentOpenid() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        return loginUser != null ? loginUser.getOpenid() : null;
    }
}
