package com.think.common.pojo;

import com.think.common.enums.ResultCodeEnum;
import lombok.Data;

import java.io.Serializable;

/**
 * 通用返回
 * @author 好市民
 */
@Data
public class Result<T> implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 错误码
     */
    private Integer code;
    /**
     * 用户可阅读的消息提示
     */
    private String message;
    /**
     * 返回数据
     */
    private T data;

    public static Result<?> success() {
        Result<?> result = new Result<>();
        result.code = ResultCodeEnum.SUCCESS.getCode();
        result.message = ResultCodeEnum.SUCCESS.getMessage();
        return result;
    }

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.code = ResultCodeEnum.SUCCESS.getCode();
        result.message = ResultCodeEnum.SUCCESS.getMessage();
        result.data = data;
        return result;
    }

    public static <T> Result<T> error() {
        return error(ResultCodeEnum.ERROR.getCode(), ResultCodeEnum.ERROR.getMessage());
    }

    public static <T> Result<T> error(ResultCodeEnum resultCodeEnum) {
        return error(resultCodeEnum.getCode(), resultCodeEnum.getMessage());
    }

    public static <T> Result<T> error(ResultCodeEnum resultCodeEnum, T data) {
        return error(resultCodeEnum.getCode(), resultCodeEnum.getMessage(), data);
    }

    public static <T> Result<T> error(Integer code, String message) {
        return error(code, message, null);
    }

    public static <T> Result<T> error(Integer code, String message, T data) {
        Result<T> result = new Result<>();
        result.code = code;
        result.message = message;
        result.data = data;
        return result;
    }
}
