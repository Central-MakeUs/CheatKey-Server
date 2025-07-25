package com.cheatkey.module.terms.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "t_terms")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Terms {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String subTitle;

    @Column(columnDefinition = "TEXT")
    private String contents;

    private boolean required;

    private String version;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

