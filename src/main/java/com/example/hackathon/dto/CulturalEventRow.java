package com.example.hackathon.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CulturalEventRow {
    
    @JsonProperty("GUNAME")
    private String guname;      // 자치구
    
    @JsonProperty("TITLE")
    private String title;       // 공연/행사명
    
    @JsonProperty("DATE")
    private String date;        // 날짜/시간
    
    @JsonProperty("PLACE")
    private String place;       // 장소
    
    @JsonProperty("ORG_LINK")
    private String orgLink;     // 홈페이지 주소
    
    @JsonProperty("HMPG_ADDR")
    private String hmpgAddr;    // 문화포털상세URL
    
    @JsonProperty("CODENAME")
    private String codename;    // 분류
    
    @JsonProperty("USE_TRGT")
    private String useTrgt;     // 이용대상
    
    @JsonProperty("USE_FEE")
    private String useFee;      // 이용요금
    
    @JsonProperty("PLAYER")
    private String player;      // 출연자정보
    
    @JsonProperty("PROGRAM")
    private String program;     // 프로그램소개
    
    @JsonProperty("ETC_DESC")
    private String etcDesc;     // 기타내용
    
    @JsonProperty("MAIN_IMG")
    private String mainImg;     // 대표이미지
    
    @JsonProperty("RGSTDATE")
    private String rgstdate;    // 신청일
    
    @JsonProperty("TICKET")
    private String ticket;      // 시민/기관
    
    @JsonProperty("STRTDATE")
    private String strtdate;    // 시작일
    
    @JsonProperty("END_DATE")
    private String endDate;     // 종료일
    
    @JsonProperty("THEMECODE")
    private String themecode;   // 테마분류
    
    @JsonProperty("LOT")
    private String lot;         // 위도(Y좌표)
    
    @JsonProperty("LAT")
    private String lat;         // 경도(X좌표)
    
    @JsonProperty("IS_FREE")
    private String isFree;      // 유무료
}
