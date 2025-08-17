package com.example.hackathon.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "festivals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Festival {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false)
    private String date;
    
    @Column(nullable = false)
    private String district;
    
    @Column(nullable = false)
    private String place;
    
    @Column(length = 1000)
    private String link;
    
    @Column(unique = true, length = 500)
    private String uniqueKey;
}
