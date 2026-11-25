package com.enigma.common;

/**
 * 统一接口返回结构。
 */
public record R<T>(int code, String message, T data) {

    public static <T> R<T> ok(T data) {
        return new R<>(200, "success", data);
    }

    public static R<Void> ok() {
        return new R<>(200, "success", null);
    }

    public static <T> R<T> fail(int code, String message) {
        return new R<>(code, message, null);
    }
}
