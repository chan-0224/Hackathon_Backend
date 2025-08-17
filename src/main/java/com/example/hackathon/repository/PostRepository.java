package com.example.hackathon.repository;

import com.example.hackathon.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findByDistrict(String district);
    List<Post> findByDistrictOrderByCreatedAtDesc(String district);
}
