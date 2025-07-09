package com.cheatkey.module.terms.domain.repository;

import com.cheatkey.module.auth.domain.entity.Auth;
import com.cheatkey.module.terms.domain.entity.AuthTermsAgreement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuthTermsAgreementRepository extends JpaRepository<AuthTermsAgreement, Long> {
    List<AuthTermsAgreement> findAllByAuth(Auth auth);

}
