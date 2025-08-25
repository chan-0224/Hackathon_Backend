package com.example.hackathon.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "Spring Boot Hackathon Project가 성공적으로 실행되었습니다!<br>" +
               "<br>사용 가능한 엔드포인트:<br>" +
               "- <a href='/api/users'>GET /api/users</a> - 모든 사용자 조회<br>" +
               "<br>MySQL 데이터베이스가 연결되어 있습니다.<br>" +
               "API 테스트를 위해 POST 요청으로 사용자를 생성해보세요!";
    }
}
