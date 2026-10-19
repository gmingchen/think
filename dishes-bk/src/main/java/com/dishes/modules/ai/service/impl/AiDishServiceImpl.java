package com.dishes.modules.ai.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dishes.common.constant.RedisConstant;
import com.dishes.common.utils.CollectionUtils;
import com.dishes.common.utils.StringUtils;
import com.dishes.core.redis.utils.RedisUtils;
import com.dishes.core.security.utils.SecurityUtils;
import com.dishes.exception.RunException;
import com.dishes.modules.ai.client.LlmClient;
import com.dishes.modules.ai.client.ImageClient;
import com.dishes.modules.ai.config.AiProperties;
import com.dishes.modules.ai.model.cache.AiRecommendCache;
import com.dishes.modules.ai.model.dto.AiAdoptDTO;
import com.dishes.modules.ai.model.dto.AiImageGenDTO;
import com.dishes.modules.ai.model.dto.AiParseCommitDTO;
import com.dishes.modules.ai.model.dto.AiParseDTO;
import com.dishes.modules.ai.model.dto.AiRecommendDTO;
import com.dishes.modules.ai.model.vo.AiImageVO;
import com.dishes.modules.ai.model.vo.AiParseVO;
import com.dishes.modules.ai.model.vo.AiRecommendVO;
import com.dishes.modules.ai.prompt.AiDishPrompt;
import com.dishes.modules.ai.prompt.AiImagePrompt;
import com.dishes.modules.ai.prompt.AiParsePrompt;
import com.dishes.modules.ai.service.AiDishService;
import com.dishes.modules.auth.model.LoginUser;
import com.dishes.modules.category.model.vo.CategoryVO;
import com.dishes.modules.category.service.CategoryService;
import com.dishes.modules.dish.model.dto.DishCreateDTO;
import com.dishes.modules.dish.model.dto.DishFoodDTO;
import com.dishes.modules.dish.model.dto.DishSeasoningDTO;
import com.dishes.modules.dish.model.dto.DishStepDTO;
import com.dishes.modules.dish.model.vo.DishFoodVO;
import com.dishes.modules.dish.model.vo.DishVO;
import com.dishes.modules.dish.service.DishFoodService;
import com.dishes.modules.dish.service.DishService;
import com.dishes.modules.food.entity.FoodEntity;
import com.dishes.modules.food.service.FoodService;
import com.dishes.modules.kitchen.service.KitchenUserService;
import com.dishes.modules.seasoning.service.SeasoningService;
import com.dishes.modules.user.service.UserService;
import com.dishes.modules.wechat.service.AppletSecCheckService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * AI 荐菜
 * @author 好市民
 */
@Slf4j
@Service("aiDishService")
public class AiDishServiceImpl implements AiDishService {

    /**
     * 命中率粗筛阈值
     */
    private static final double MIN_HIT_RATE = 0.3;
    /**
     * 进入 LLM 精排的候选上限
     */
    private static final int MAX_CANDIDATE = 10;
    /**
     * LLM 排序结果非法时，按规则命中序兜底展示的条数上限（实际取 min(人数推导上限, 候选数)）
     */
    private static final int FALLBACK_RANKED_SIZE = 3;
    /**
     * 人数缺省值（peopleCount 为空时按 2-3 人档处理）
     */
    private static final int DEFAULT_PEOPLE = 3;
    /**
     * 第一组展示条数上限（4 人+ 档）
     */
    private static final int RANKED_MAX = 4;
    /**
     * 第二组新菜谱条数硬上限（防 token 膨胀，4 人+ 也最多 3 道）
     */
    private static final int FRESH_MAX = 3;
    /**
     * JSON 解析失败重试时使用的低温度
     */
    private static final double RETRY_TEMPERATURE = 0.2;
    /**
     * 日限次计数 key 的过期时间（秒），key 已带日期天然隔离，此值仅用于兜底清理
     */
    private static final long COUNT_EXPIRE = 86400L;
    /**
     * 第一组兜底（LLM 排序不可用、按命中序展示）时的默认理由
     */
    private static final String FALLBACK_REASON = "按你家现有食材的命中率排出来的拿手菜";
    /**
     * 辛香料黑名单：采纳时即使模型误写进 foods，也改挂到 seasonings（项目里葱姜蒜等归调料）
     */
    private static final Set<String> AROMATIC_KEYWORDS = new HashSet<>(Arrays.asList(
            "葱", "姜", "蒜", "辣椒", "花椒", "八角", "桂皮", "香叶"));
    /**
     * 生图菜名结构校验：2~32 字、不含控制字符
     * <p>
     * 真实菜名会带「!!!」「（微辣）」甚至纯英文（Chicken Wings）,不做语义限制；
     * 只挡控制字符——换行会打乱 prompt 的分段结构。是否违规全部交给微信内容审核判断
     * </p>
     */
    private static final Pattern DISH_NAME_PATTERN = Pattern.compile("^[^\\u0000-\\u001F\\u007F]{2,32}$");
    /**
     * 菜名不得包含外链：菜谱封面不需要链接，且 URL 是引流与绕过审核的常见载体
     */
    private static final Pattern DISH_NAME_URL_PATTERN = Pattern.compile("(?i)(https?://|www\\.)");
    /**
     * 识别结果条数上限：对齐 AiParseCommitDTO 的 @Size。LLM 输出条数不受控，不截断则可能识别成功、
     * 用户点「确认使用」时才被 commit 校验拒单（额度已扣）
     */
    private static final int PARSE_MAX_ITEM = 50;
    private static final int PARSE_MAX_STEP = 30;
    /**
     * 识别结果字段长度上限：对齐 DB 列宽（dish/food/seasoning/category.name 30、dish__food.amount
     * 与 process 20、dish_step.desc 500），防 LLM 超长输出在 commit 建库时撑爆 SQL；remark 取 DTO 的 200（严于 dish.remark 的 500）
     */
    private static final int PARSE_MAX_NAME_LEN = 30;
    private static final int PARSE_MAX_AMOUNT_LEN = 20;
    private static final int PARSE_MAX_REMARK_LEN = 200;
    private static final int PARSE_MAX_STEP_LEN = 500;

