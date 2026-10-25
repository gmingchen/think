package com.think.modules.wechat.service.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.think.common.constant.RedisConstant;
import com.think.common.enums.ContentCheckResultEnum;
import com.think.common.enums.MediaBizTypeEnum;
import com.think.core.redis.utils.RedisUtils;
import com.think.modules.decision.entity.DecisionEntity;
import com.think.modules.decision.mapper.DecisionMapper;
import com.think.modules.os.service.StorageService;
import com.think.modules.user.entity.UserEntity;
import com.think.modules.user.mapper.UserMapper;
import com.think.modules.wechat.entity.MediaCheckEntity;
import com.think.modules.wechat.handler.WechatHandler;
import com.think.modules.wechat.mapper.MediaCheckMapper;
import com.think.modules.wechat.model.dto.AppletMediaCheckDTO;
import com.think.modules.wechat.service.AppletMediaCheckService;
import com.think.modules.wechat.service.WechatAccessTokenService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 小程序多媒体内容安全审核（mediaCheckAsync，异步）
 * <p>
 * 送审与主业务解耦：提交侧 fire-and-forget（先放行），结果侧由「消息推送」回调驱动处理。
 * 违规处理粒度为「仅摘除该张图 / 重置该头像」，不牵连其余内容。
 * </p>
 *
 * @author 好市民
 */
@Slf4j
@Service("appletMediaCheckService")
public class AppletMediaCheckServiceImpl implements AppletMediaCheckService {

    /**
     * 接口版本：v2
     */
    private static final int VERSION = 2;
    /**
     * 媒体类型：2-图片
     */
    private static final int MEDIA_TYPE_IMAGE = 2;
    private static final String PROVIDER = "wx";
    private static final String SUGGEST_PENDING = "pending";
    private static final String RESULT_PASS = "pass";
    private static final String RESULT_REVIEW = "review";
    private static final String RESULT_RISKY = "risky";
    private static final String EVENT_MEDIA_CHECK = "wxa_media_check";

    @Autowired
    private MediaCheckMapper mediaCheckMapper;
    @Autowired
    private WechatAccessTokenService wechatAccessTokenService;
    @Autowired
    private DecisionMapper decisionMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private StorageService storageService;
    @Autowired
    private RedisUtils redisUtils;

    @Override
    public void submitImageCheck(MediaBizTypeEnum bizType, Long bizId, Integer target,
                                 String mediaUrl, String openid, int scene) {
        if (bizType == null || StringUtils.isBlank(mediaUrl)) {
            return;
        }
        Map<String, Object> params = new HashMap<>(8);
        params.put("media_url", mediaUrl);
        params.put("media_type", MEDIA_TYPE_IMAGE);
        params.put("version", VERSION);
        params.put("scene", scene);
        params.put("openid", openid);

        String traceId;
        try {
            AppletMediaCheckDTO response = wechatAccessTokenService.executeWithTokenRefresh(accessToken ->
                    WechatHandler.doPostJsonOnly(
                            "https://api.weixin.qq.com/wxa/media_check_async?access_token=" + accessToken,
                            params, AppletMediaCheckDTO.class));
            if (response == null || (response.getErrcode() != null && response.getErrcode() != 0)
                    || StringUtils.isBlank(response.getTraceId())) {
                log.error("图片异步送审未取得 trace_id，跳过：bizType={}, bizId={}, errmsg={}",
                        bizType, bizId, response == null ? "null" : response.getErrmsg());
                return;
            }
            traceId = response.getTraceId();
        } catch (Exception e) {
            // fire-and-forget：送审调用失败不阻断主业务（图片/头像已放行），仅告警
            log.error("图片异步送审调用异常，跳过：bizType={}, bizId={}, url={}, err={}",
                    bizType, bizId, mediaUrl, e.getMessage());
            return;
        }
        mediaCheckMapper.insert(new MediaCheckEntity()
                .setBizType(bizType.getCode())
                .setBizId(bizId)
                .setTarget(target)
                .setMediaUrl(mediaUrl)
                .setTraceId(traceId)
                .setOpenid(openid)
                .setScene(scene)
                .setProvider(PROVIDER)
                .setSuggest(SUGGEST_PENDING)
                .setCheckStatus(ContentCheckResultEnum.PENDING.getCode()));
        log.info("图片已提交异步送审：bizType={}, bizId={}, traceId={}", bizType, bizId, traceId);
    }

