package com.dishes.modules.member.convert;

import com.dishes.common.convert.BaseConvert;
import com.dishes.modules.member.entity.MemberOrderEntity;
import com.dishes.modules.member.model.vo.MemberRecordVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 会员
 * <p>
 * 只做订单 → 开通记录这一条映射（§9.1）。套餐与卡片的字段要按档位拼文案、要算价格串，
 * 不是同名拷贝，所以留给 service 手工组装，不在此处硬凑 @Mapping。
 * </p>
 * @author 好市民
 */
@Mapper
public interface MemberConvert extends BaseConvert {
    MemberConvert INSTANCE = Mappers.getMapper(MemberConvert.class);

    /**
     * 订单 → 开通记录行
     * <p>
     * amount 取 actual_price（实付），因为记录页回答的是「我付了多少」而不是「道具原价多少」；
     * 首单那笔会显示 ¥2.9 而不是 ¥7.9。
     * </p>
     */
    @Mapping(source = "actualPrice", target = "amount")
    MemberRecordVO convertRecord(MemberOrderEntity bean);

    List<MemberRecordVO> convertRecordList(List<MemberOrderEntity> list);
}