    @Autowired
    private AiProperties aiProperties;
    @Autowired
    private LlmClient llmClient;
    @Autowired
    private ImageClient imageClient;
    @Autowired
    private RedisUtils redisUtils;
    @Autowired
    private DishService dishService;
    @Autowired
    private DishFoodService dishFoodService;
    @Autowired
    private FoodService foodService;
    @Autowired
    private SeasoningService seasoningService;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private KitchenUserService kitchenUserService;
    @Autowired
    private UserService userService;
    @Autowired
    private AppletSecCheckService appletSecCheckService;

    /**
     * 按当前登录人的有效档位取今日 AI 额度上限：畅厨（tier ≥ 2）用 pro 上限，其余（免费 / 轻享 / 已过期）用基础上限。
     * <p>轻享不加量是 §1.2 定稿——它免的是展示型广告，不在 AI 额度上额外给好处；
     * 档位只认 {@code memberTierAt(now)}（已按 expire_at 归零，与全站判定同口径 §6.3），快照缺失按非会员处理。</p>
     */
    private int resolveDailyLimit(int baseLimit, int proLimit) {
        LoginUser user = SecurityUtils.getLoginUser();
        int tier = user != null ? user.memberTierAt(LocalDateTime.now()) : 0;
        return tier >= 2 ? proLimit : baseLimit;
    }

    @Override
    public Integer remainCount() {
        Long userId = SecurityUtils.getLoginUserId();
        int limit = resolveDailyLimit(aiProperties.getLlm().getDailyLimit(), aiProperties.getLlm().getProDailyLimit());
        String countKey = RedisConstant.AI_RECOMMEND_COUNT + userId + ":" + LocalDate.now();
        long used = redisUtils.get(countKey)
                .map(v -> Long.parseLong(v.toString()))
                .orElse(0L);
        return (int) Math.max(0, limit - used);
    }

    @Override
    public Integer parseRemainCount() {
        Long userId = SecurityUtils.getLoginUserId();
        int limit = resolveDailyLimit(aiProperties.getLlm().getParseDailyLimit(), aiProperties.getLlm().getProParseDailyLimit());
        String countKey = RedisConstant.AI_PARSE_COUNT + userId + ":" + LocalDate.now();
        long used = redisUtils.get(countKey)
                .map(v -> Long.parseLong(v.toString()))
                .orElse(0L);
        return (int) Math.max(0, limit - used);
    }

