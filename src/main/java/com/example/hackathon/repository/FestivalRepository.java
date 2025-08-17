package com.example.hackathon.repository;

import com.example.hackathon.entity.Festival;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FestivalRepository extends JpaRepository<Festival, Long> {
    List<Festival> findByDistrict(String district);
    List<Festival> findByNameContaining(String name);
    List<Festival> findByDistrictAndNameContaining(String district, String name);
    boolean existsByUniqueKey(String uniqueKey);
}
