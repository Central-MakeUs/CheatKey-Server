package com.cheatkey.module.terms.domain.repository;

import com.cheatkey.module.terms.domain.entity.Terms;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TermsRepository extends JpaRepository<Terms, Long> {
    List<Terms> findAllByOrderByRequiredDesc(); // 필수 우선 정렬용

    List<Terms> findAllByRequiredTrue();
}