    @Override
    public Integer imageRemainCount() {
        Long userId = SecurityUtils.getLoginUserId();
        int limit = resolveDailyLimit(aiProperties.getImage().getDailyLimit(), aiProperties.getImage().getProDailyLimit());
        String countKey = RedisConstant.AI_IMAGE_COUNT + userId + ":" + LocalDate.now();
        long used = redisUtils.get(countKey)
                .map(v -> Long.parseLong(v.toString()))
                .orElse(0L);
        return (int) Math.max(0, limit - used);
    }
    @Override
    public AiRecommendVO recommend(AiRecommendDTO dto) {
        Long userId = SecurityUtils.getLoginUserId();
        Long kitchenId = dto.getKitchenId();
        kitchenUserService.checkMember(kitchenId, userId);

        // 日限次：自增后再判，超限直接拒绝（简单可靠，不做回滚）；上限按当前登录人档位取（畅厨提额）
        int limit = resolveDailyLimit(aiProperties.getLlm().getDailyLimit(), aiProperties.getLlm().getProDailyLimit());
        String countKey = RedisConstant.AI_RECOMMEND_COUNT + userId + ":" + LocalDate.now();
        long count = redisUtils.increment(countKey, COUNT_EXPIRE);
        if (count > limit) {
            throw new RunException("今日 AI 荐菜次数已用完，明天再来");
        }
        int remainCount = (int) Math.max(0, limit - count);

        // 勾选食材 → 本厨房有效食材（非本厨房 id 静默剔除）
        List<FoodEntity> selectedFoods = foodService.list(new LambdaQueryWrapper<FoodEntity>()
                .eq(FoodEntity::getKitchenId, kitchenId)
                .in(FoodEntity::getId, dto.getFoodIds()));
        Set<Long> selectedFoodIds = selectedFoods.stream().map(FoodEntity::getId).collect(Collectors.toSet());
        List<String> selectedFoodNames = selectedFoods.stream().map(FoodEntity::getName).collect(Collectors.toList());

        // 厨房全部菜谱：一次查询既供命中率粗筛，又提供 fresh 去重菜名
        List<DishVO> dishes = dishService.list(kitchenId);

        // 换一批去重：排除本会话已展示过的第一组菜，从剩余候选重排；若被排空则回退全量（候选已耗尽）
        Set<Long> excludeDishIds = CollectionUtils.isEmpty(dto.getExcludeDishIds())
                ? Collections.emptySet() : new HashSet<>(dto.getExcludeDishIds());
        List<AiCandidate> candidates = coarseFilter(dishes, selectedFoodIds, excludeDishIds);
        if (candidates.isEmpty() && !excludeDishIds.isEmpty()) {
            candidates = coarseFilter(dishes, selectedFoodIds, Collections.emptySet());
        }

        // fresh 避开名单 = 厨房现有菜名 + 本会话已生成过的新菜名，保证换一批出不同的新菜
        List<String> existingDishNames = dishes.stream().map(DishVO::getName).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(dto.getExcludeFreshNames())) {
            existingDishNames.addAll(dto.getExcludeFreshNames());
        }

        // 推荐菜量随人数伸缩：1 人精简、2-3 人常规、4 人+ 加量（上限受 RANKED_MAX / FRESH_MAX 约束）
        int rankedLimit = rankedLimitForPeople(dto.getPeopleCount());
        int freshLimit = freshCountForPeople(dto.getPeopleCount());

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(LlmClient.message("system", AiDishPrompt.system(rankedLimit, freshLimit)));
        messages.add(LlmClient.message("user", AiDishPrompt.user(
                selectedFoodNames, dto.getPeopleCount(), dto.getTaste(), dto.getAvoid(),
                candidateTexts(candidates), existingDishNames)));

        // 一次调用产出两段（解析失败降温度重试一次）
        JSONObject result = callLlm(messages);

        List<AiRecommendVO.RankedDish> ranked = resolveRanked(result, candidates, rankedLimit);
        List<AiRecommendVO.FreshDish> fresh = resolveFresh(result, freshLimit);

        // 暂存第二组供采纳
        String recommendId = IdUtil.fastSimpleUUID();
        AiRecommendCache cache = new AiRecommendCache()
                .setUserId(userId)
                .setKitchenId(kitchenId)
                .setFresh(fresh);
        redisUtils.set(RedisConstant.AI_RECOMMEND + recommendId, cache, aiProperties.getLlm().getRecommendExpire());

