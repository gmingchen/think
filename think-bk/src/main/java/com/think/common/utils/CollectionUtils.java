package com.think.common.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Collection 工具类
 * @author 好市民
 */
public class CollectionUtils extends CollUtil {

    public static <T> List<T> unionDistinctToList(Collection<T> coll1, Collection<T> coll2) {
        return new ArrayList<>(unionDistinct(coll1, coll2));
    }

    public static <T> List<T> convertList(Collection<T> collection) {
        if (isEmpty(collection)) {
            return new ArrayList<>();
        }
        return collection.stream().filter(Objects::nonNull).collect(Collectors.toList());
    }

    public static <T, U> List<U> mapList(Collection<T> collection, Function<T, U> map) {
        if (isEmpty(collection)) {
            return new ArrayList<>();
        }
        return collection.stream().map(map).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public static <T> List<T> filterList(Collection<T> collection, Predicate<T> filter) {
        if (isEmpty(collection)) {
            return new ArrayList<>();
        }
        return collection.stream().filter(filter).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public static <T, U> List<U> filterMapList(Collection<T> collection, Predicate<T> filter, Function<T, U> map) {
        if (isEmpty(collection)) {
            return new ArrayList<>();
        }
        return collection.stream().filter(filter).map(map).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public static <T> List<T> page(Long pageNo, Long pageSize, List<T> list) {
        return ListUtil.page(pageNo.intValue(), pageSize.intValue(), list);
    }
}
