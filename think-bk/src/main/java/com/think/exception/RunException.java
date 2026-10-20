package com.think.exception;

import com.think.common.enums.ResultCodeEnum;
import lombok.Data;

/**
 * 自定义异常
 * @author 好市民
 */
@Data
public class RunException extends RuntimeException {
    /**
     * 异常状态码
     */
    private Integer code = ResultCodeEnum.ERROR.getCode();
    /**
     * 异常信息
     */
    private String message = ResultCodeEnum.ERROR.getMessage();

    public RunException() {
        super();
    }
    public RunException(Integer code) {
        super();
        this.code = code;
    }
    public RunException(String message) {
        super();
        this.message = message;
    }
    public RunException(Integer code, String message) {
        super();
        this.code = code;
        this.message = message;
    }
    public RunException(ResultCodeEnum resultCodeEnum) {
        super();
        this.code = resultCodeEnum.getCode();
        this.message = resultCodeEnum.getMessage();
    }
}
