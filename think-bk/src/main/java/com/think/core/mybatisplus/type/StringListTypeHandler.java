package com.think.core.mybatisplus.type;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 字符串列表 &harr; 逗号分隔字符串 编解码（列为普通 VARCHAR，非 JSON）。
 * <p>
 * 仅经 {@code @TableField(typeHandler = StringListTypeHandler.class)} 显式绑定使用，
 * 未做全局注册（不加 {@code @MappedTypes}），故不影响其它 List 字段。
 * 约定：null 列 &harr; null；空列表写入 NULL；读回空白串返回空列表。
 * 分隔符逗号，适用于 URL 等不含逗号的短字符串集合（如决策单图片，最多 3 张）。
 * </p>
 *
 * @author 好市民
 */
public class StringListTypeHandler extends BaseTypeHandler<List<String>> {

    private static final String SEPARATOR = ",";

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<String> parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, join(parameter));
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return split(rs.getString(columnName));
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return split(rs.getString(columnIndex));
    }

    @Override
    public List<String> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return split(cs.getString(columnIndex));
    }

    /**
     * 列表 -> 逗号分隔字符串；null 或空列表统一落 NULL。
     */
    private String join(List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.joining(SEPARATOR));
    }

    /**
     * 逗号分隔字符串 -> 列表；null 列返回 null，空白串返回空列表。
     */
    private List<String> split(String value) {
        if (value == null) {
            return null;
        }
        if (value.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(value.split(SEPARATOR, -1))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
