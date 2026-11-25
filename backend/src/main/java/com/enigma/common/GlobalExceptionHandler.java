package com.enigma.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<R<Void>> handleBusiness(BusinessException e) {
        HttpStatus status = HttpStatus.resolve(e.getCode());
        if (status == null) {
            status = HttpStatus.BAD_REQUEST;
        }
        return ResponseEntity.status(status).body(R.fail(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<R<Void>> handleValidation(MethodArgumentNotValidException e) {
        FieldError error = e.getBindingResult().getFieldError();
        String message = error == null ? "参数校验失败" : error.getField() + " " + error.getDefaultMessage();
        return ResponseEntity.badRequest().body(R.fail(400, message));
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<R<Void>> handleDuplicate(DuplicateKeyException e) {
        return ResponseEntity.badRequest().body(R.fail(400, "数据重复，请勿重复提交或重复配置"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<R<Void>> handleIntegrity(DataIntegrityViolationException e) {
        log.warn("数据完整性校验失败: {}", e.getMessage());
        return ResponseEntity.badRequest().body(R.fail(400, "数据存在关联引用，无法完成当前操作"));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<R<Void>> handleBadCredentials(BadCredentialsException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(R.fail(401, "用户名或密码错误"));
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<R<Void>> handleDisabled(DisabledException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(R.fail(403, "账号已被禁用，请联系管理员"));
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<R<Void>> handleUserNotFound(UsernameNotFoundException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(R.fail(401, "用户名或密码错误"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<R<Void>> handleAccessDenied(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(R.fail(403, "没有操作权限"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<R<Void>> handleOther(Exception e) {
        log.error("服务器内部错误", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(R.fail(500, "服务器内部错误：" + e.getMessage()));
    }
}