    @Override
    public boolean handleMediaCheck(JSONObject json) {
        String event = json.getStr("Event", json.getStr("MsgType", StringUtils.EMPTY));
        if (!EVENT_MEDIA_CHECK.equalsIgnoreCase(event)) {
            // 非媒体审核事件，交回上层留痕
            return false;
        }
        String traceId = json.getStr("trace_id", json.getStr("TraceId", null));
        if (StringUtils.isBlank(traceId)) {
            log.error("收到 wxa_media_check 事件但无 trace_id，忽略：{}", json);
            return true;
        }
        try {
            String suggest = resolveSuggest(json);
            MediaCheckEntity row = mediaCheckMapper.selectOne(new LambdaQueryWrapper<MediaCheckEntity>()
                    .eq(MediaCheckEntity::getTraceId, traceId));
            if (row == null) {
                // 流水不存在（如送审事务回滚后微信仍回调）：确认收到即可，无需重推
                log.error("收到未知 trace_id 的媒体审核回调，忽略：traceId={}, suggest={}", traceId, suggest);
                return true;
            }
            boolean violation = isViolation(suggest);
            row.setSuggest(suggest)
                    .setDetail(StringUtils.abbreviate(String.valueOf(json.get("detail")), 500))
                    .setCheckStatus(violation
                            ? ContentCheckResultEnum.REJECT.getCode()
                            : ContentCheckResultEnum.PASS.getCode())
                    .setCheckedAt(LocalDateTime.now());
            mediaCheckMapper.updateById(row);
            if (violation) {
                penalize(row);
            }
            log.info("媒体审核回调处理完成：traceId={}, bizType={}, bizId={}, suggest={}",
                    traceId, row.getBizType(), row.getBizId(), suggest);
        } catch (Exception e) {
            // 处理异常仍回 OK：违规内容处理多为幂等且微信重推同一结论无意义，避免无限重推；异常靠日志排查
            log.error("媒体审核回调处理异常：traceId=" + traceId, e);
        }
        return true;
    }

    /**
     * 取汇总结论：优先 result.suggest，缺失则扫描 detail 各策略取最坏（risky &gt; review &gt; pass）
     */
    private String resolveSuggest(JSONObject json) {
        JSONObject result = json.getJSONObject("result");
        if (result != null && StringUtils.isNotBlank(result.getStr("suggest"))) {
            return result.getStr("suggest");
        }
        JSONArray detail = json.getJSONArray("detail");
        String worst = RESULT_PASS;
        if (detail != null) {
            for (int i = 0; i < detail.size(); i++) {
                JSONObject item = detail.getJSONObject(i);
                String s = item == null ? null : item.getStr("suggest");
                if (RESULT_RISKY.equalsIgnoreCase(s)) {
                    return RESULT_RISKY;
                }
                if (RESULT_REVIEW.equalsIgnoreCase(s)) {
                    worst = RESULT_REVIEW;
                }
            }
        }
        return worst;
    }

    /**
     * risky/review 均视为需处理（与文本审核口径一致，对外可见内容风险容忍度低）
     */
    private boolean isViolation(String suggest) {
        return RESULT_RISKY.equalsIgnoreCase(suggest) || RESULT_REVIEW.equalsIgnoreCase(suggest);
    }

    /**
     * 违规处理：决策单图片→从 images 移除该URL；用户头像→重置为空。均顺带删除七牛文件。
     */
    private void penalize(MediaCheckEntity row) {
        MediaBizTypeEnum bizType = MediaBizTypeEnum.of(row.getBizType());
        String url = row.getMediaUrl();
        if (bizType == null || StringUtils.isBlank(url)) {
            return;
        }
        if (bizType == MediaBizTypeEnum.DECISION_IMAGE) {
            DecisionEntity decision = decisionMapper.selectById(row.getBizId());
            if (decision == null || decision.getImages() == null || !decision.getImages().contains(url)) {
                // 决策单已删/图片已变更：无需处理（避免误删当前内容）
                return;
            }
            List<String> images = new ArrayList<>(decision.getImages());
            images.remove(url);
            decisionMapper.update(new DecisionEntity().setImages(images),
                    new LambdaUpdateWrapper<DecisionEntity>().eq(DecisionEntity::getId, decision.getId()));
            deleteFileQuietly(url);
            log.info("媒体审核违规，已摘除决策单图片：decisionId={}, url={}", decision.getId(), url);
        } else if (bizType == MediaBizTypeEnum.USER_AVATAR) {
            UserEntity user = userMapper.selectById(row.getBizId());
            if (user == null || !url.equals(user.getAvatar())) {
                // 头像已被用户换成别的：不动用户当前设置
                return;
            }
            userMapper.update(null, new LambdaUpdateWrapper<UserEntity>()
                    .set(UserEntity::getAvatar, null)
                    .eq(UserEntity::getId, user.getId()));
            redisUtils.delete(RedisConstant.USER + user.getId());
            deleteFileQuietly(url);
            log.info("媒体审核违规，已重置用户头像：userId={}, url={}", user.getId(), url);
        }
    }

    private void deleteFileQuietly(String url) {
        try {
            storageService.deleteByUrl(url);
        } catch (Exception e) {
            log.error("删除违规媒体文件失败（不影响处置结果）：url={}, err={}", url, e.getMessage());
        }
    }
}