        AiRecommendVO vo = new AiRecommendVO();
        vo.setRecommendId(recommendId);
        vo.setRanked(ranked);
        vo.setFresh(fresh);
        vo.setRemainCount(remainCount);
        return vo;
    }

    @Override
    public Long adopt(AiAdoptDTO dto) {
        Long userId = SecurityUtils.getLoginUserId();
        AiRecommendCache cache = redisUtils.get(RedisConstant.AI_RECOMMEND + dto.getRecommendId(), AiRecommendCache.class)
                .orElseThrow(() -> new RunException("推荐已过期，重新生成一份吧"));
        // 凭证归属校验，越权不泄露原因，统一按过期处理
        if (!cache.getUserId().equals(userId)) {
            throw new RunException("推荐已过期，重新生成一份吧");
        }
        List<AiRecommendVO.FreshDish> freshList = cache.getFresh();
        Integer index = dto.getIndex();
        if (CollectionUtils.isEmpty(freshList) || index == null || index < 0 || index >= freshList.size()) {
            throw new RunException("该推荐不存在");
        }
        AiRecommendVO.FreshDish fd = freshList.get(index);
        Long kitchenId = cache.getKitchenId();
        // 提前验编辑权限，避免无权限时白白新建食材/调料
        kitchenUserService.checkPermission(kitchenId);

        // 辛香料兜底分流：模型误写进 foods 的葱姜蒜等改挂到 seasonings，避免新建重复食材
        List<AiRecommendVO.FreshSeasoning> seasonings = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(fd.getSeasonings())) {
            seasonings.addAll(fd.getSeasonings());
        }
        List<AiRecommendVO.FreshFood> foods = splitAromatics(fd.getFoods(), seasonings);

        DishCreateDTO create = new DishCreateDTO()
                .setKitchenId(kitchenId)
                .setCategoryId(matchCategoryId(kitchenId, fd.getCategoryHint()))
                .setName(StringUtils.trim(fd.getName()))
                .setRemark(fd.getRemark())
                .setFoods(buildFoods(kitchenId, foods, userId))
                .setSeasonings(buildSeasonings(kitchenId, seasonings, userId))
                .setSteps(buildSteps(fd.getSteps()));
        return dishService.create(create);
    }

    @Override
    public AiParseVO parse(AiParseDTO dto) {
        Long userId = SecurityUtils.getLoginUserId();
        Long kitchenId = dto.getKitchenId();
        kitchenUserService.checkMember(kitchenId, userId);

        // 文案识别独立限次：自增后判限，超限直接拒（仅 parse 计数，commit 不耗额度）；上限按档位取（畅厨提额）
        int limit = resolveDailyLimit(aiProperties.getLlm().getParseDailyLimit(), aiProperties.getLlm().getProParseDailyLimit());
        String countKey = RedisConstant.AI_PARSE_COUNT + userId + ":" + LocalDate.now();
        long count = redisUtils.increment(countKey, COUNT_EXPIRE);
        if (count > limit) {
            throw new RunException("今日 AI 识别次数已用完，明天再来");
        }

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(LlmClient.message("system", AiParsePrompt.system()));
        messages.add(LlmClient.message("user", AiParsePrompt.user(dto.getText())));

        // 解析失败降温度重试一次；parse 只出预览，不建库、不写 dish 表
        JSONObject result = callLlm(messages);
        AiParseVO vo = JSONUtil.toBean(result, AiParseVO.class);
        // 兜底空集合，保证前端预览结构稳定（缺项不代表失败）
        if (vo.getFoods() == null) {
            vo.setFoods(new ArrayList<>());
        }
        if (vo.getSeasonings() == null) {
            vo.setSeasonings(new ArrayList<>());
        }
        if (vo.getSteps() == null) {
            vo.setSteps(new ArrayList<>());
        }
        normalizeParse(vo);
        vo.setCategoryId(null);
        vo.setCategoryName(null);
        return vo;
    }

    /**
     * 识别结果归一：按 commit 校验上限与 DB 列宽截断，并丢掉无名条目
     * <p>
     * 选择截断而非拒单：parse 已经扣过一次识别额度，若等用户点「确认使用」才被拦下等于白花一次次数；
     * 在预览之前归一，用户所见即最终回填进表单的内容
     * </p>
     */
    private void normalizeParse(AiParseVO vo) {
        vo.setName(subTrim(vo.getName(), PARSE_MAX_NAME_LEN));
        vo.setRemark(subTrim(vo.getRemark(), PARSE_MAX_REMARK_LEN));
        vo.setCategoryHint(subTrim(vo.getCategoryHint(), PARSE_MAX_NAME_LEN));

        List<AiParseVO.Food> foods = new ArrayList<>();
        for (AiParseVO.Food food : vo.getFoods()) {
            if (food == null || StringUtils.isBlank(food.getName()) || foods.size() >= PARSE_MAX_ITEM) {
                continue;
            }
            food.setName(subTrim(food.getName(), PARSE_MAX_NAME_LEN));
            food.setAmount(subTrim(food.getAmount(), PARSE_MAX_AMOUNT_LEN));
            food.setProcess(subTrim(food.getProcess(), PARSE_MAX_AMOUNT_LEN));
            foods.add(food);
        }
        vo.setFoods(foods);

        List<AiParseVO.Seasoning> seasonings = new ArrayList<>();
        for (AiParseVO.Seasoning seasoning : vo.getSeasonings()) {
            if (seasoning == null || StringUtils.isBlank(seasoning.getName()) || seasonings.size() >= PARSE_MAX_ITEM) {
                continue;
            }
            seasoning.setName(subTrim(seasoning.getName(), PARSE_MAX_NAME_LEN));
            seasoning.setAmount(subTrim(seasoning.getAmount(), PARSE_MAX_AMOUNT_LEN));
            seasonings.add(seasoning);
        }
        vo.setSeasonings(seasonings);

        List<String> steps = new ArrayList<>();
        for (String step : vo.getSteps()) {
            if (StringUtils.isBlank(step) || steps.size() >= PARSE_MAX_STEP) {
                continue;
            }
            steps.add(subTrim(step, PARSE_MAX_STEP_LEN));
        }
        vo.setSteps(steps);
    }

    /**
     * 去首尾空白后按上限截断（null 安全，不把 null 硬转成空串）
     */
    private String subTrim(String text, int maxLen) {
        String trimmed = StringUtils.trim(text);
        return trimmed == null ? null : StringUtils.sub(trimmed, 0, maxLen);
    }

    @Override
    @Transactional(rollbackFor = RunException.class)
    public AiParseVO commit(AiParseCommitDTO dto) {
        Long userId = SecurityUtils.getLoginUserId();
        Long kitchenId = dto.getKitchenId();
        kitchenUserService.checkMember(kitchenId, userId);
        // 会向食材/调料库写入新条目，需管理权限（对齐 adopt）
        kitchenUserService.checkPermission(kitchenId);

        // 辛香料兜底分流：模型误写进 foods 的葱姜蒜改挂到 seasonings
        List<AiParseVO.Seasoning> seasonings = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(dto.getSeasonings())) {
            seasonings.addAll(dto.getSeasonings());
        }
        List<AiParseVO.Food> foods = splitParseAromatics(dto.getFoods(), seasonings);

        AiParseVO vo = new AiParseVO();
        vo.setName(StringUtils.trim(dto.getName()));
        vo.setRemark(dto.getRemark());
        vo.setCategoryHint(dto.getCategoryHint());
        CategoryVO category = matchCategory(kitchenId, dto.getCategoryHint());
        if (category != null) {
            vo.setCategoryId(category.getId());
            vo.setCategoryName(category.getName());
        }
        vo.setFoods(buildParseFoods(kitchenId, foods, userId));
        vo.setSeasonings(buildParseSeasonings(kitchenId, seasonings, userId));
        vo.setSteps(CollectionUtils.isEmpty(dto.getSteps())
                ? new ArrayList<>() : new ArrayList<>(dto.getSteps()));
        return vo;
    }

    @Override
    public AiImageVO genImage(AiImageGenDTO dto) {
        Long userId = SecurityUtils.getLoginUserId();
        Long kitchenId = dto.getKitchenId();
        kitchenUserService.checkMember(kitchenId, userId);
        // 内容安全闸门：放在计数之前，被拦下的请求不该占用当日额度
        checkImageSafe(dto, userId);

        // 独立限次：自增后判限；云函数失败时回退额度，避免未出图也扣额（与 parse 失败不回退不同：LLM 失败已消耗 token，本层生图失败云函数未返图则未消耗赠额）
        int limit = resolveDailyLimit(aiProperties.getImage().getDailyLimit(), aiProperties.getImage().getProDailyLimit());
        String countKey = RedisConstant.AI_IMAGE_COUNT + userId + ":" + LocalDate.now();
        long count = redisUtils.increment(countKey, COUNT_EXPIRE);
        if (count > limit) {
            throw new RunException("今日 AI 生图次数已用完，明天再来");
        }

        String prompt = AiImagePrompt.build(dto);
        try {
            String imageBase64 = imageClient.generate(prompt);
            return new AiImageVO().setImageBase64(imageBase64);
        } catch (RunException e) {
            // 生图失败回退本次额（云函数内部未成功不会消耗赠额）
            redisUtils.decrement(countKey);
            throw e;
        }
    }

    /**
     * 生图内容安全闸门：结构校验 + 微信内容审核
     * <p>
     * 生图入口的菜名/食材/步骤全部用户手填且直接拼进 prompt，不拦截等于把「任意文本 → 可对外分享图片」的通道敲开。
     * 结构层只做最小拦截（控制字符、外链），不在代码里判“像不像菜名”；
     * 涉黄涉政等语义违规全部交给 msgSecCheck 的官方词库，避开在公开仓库自维护敏感词表（词表本身会泄露，且永远滞后）
     * </p>
     */
    private void checkImageSafe(AiImageGenDTO dto, Long userId) {
        String name = StringUtils.trim(dto.getName());
        if (!DISH_NAME_PATTERN.matcher(name).matches()) {
            throw new RunException("菜名需为 2~32 字的常规文本，请修改后重试");
        }
        if (DISH_NAME_URL_PATTERN.matcher(name).find()) {
            throw new RunException("菜名不能包含链接");
        }
        String openid = Optional.ofNullable(userService.info(userId))
                .map(user -> user.getOpenid()).orElse(null);
        if (StringUtils.isBlank(openid)) {
            // 无 openid 时微信侧会拒调，审核无法定位发布者，留痕便于发现
            log.warn("用户 {} 缺少 openid，生图内容审核无法定位发布者", userId);
        }
        appletSecCheckService.checkText(AiImagePrompt.buildUserContent(dto), openid);
    }

    /**
     * 命中率粗筛：非可选食材计命中，调料不计
     */
    private List<AiCandidate> coarseFilter(List<DishVO> dishes, Set<Long> selectedFoodIds, Set<Long> excludeDishIds) {
        if (CollectionUtils.isEmpty(dishes)) {
            return new ArrayList<>();
        }
        List<Long> dishIds = dishes.stream().map(DishVO::getId).collect(Collectors.toList());
        Map<Long, List<DishFoodVO>> foodMap = dishFoodService.mapByDishIds(dishIds);

        List<AiCandidate> candidates = new ArrayList<>();
        for (DishVO dish : dishes) {
            if (excludeDishIds != null && excludeDishIds.contains(dish.getId())) {
                continue;
            }
            List<DishFoodVO> items = foodMap.getOrDefault(dish.getId(), Collections.emptyList());
            long denominator = items.stream().filter(i -> !isOptional(i)).count();
            if (denominator == 0) {
                continue;
            }
            long hit = items.stream()
                    .filter(i -> !isOptional(i) && selectedFoodIds.contains(i.getFoodId()))
                    .count();
            double rate = (double) hit / denominator;
            if (rate < MIN_HIT_RATE) {
                continue;
            }
            List<String> have = items.stream()
                    .filter(i -> selectedFoodIds.contains(i.getFoodId()))
                    .map(DishFoodVO::getFoodName).collect(Collectors.toList());
            List<String> lack = items.stream()
                    .filter(i -> !selectedFoodIds.contains(i.getFoodId()))
                    .map(DishFoodVO::getFoodName).collect(Collectors.toList());
            candidates.add(new AiCandidate(dish, rate, have, lack));
        }
        // 命中率高者优先，其次做过的次数多者优先
        candidates.sort((a, b) -> {
            int cmp = Double.compare(b.rate, a.rate);
            return cmp != 0 ? cmp : Integer.compare(nvl(b.dish.getCookCount()), nvl(a.dish.getCookCount()));
        });
        return candidates.size() > MAX_CANDIDATE ? new ArrayList<>(candidates.subList(0, MAX_CANDIDATE)) : candidates;
    }

    private List<String> candidateTexts(List<AiCandidate> candidates) {
        return candidates.stream().map(c -> {
            StringBuilder sb = new StringBuilder("[ID:" + c.dish.getId() + "] ").append(c.dish.getName());
            sb.append("（已有：").append(CollectionUtils.isEmpty(c.have) ? "无" : StringUtils.join("、", c.have));
            sb.append("；缺：").append(CollectionUtils.isEmpty(c.lack) ? "无" : StringUtils.join("、", c.lack)).append("）");
            return sb.toString();
        }).collect(Collectors.toList());
    }

    /**
     * 解析第一组：dishId 必须命中候选，全非法则按规则命中序兜底；结果截断到人数推导上限
     */
    private List<AiRecommendVO.RankedDish> resolveRanked(JSONObject result, List<AiCandidate> candidates, int rankedMax) {
        List<AiRecommendVO.RankedDish> ranked = new ArrayList<>();
        if (CollectionUtils.isEmpty(candidates)) {
            return ranked;
        }
        Map<Long, AiCandidate> candidateById = candidates.stream()
                .collect(Collectors.toMap(c -> c.dish.getId(), c -> c, (a, b) -> a));
        JSONArray rankedArr = result.getJSONArray("ranked");
        Set<Long> used = new LinkedHashSet<>();
        if (rankedArr != null) {
            for (Object item : rankedArr) {
                if (ranked.size() >= rankedMax) {
                    break;
                }
                JSONObject obj = (JSONObject) item;
                Long dishId = obj.getLong("dishId");
                AiCandidate candidate = dishId == null ? null : candidateById.get(dishId);
                if (candidate == null || !used.add(dishId)) {
                    continue;
                }
                ranked.add(toRanked(candidate, obj.getStr("reason")));
            }
        }
        // LLM 排序不可用时，按粗筛命中序兜底，补上通用理由避免卡片空理由
        if (ranked.isEmpty()) {
            int size = Math.min(Math.min(rankedMax, FALLBACK_RANKED_SIZE), candidates.size());
            for (int i = 0; i < size; i++) {
                ranked.add(toRanked(candidates.get(i), FALLBACK_REASON));
            }
        }
        return ranked;
    }

    private AiRecommendVO.RankedDish toRanked(AiCandidate candidate, String reason) {
        DishVO dish = candidate.dish;
        AiRecommendVO.RankedDish ranked = new AiRecommendVO.RankedDish();
        ranked.setDishId(dish.getId());
        ranked.setDishName(dish.getName());
        ranked.setDishCover(dish.getCover());
        ranked.setReason(StringUtils.trim(reason));
        ranked.setHaveFoods(candidate.have);
        ranked.setLackFoods(candidate.lack);
        return ranked;
    }

    /**
     * 解析第二组新菜谱，字段名与 FreshDish 完全对齐，直接反序列化并截断到人数推导上限
     */
    private List<AiRecommendVO.FreshDish> resolveFresh(JSONObject result, int freshLimit) {
        List<AiRecommendVO.FreshDish> fresh = new ArrayList<>();
        JSONArray freshArr = result.getJSONArray("fresh");
        if (freshArr == null) {
            return fresh;
        }
        int size = Math.min(freshLimit, freshArr.size());
        for (int i = 0; i < size; i++) {
            AiRecommendVO.FreshDish fd = JSONUtil.toBean(freshArr.getJSONObject(i), AiRecommendVO.FreshDish.class);
            if (fd != null && StringUtils.isNotBlank(fd.getName())) {
                fresh.add(fd);
            }
        }
        return fresh;
    }

    /**
     * 辛香料分流：命中黑名单的“食材”转为调料追加到 seasoningsOut，返回剔除后的真实食材列表
     */
    private List<AiRecommendVO.FreshFood> splitAromatics(List<AiRecommendVO.FreshFood> foods,
                                                          List<AiRecommendVO.FreshSeasoning> seasoningsOut) {
        List<AiRecommendVO.FreshFood> kept = new ArrayList<>();
        if (CollectionUtils.isEmpty(foods)) {
            return kept;
        }
        for (AiRecommendVO.FreshFood food : foods) {
            if (food == null) {
                continue;
            }
            if (isAromatic(food.getName())) {
                AiRecommendVO.FreshSeasoning moved = new AiRecommendVO.FreshSeasoning();
                moved.setName(food.getName());
                moved.setAmount(food.getAmount());
                moved.setIsOptional(nvl(food.getIsOptional()));
                seasoningsOut.add(moved);
            } else {
                kept.add(food);
            }
        }
        return kept;
    }

    /**
     * 归一化后按关键字包含判定，兼容“蒜末/葱花/姜丝”等变体
     */
    private boolean isAromatic(String name) {
        if (StringUtils.isBlank(name)) {
            return false;
        }
        String n = StringUtils.normalizeName(name);
        for (String keyword : AROMATIC_KEYWORDS) {
            if (n.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private List<DishFoodDTO> buildFoods(Long kitchenId, List<AiRecommendVO.FreshFood> foods, Long userId) {
        List<DishFoodDTO> list = new ArrayList<>();
        if (CollectionUtils.isEmpty(foods)) {
            return list;
        }
        List<String> names = foods.stream()
                .map(AiRecommendVO.FreshFood::getName)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
        Map<String, Long> idMap = foodService.matchOrCreateByNames(kitchenId, names, userId);
        Set<Long> used = new LinkedHashSet<>();
        int sort = 0;
        for (AiRecommendVO.FreshFood food : foods) {
            Long foodId = idMap.get(StringUtils.normalizeName(food.getName()));
            if (foodId == null || !used.add(foodId)) {
                continue;
            }
            list.add(new DishFoodDTO()
                    .setFoodId(foodId)
                    .setAmount(food.getAmount())
                    .setProcess(food.getProcess())
                    .setIsOptional(nvl(food.getIsOptional()))
                    .setSort(sort++));
        }
        return list;
    }

    private List<DishSeasoningDTO> buildSeasonings(Long kitchenId, List<AiRecommendVO.FreshSeasoning> seasonings, Long userId) {
        List<DishSeasoningDTO> list = new ArrayList<>();
        if (CollectionUtils.isEmpty(seasonings)) {
            return list;
        }
        List<String> names = seasonings.stream()
                .map(AiRecommendVO.FreshSeasoning::getName)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
        Map<String, Long> idMap = seasoningService.matchOrCreateByNames(kitchenId, names, userId);
        Set<Long> used = new LinkedHashSet<>();
        int sort = 0;
        for (AiRecommendVO.FreshSeasoning seasoning : seasonings) {
            Long seasoningId = idMap.get(StringUtils.normalizeName(seasoning.getName()));
            if (seasoningId == null || !used.add(seasoningId)) {
                continue;
            }
            list.add(new DishSeasoningDTO()
                    .setSeasoningId(seasoningId)
                    .setAmount(seasoning.getAmount())
                    .setIsOptional(nvl(seasoning.getIsOptional()))
                    .setSort(sort++));
        }
        return list;
    }

    private List<DishStepDTO> buildSteps(List<String> steps) {
        List<DishStepDTO> list = new ArrayList<>();
        if (CollectionUtils.isEmpty(steps)) {
            return list;
        }
        int sort = 0;
        for (String description : steps) {
            if (StringUtils.isBlank(description)) {
                continue;
            }
            list.add(new DishStepDTO().setDescription(StringUtils.trim(description)).setSort(sort++));
        }
        return list;
    }

    /**
     * 分类提示仅尝试匹配现有分类名，匹配不到则不设分类（不自动新建，避免白耗分类额度）
     */
    private Long matchCategoryId(Long kitchenId, String categoryHint) {
        if (StringUtils.isBlank(categoryHint)) {
            return null;
        }
        String key = StringUtils.normalizeName(categoryHint);
        return categoryService.list(kitchenId).stream()
                .filter(c -> StringUtils.normalizeName(c.getName()).equals(key))
                .map(CategoryVO::getId)
                .findFirst().orElse(null);
    }

    /**
     * 分类提示匹配现有分类（返回完整 CategoryVO 以便同时取 id 与展示名）；匹配不到返回 null，不自动新建
     */
    private CategoryVO matchCategory(Long kitchenId, String categoryHint) {
        if (StringUtils.isBlank(categoryHint)) {
            return null;
        }
        String key = StringUtils.normalizeName(categoryHint);
        return categoryService.list(kitchenId).stream()
                .filter(c -> StringUtils.normalizeName(c.getName()).equals(key))
                .findFirst().orElse(null);
    }

    /**
     * 文案识别专用辛香料分流：命中黑名单的“食材”转为调料追加到 seasoningsOut，返回剔除后的真实食材列表
     */
    private List<AiParseVO.Food> splitParseAromatics(List<AiParseVO.Food> foods,
                                                     List<AiParseVO.Seasoning> seasoningsOut) {
        List<AiParseVO.Food> kept = new ArrayList<>();
        if (CollectionUtils.isEmpty(foods)) {
            return kept;
        }
        for (AiParseVO.Food food : foods) {
            if (food == null) {
                continue;
            }
            if (isAromatic(food.getName())) {
                AiParseVO.Seasoning moved = new AiParseVO.Seasoning()
                        .setName(food.getName())
                        .setAmount(food.getAmount())
                        .setIsOptional(nvl(food.getIsOptional()));
                seasoningsOut.add(moved);
            } else {
                kept.add(food);
            }
        }
        return kept;
    }

    /**
     * 名字→食材 ID（命中复用/未命中新建），同时携带展示名供前端渲染行；去重、保序
     */
    private List<AiParseVO.Food> buildParseFoods(Long kitchenId, List<AiParseVO.Food> foods, Long userId) {
        List<AiParseVO.Food> list = new ArrayList<>();
        if (CollectionUtils.isEmpty(foods)) {
            return list;
        }
        List<String> names = foods.stream()
                .map(AiParseVO.Food::getName)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
        Map<String, Long> idMap = foodService.matchOrCreateByNames(kitchenId, names, userId);
        Set<Long> used = new LinkedHashSet<>();
        for (AiParseVO.Food food : foods) {
            Long foodId = idMap.get(StringUtils.normalizeName(food.getName()));
            if (foodId == null || !used.add(foodId)) {
                continue;
            }
            list.add(new AiParseVO.Food()
                    .setFoodId(foodId)
                    .setName(StringUtils.trim(food.getName()))
                    .setAmount(food.getAmount())
                    .setProcess(food.getProcess())
                    .setIsOptional(nvl(food.getIsOptional())));
        }
        return list;
    }

    /**
     * 名字→调料 ID，同 buildParseFoods
     */
    private List<AiParseVO.Seasoning> buildParseSeasonings(Long kitchenId, List<AiParseVO.Seasoning> seasonings, Long userId) {
        List<AiParseVO.Seasoning> list = new ArrayList<>();
        if (CollectionUtils.isEmpty(seasonings)) {
            return list;
        }
        List<String> names = seasonings.stream()
                .map(AiParseVO.Seasoning::getName)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
        Map<String, Long> idMap = seasoningService.matchOrCreateByNames(kitchenId, names, userId);
        Set<Long> used = new LinkedHashSet<>();
        for (AiParseVO.Seasoning seasoning : seasonings) {
            Long seasoningId = idMap.get(StringUtils.normalizeName(seasoning.getName()));
            if (seasoningId == null || !used.add(seasoningId)) {
                continue;
            }
            list.add(new AiParseVO.Seasoning()
                    .setSeasoningId(seasoningId)
                    .setName(StringUtils.trim(seasoning.getName()))
                    .setAmount(seasoning.getAmount())
                    .setIsOptional(nvl(seasoning.getIsOptional())));
        }
        return list;
    }

    /**
     * 调 LLM 并解析 JSON，失败降温度重试一次
     */
    private JSONObject callLlm(List<Map<String, Object>> messages) {
        try {
            return parseJson(llmClient.chat(messages, aiProperties.getLlm().getTemperature()));
        } catch (Exception e) {
            log.warn("AI 首次调用或解析失败，降温度重试", e);
            try {
                return parseJson(llmClient.chat(messages, RETRY_TEMPERATURE));
            } catch (Exception ex) {
                log.error("AI 重试仍失败", ex);
                throw new RunException("AI 开小差了，换个食材组合试试");
            }
        }
    }

    /**
     * 剥离模型可能夹带的 markdown 代码块围栏，截取首个 { 到末个 } 再解析
     */
    private JSONObject parseJson(String content) {
        if (StringUtils.isBlank(content)) {
            throw new RunException("AI 未返回结果");
        }
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new RunException("AI 返回格式异常");
        }
        return JSONUtil.parseObj(content.substring(start, end + 1));
    }

    private boolean isOptional(DishFoodVO item) {
        return item.getIsOptional() != null && item.getIsOptional() == 1;
    }

    private int nvl(Integer value) {
        return value == null ? 0 : value;
    }

    /**
     * 人数 → 第一组（现有能做）展示上限：1 人 2 道 / 2-3 人 3 道 / 4 人+ RANKED_MAX
     */
    private int rankedLimitForPeople(Integer people) {
        int p = people == null || people <= 0 ? DEFAULT_PEOPLE : people;
        if (p <= 1) {
            return 2;
        }
        if (p <= 3) {
            return 3;
        }
        return RANKED_MAX;
    }

    /**
     * 人数 → 第二组（新菜谱）生成条数：1 人 1 道 / 2-3 人 2 道 / 4 人+ FRESH_MAX
     */
    private int freshCountForPeople(Integer people) {
        int p = people == null || people <= 0 ? DEFAULT_PEOPLE : people;
        if (p <= 1) {
            return 1;
        }
        if (p <= 3) {
            return 2;
        }
        return FRESH_MAX;
    }

    /**
     * 粗筛候选：菜谱 + 命中率 + 已有/缺少食材
     */
    private static class AiCandidate {
        private final DishVO dish;
        private final double rate;
        private final List<String> have;
        private final List<String> lack;

        private AiCandidate(DishVO dish, double rate, List<String> have, List<String> lack) {
            this.dish = dish;
            this.rate = rate;
            this.have = have;
            this.lack = lack;
        }
    }
}
