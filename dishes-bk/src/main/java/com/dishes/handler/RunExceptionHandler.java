package com.dishes.handler;

import com.dishes.common.enums.ResultCodeEnum;
import com.dishes.common.pojo.Result;
import com.dishes.exception.RunException;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.sql.SQLException;
import java.util.List;

/**
 * 全局异常处理器
 * @author 好市民
 */
@Slf4j
@RestControllerAdvice
public class RunExceptionHandler {

    /**
     * 运行时自定义异常
     * @param e
     * @return
     */
    @ExceptionHandler(RunException.class)
    public Result<String> runExceptionHandle(RunException e) {
        log.error(e.getMessage(), e);
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 404异常
     * @param e
     * @return
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public Result<String> noFoundExceptionHandle(NoHandlerFoundException e) {
        log.error(e.getMessage(), e);
        return Result.error(ResultCodeEnum.NOT_FOUND);
    }

    /**
     * 请求方法异常
     * @param e
     * @return
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<String> httpRequestMethodNotSupportedExceptionHandle(HttpRequestMethodNotSupportedException e){
        log.error(e.getMessage(), e);
        return Result.error(ResultCodeEnum.METHOD_ERROR);
    }

    /**
     * 没有权限
     * @param e
     * @return
     */
    @ExceptionHandler(AccessDeniedException.class)
    public Result<String> accessDeniedExceptionHandle(AccessDeniedException e){
        return Result.error(ResultCodeEnum.NOT_ALLOWED);
    }

    /**
     * URL拼接参数校验异常
     * @param e
     * @return
     */
    @ExceptionHandler(BindException.class)
    public Result<String> handleBindException(BindException e) {
        log.error(e.getMessage(), e);
        return Result.error(
                ResultCodeEnum.VERIFICATION_ERROR.getCode(),
                validatedErrorMessage(e.getBindingResult().getFieldErrors())
        );
    }
    /**
     * Body参数校验异常
     * @param e
     * @return
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<String> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.error(e.getMessage(), e);
        return Result.error(
                ResultCodeEnum.VERIFICATION_ERROR.getCode(),
                validatedErrorMessage(e.getBindingResult().getFieldErrors())
        );
    }
    /**
     * 参数校验异常信息处理
     * @param list 字段异常数组
     * @return
     */
    private String validatedErrorMessage(List<FieldError> list) {
        StringBuilder message = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            FieldError fieldError = list.get(i);
            message.append(fieldError.getField())
                    .append("-")
                    .append(fieldError.getDefaultMessage())
                    .append(i < list.size() - 1 ? "," : "");
        }
        return message.toString();
    }

    /**
     * 数据库key重复异常
     * @param e
     * @return
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public Result<String> duplicateKeyExceptionHandle(DuplicateKeyException e){
        log.error(e.getMessage(), e);
        return Result.error(ResultCodeEnum.DUPLICATE_KEY);
    }

    /**
     * Sql异常
     * @param e
     * @return
     */
    @ExceptionHandler(BadSqlGrammarException.class)
    public Result<String> badSqlGrammarExceptionHandle(BadSqlGrammarException e){
        log.error(e.getMessage(), e);
        return Result.error(ResultCodeEnum.BAD_SQL);
    }

    /**
     * Sql异常
     * @param e
     * @return
     */
    @ExceptionHandler(SQLException.class)
    public Result<String> sqlExceptionHandle(SQLException e){
        log.error(e.getMessage(), e);
        return Result.error(ResultCodeEnum.BAD_SQL);
    }

    /**
     * 数据完整性异常
     * @param e
     * @return
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public Result<String> dataIntegrityViolationExceptionHandle(DataIntegrityViolationException e){
        log.error(e.getMessage(), e);
        return Result.error(ResultCodeEnum.DATA_INTEGRITY_VIOLATION);
    }

    /**
     * 浏览器关闭了连接异常捕获
     * @param e
     * @return
     */
    @ExceptionHandler(ClientAbortException.class)
    public Result<?> clientAbortExceptionHandle(ClientAbortException e){
        return Result.success();
    }

    /**
     * 服务端异常
     * @param e
     * @return
     */
    @ExceptionHandler(Exception.class)
    public Result<String> exceptionHandle(Exception e){
        log.error(e.getMessage(), e);
        return Result.error(ResultCodeEnum.ERROR);
    }
}
