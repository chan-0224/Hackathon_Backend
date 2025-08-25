package com.example.hackathon.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CulturalEventApiResponse {
    
    @JsonProperty("culturalEventInfo")
    private CulturalEventInfo culturalEventInfo;
}
