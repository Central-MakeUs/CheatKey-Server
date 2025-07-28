package com.cheatkey.module.auth.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "t_auth_profile_images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ProfileImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String imageUrl;
    private String imageName;
    private Integer displayOrder;
    private Boolean isActive;
} 