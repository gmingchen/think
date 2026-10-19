package com.dishes.modules.cookorder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dishes.common.enums.CookOrderStatusEnum;
import com.dishes.common.enums.KitchenRoleEnum;
import com.dishes.common.pojo.PageResult;
import com.dishes.common.utils.CollectionUtils;
import com.dishes.core.mybatisplus.service.ServiceImplX;
import com.dishes.core.security.utils.SecurityUtils;
import com.dishes.exception.RunException;
import com.dishes.modules.cookorder.convert.CookOrderConvert;
import com.dishes.modules.cookorder.entity.CookOrderEntity;
import com.dishes.modules.cookorder.mapper.CookOrderMapper;
import com.dishes.modules.cookorder.model.dto.*;
import com.dishes.modules.cookorder.model.vo.CookOrderInfoVO;
import com.dishes.modules.cookorder.model.vo.CookOrderVO;
import com.dishes.modules.cookorder.service.CookOrderDishService;
import com.dishes.modules.cookorder.service.CookOrderService;
import com.dishes.modules.cookreview.entity.CookReviewEntity;
import com.dishes.modules.cookreview.mapper.CookReviewMapper;
import com.dishes.modules.cookreview.model.dto.CookReviewCreateDTO;
import com.dishes.modules.cookreview.model.dto.CookReviewDishDTO;
import com.dishes.modules.cookreview.service.CookReviewService;
import com.dishes.modules.dish.entity.DishEntity;
import com.dishes.modules.dish.service.DishService;
import com.dishes.modules.kitchen.entity.KitchenUserEntity;
import com.dishes.modules.kitchen.service.KitchenUserService;
import com.dishes.modules.user.entity.UserEntity;
import com.dishes.modules.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 烹饪订单
 * @author 好市民
 */
@Service("cookOrderService")
public class CookOrderServiceImpl extends ServiceImplX<CookOrderMapper, CookOrderEntity> implements CookOrderService {

    @Autowired
    private KitchenUserService kitchenUserService;
    @Autowired
    private CookOrderDishService cookOrderDishService;
    @Autowired
    private CookReviewMapper cookReviewMapper;
    @Autowired
    private CookReviewService cookReviewService;
    @Autowired
    private DishService dishService;
    @Autowired
    private UserService userService;

