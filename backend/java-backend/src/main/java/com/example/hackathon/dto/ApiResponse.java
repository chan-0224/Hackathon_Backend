package com.example.hackathon.dto;

import java.time.LocalDateTime;

/**
 * API 응답을 위한 Record (JDK 21 기능 활용)
 * 
 * @param success 성공 여부
 * @param message 응답 메시지
 * @param data 응답 데이터
 * @param timestamp 응답 시간
 * @param <T> 데이터 타입
 */
public record ApiResponse<T>(
    boolean success,
    String message,
    T data,
    LocalDateTime timestamp
) {
    /**
     * 성공 응답 생성
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "성공", data, LocalDateTime.now());
    }
    
    /**
     * 성공 응답 생성 (메시지 포함)
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, LocalDateTime.now());
    }
    
    /**
     * 실패 응답 생성
     */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null, LocalDateTime.now());
    }
    
    /**
     * 실패 응답 생성 (데이터 포함)
     */
    public static <T> ApiResponse<T> error(String message, T data) {
        return new ApiResponse<>(false, message, data, LocalDateTime.now());
    }
}
