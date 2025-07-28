package com.cheatkey.module.auth.domain.repository;

import com.cheatkey.module.auth.domain.entity.ProfileImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProfileImageRepository extends JpaRepository<ProfileImage, Long> {

    @Query("SELECT p FROM ProfileImage p WHERE p.isActive = true ORDER BY p.displayOrder")
    List<ProfileImage> findAllActiveOrderByDisplayOrder();

    @Query("SELECT p FROM ProfileImage p WHERE p.id = :id AND p.isActive = true")
    ProfileImage findByIdAndActive(@Param("id") Long id);
} 