package com.example.hackathon.dto;

/**
 * 챗봇 질문 요청을 위한 Record (JDK 21 기능 활용)
 * 
 * @param question 사용자가 입력한 질문
 * @author 해커톤 팀
 * @version 2.0
 */
public record ChatbotRequest(String question) {}