    @Override
    public PageResult<CookOrderVO> page(CookOrderPageDTO dto) {
        Long userId = SecurityUtils.getLoginUserId();
        KitchenUserEntity kitchenUser = kitchenUserService.checkMember(dto.getKitchenId(), userId);
        // 食客只能看自己的订单
        boolean isGuest = kitchenUser.getRole().equals(KitchenRoleEnum.GUEST.getCode());
        LambdaQueryWrapper<CookOrderEntity> wrapper = new LambdaQueryWrapper<CookOrderEntity>()
                .eq(CookOrderEntity::getKitchenId, dto.getKitchenId());
        if (isGuest || (dto.getType() != null && dto.getType() == 1)) {
            wrapper.eq(CookOrderEntity::getOrdererId, userId);
        }
        wrapper.orderByDesc(CookOrderEntity::getDate)
                .orderByDesc(CookOrderEntity::getTimePeriod);
        PageResult<CookOrderEntity> pageResult = baseMapper.selectPage(dto, wrapper);
        List<CookOrderVO> voList = CollectionUtils.mapList(pageResult.getList(), CookOrderConvert.INSTANCE::convert);
        // 填充菜品列表
        for (CookOrderVO vo : voList) {
            vo.setDishes(cookOrderDishService.list(vo.getId()));
        }
        // 填充下单人昵称
        List<Long> ordererIds = voList.stream().map(CookOrderVO::getOrdererId).distinct().collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(ordererIds)) {
            Map<Long, String> nameMap = userService.listByIds(ordererIds).stream()
                    .collect(Collectors.toMap(u -> u.getId(), u -> u.getNickname() != null ? u.getNickname() : "", (a, b) -> a));
            voList.forEach(vo -> vo.setOrdererName(nameMap.getOrDefault(vo.getOrdererId(), "")));
        }
        // 填充已评价订单的评分
        List<Long> reviewedOrderIds = voList.stream()
                .filter(vo -> CookOrderStatusEnum.REVIEWED.getCode().equals(vo.getStatus()))
                .map(CookOrderVO::getId)
                .collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(reviewedOrderIds)) {
            LambdaQueryWrapper<CookReviewEntity> reviewWrapper = new LambdaQueryWrapper<CookReviewEntity>()
                    .in(CookReviewEntity::getCookOrderId, reviewedOrderIds);
            List<CookReviewEntity> reviews = cookReviewMapper.selectList(reviewWrapper);
            Map<Long, Double> ratingMap = reviews.stream()
                    .collect(Collectors.groupingBy(
                            CookReviewEntity::getCookOrderId,
                            Collectors.averagingDouble(r -> r.getRating().doubleValue())
                    ));
            voList.forEach(vo -> {
                if (ratingMap.containsKey(vo.getId())) {
                    vo.setRating(Math.round(ratingMap.get(vo.getId()) * 10.0) / 10.0);
                }
            });
        }
        return new PageResult<>(pageResult.getTotal(), pageResult.getPages(), voList)
                .setCurrent(pageResult.getCurrent());
    }

    @Override
    public List<CookOrderInfoVO> today(Long kitchenId) {
        kitchenUserService.checkMember(kitchenId, SecurityUtils.getLoginUserId());
        LambdaQueryWrapper<CookOrderEntity> wrapper = new LambdaQueryWrapper<CookOrderEntity>()
                .eq(CookOrderEntity::getKitchenId, kitchenId)
                .eq(CookOrderEntity::getDate, LocalDate.now())
                .ne(CookOrderEntity::getStatus, CookOrderStatusEnum.CANCELLED.getCode())
                .orderByAsc(CookOrderEntity::getTimePeriod)
                .orderByAsc(CookOrderEntity::getStatus);
        List<CookOrderEntity> entities = list(wrapper);
        return fillOrders(entities);
    }

    @Override
    public List<CookOrderInfoVO> latest(Long kitchenId) {
        kitchenUserService.checkMember(kitchenId, SecurityUtils.getLoginUserId());
        int currentPeriod = getCurrentTimePeriod();
        // 1. 从当前时段开始，找最近的一单
        LambdaQueryWrapper<CookOrderEntity> wrapper = new LambdaQueryWrapper<CookOrderEntity>()
                .eq(CookOrderEntity::getKitchenId, kitchenId)
                .ge(CookOrderEntity::getDate, LocalDate.now())
                .and(w -> w
                        .gt(CookOrderEntity::getDate, LocalDate.now())
                        .or(o -> o.eq(CookOrderEntity::getDate, LocalDate.now())
                                .ge(CookOrderEntity::getTimePeriod, currentPeriod)))
                .ne(CookOrderEntity::getStatus, CookOrderStatusEnum.CANCELLED.getCode())
                .orderByAsc(CookOrderEntity::getDate)
                .orderByAsc(CookOrderEntity::getTimePeriod)
                .last("LIMIT 1");
        CookOrderEntity nearest = baseMapper.selectOne(wrapper);
        if (nearest == null) {
            return Collections.emptyList();
        }
        // 2. 查出该日期+时段的所有订单
        LambdaQueryWrapper<CookOrderEntity> targetWrapper = new LambdaQueryWrapper<CookOrderEntity>()
                .eq(CookOrderEntity::getKitchenId, kitchenId)
                .eq(CookOrderEntity::getDate, nearest.getDate())
                .eq(CookOrderEntity::getTimePeriod, nearest.getTimePeriod())
                .ne(CookOrderEntity::getStatus, CookOrderStatusEnum.CANCELLED.getCode());
        return fillOrders(list(targetWrapper));
    }

    /**
     * 根据当前小时判断时段：1-早餐(5-10) 2-午餐(10-15) 3-晚餐(15-22) 4-宵夜(22-5)
     */
    private int getCurrentTimePeriod() {
        int hour = LocalDateTime.now().getHour();
        if (hour >= 5 && hour < 10) return 1;
        if (hour >= 10 && hour < 15) return 2;
        if (hour >= 15 && hour < 22) return 3;
        return 4;
    }

    @Override
    public CookOrderInfoVO info(Long id) {
        CookOrderEntity entity = validateExists(id);
        kitchenUserService.checkMember(entity.getKitchenId(), SecurityUtils.getLoginUserId());
        CookOrderInfoVO vo = CookOrderConvert.INSTANCE.convertInfo(entity);
        vo.setDishes(cookOrderDishService.list(id));
        // 填充下单人昵称
        if (vo.getOrdererId() != null) {
            UserEntity user = userService.getById(vo.getOrdererId());
            vo.setOrdererName(user != null && user.getNickname() != null ? user.getNickname() : "");
        }
        // 填充已评价订单的评分 & 每个菜品评价
        if (CookOrderStatusEnum.REVIEWED.getCode().equals(vo.getStatus())) {
            LambdaQueryWrapper<CookReviewEntity> reviewWrapper = new LambdaQueryWrapper<CookReviewEntity>()
                    .eq(CookReviewEntity::getCookOrderId, id);
            List<CookReviewEntity> reviews = cookReviewMapper.selectList(reviewWrapper);
            if (CollectionUtils.isNotEmpty(reviews)) {
                // 整体评分
                double avg = reviews.stream().collect(Collectors.averagingDouble(r -> r.getRating().doubleValue()));
                vo.setRating(Math.round(avg * 10.0) / 10.0);
                // 按菜品填充
                Map<Long, CookReviewEntity> reviewByDish = reviews.stream()
                        .collect(Collectors.toMap(CookReviewEntity::getDishId, r -> r, (a, b) -> a));
                vo.getDishes().forEach(dish -> {
                    CookReviewEntity review = reviewByDish.get(dish.getDishId());
                    if (review != null) {
                        dish.setRating(review.getRating());
                        dish.setContent(review.getContent());
                    }
                });
            }
        }
        return vo;
    }

    @Transactional(rollbackFor = RunException.class)
    @Override
    public Long create(CookOrderCreateDTO dto) {
        Long userId = SecurityUtils.getLoginUserId();
        // 下单全员开放：厨师长/管厨/食客均可点餐，仅需厨房成员身份（checkMember 非成员抛异常）
        kitchenUserService.checkMember(dto.getKitchenId(), userId);
        // 插入订单
        CookOrderEntity entity = CookOrderConvert.INSTANCE.convertEntity(dto)
                .setOrdererId(userId)
                .setStatus(CookOrderStatusEnum.PLACED.getCode());
        baseMapper.insert(entity);
        // 插入订单菜品
        cookOrderDishService.create(entity.getId(), dto.getDishes());
        return entity.getId();
    }

    @Transactional(rollbackFor = RunException.class)
    @Override
    public void update(CookOrderUpdateDTO dto) {
        CookOrderEntity entity = validateExists(dto.getId());
        checkOrderer(entity);
        checkStatus(entity, CookOrderStatusEnum.PLACED.getCode());
        // 更新基础信息
        CookOrderEntity updateEntity = CookOrderConvert.INSTANCE.convertEntity(dto);
        baseMapper.updateById(updateEntity);
        // 全量替换菜品
        cookOrderDishService.deleteByCookOrderId(dto.getId());
        cookOrderDishService.create(dto.getId(), dto.getDishes());
    }

    @Transactional(rollbackFor = RunException.class)
    @Override
    public void delete(CookOrderDeleteDTO dto) {
        CookOrderEntity entity = validateExists(dto.getId());
        checkPermission(entity);
        checkStatus(entity, CookOrderStatusEnum.PLACED.getCode());
        // 删除菜品关联
        cookOrderDishService.deleteByCookOrderId(dto.getId());
        // 删除订单
        baseMapper.deleteById(dto.getId());
    }

    @Override
    public void accept(CookOrderAcceptDTO dto) {
        CookOrderEntity entity = validateExists(dto.getId());
        kitchenUserService.checkPermission(entity.getKitchenId());
        checkStatus(entity, CookOrderStatusEnum.PLACED.getCode());
        entity.setStatus(CookOrderStatusEnum.ACCEPTED.getCode());
        baseMapper.updateById(entity);
    }

    @Override
    public void serve(CookOrderServeDTO dto) {
        CookOrderEntity entity = validateExists(dto.getId());
        kitchenUserService.checkMember(entity.getKitchenId(), SecurityUtils.getLoginUserId());
        checkStatus(entity, CookOrderStatusEnum.PLACED.getCode(), CookOrderStatusEnum.ACCEPTED.getCode());
        entity.setStatus(CookOrderStatusEnum.SERVED.getCode());
        entity.setServedAt(LocalDateTime.now());
        baseMapper.updateById(entity);
        // 增加菜品做过次数
        List<Long> dishIds = CollectionUtils.mapList(cookOrderDishService.list(dto.getId()), com.dishes.modules.cookorder.model.vo.CookOrderDishVO::getDishId);
        dishService.incrementCookCount(dishIds);
    }

    @Override
    public void cancel(CookOrderCancelDTO dto) {
        CookOrderEntity entity = validateExists(dto.getId());
        kitchenUserService.checkMember(entity.getKitchenId(), SecurityUtils.getLoginUserId());
        checkStatus(entity, CookOrderStatusEnum.PLACED.getCode(), CookOrderStatusEnum.ACCEPTED.getCode());
        entity.setStatus(CookOrderStatusEnum.CANCELLED.getCode());
        entity.setCancelledAt(LocalDateTime.now());
        baseMapper.updateById(entity);
    }

    @Transactional(rollbackFor = RunException.class)
    @Override
    public void review(CookReviewCreateDTO dto) {
        Long userId = SecurityUtils.getLoginUserId();
        // 校验订单
        CookOrderEntity order = validateExists(dto.getCookOrderId());
        if (!order.getOrdererId().equals(userId)) {
            throw new RunException("只有下单人才能评价");
        }
        if (!order.getStatus().equals(CookOrderStatusEnum.SERVED.getCode())) {
            throw new RunException("订单状态不允许评价");
        }
        // 批量插入评价
        cookReviewService.saveBatch(dto.getCookOrderId(), userId, dto.getDishes());
        // 更新订单状态为已评价
        order.setStatus(CookOrderStatusEnum.REVIEWED.getCode());
        order.setReviewedAt(LocalDateTime.now());
        baseMapper.updateById(order);
        // 更新菜品平均评分
        updateDishRating(dto.getDishes());
    }

    private void updateDishRating(List<CookReviewDishDTO> dishes) {
        for (CookReviewDishDTO dishDTO : dishes) {
            LambdaQueryWrapper<CookReviewEntity> wrapper = new LambdaQueryWrapper<CookReviewEntity>()
                    .eq(CookReviewEntity::getDishId, dishDTO.getDishId());
            List<CookReviewEntity> reviews = cookReviewMapper.selectList(wrapper);
            BigDecimal avg = reviews.stream()
                    .map(r -> BigDecimal.valueOf(r.getRating()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(reviews.size()), 1, BigDecimal.ROUND_HALF_UP);
            DishEntity dish = new DishEntity();
            dish.setId(dishDTO.getDishId());
            dish.setRating(avg);
            dishService.updateById(dish);
        }
    }

    // ==================== 公共填充 ====================

    private List<CookOrderInfoVO> fillOrders(List<CookOrderEntity> entities) {
        List<CookOrderInfoVO> voList = CollectionUtils.mapList(entities, CookOrderConvert.INSTANCE::convertInfo);
        // 填充菜品列表
        for (CookOrderInfoVO vo : voList) {
            vo.setDishes(cookOrderDishService.list(vo.getId()));
        }
        // 填充下单人昵称
        List<Long> ordererIds = voList.stream().map(CookOrderInfoVO::getOrdererId).distinct().collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(ordererIds)) {
            Map<Long, String> nameMap = userService.listByIds(ordererIds).stream()
                    .collect(Collectors.toMap(u -> u.getId(), u -> u.getNickname() != null ? u.getNickname() : "", (a, b) -> a));
            voList.forEach(vo -> vo.setOrdererName(nameMap.getOrDefault(vo.getOrdererId(), "")));
        }
        // 填充已评价订单的评分
        List<Long> reviewedOrderIds = voList.stream()
                .filter(vo -> CookOrderStatusEnum.REVIEWED.getCode().equals(vo.getStatus()))
                .map(CookOrderInfoVO::getId)
                .collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(reviewedOrderIds)) {
            LambdaQueryWrapper<CookReviewEntity> reviewWrapper = new LambdaQueryWrapper<CookReviewEntity>()
                    .in(CookReviewEntity::getCookOrderId, reviewedOrderIds);
            List<CookReviewEntity> reviews = cookReviewMapper.selectList(reviewWrapper);
            Map<Long, Double> ratingMap = reviews.stream()
                    .collect(Collectors.groupingBy(
                            CookReviewEntity::getCookOrderId,
                            Collectors.averagingDouble(r -> r.getRating().doubleValue())
                    ));
            voList.forEach(vo -> {
                if (ratingMap.containsKey(vo.getId())) {
                    vo.setRating(Math.round(ratingMap.get(vo.getId()) * 10.0) / 10.0);
                }
            });
        }
        return voList;
    }

    // ==================== 权限校验 ====================

    private void checkPermission(CookOrderEntity entity) {
        kitchenUserService.checkPermission(entity.getKitchenId());
    }

    private void checkOrderer(CookOrderEntity entity) {
        if (!entity.getOrdererId().equals(SecurityUtils.getLoginUserId())) {
            throw new RunException("只有下单人才能修改订单");
        }
    }

    private void checkStatus(CookOrderEntity entity, int... allowed) {
        for (int status : allowed) {
            if (entity.getStatus() == status) return;
        }
        throw new RunException("当前状态不允许此操作");
    }
}
