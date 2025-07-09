package com.cheatkey.module.terms.domain.service;

import com.cheatkey.common.exception.CustomException;
import com.cheatkey.common.exception.ErrorCode;
import com.cheatkey.module.auth.domain.entity.Auth;
import com.cheatkey.module.terms.domain.entity.AuthTermsAgreement;
import com.cheatkey.module.terms.domain.entity.Terms;
import com.cheatkey.module.terms.domain.repository.AuthTermsAgreementRepository;
import com.cheatkey.module.terms.domain.repository.TermsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TermsAgreementService {

    private final TermsRepository termsRepository;
    private final AuthTermsAgreementRepository authTermsAgreementRepository;

    public void processAgreement(Auth auth, List<Long> requiredIds, List<Long> optionalIds) {
        List<Long> requiredTermIds = termsRepository.findAllByRequiredTrue().stream()
                .map(Terms::getId)
                .toList();

        if (!new HashSet<>(requiredIds).containsAll(requiredTermIds)) {
            throw new CustomException(ErrorCode.AUTH_REQUIRED_TERMS_NOT_AGREED);
        }

        Set<Long> allAgreedIds = new HashSet<>(requiredIds);
        if (optionalIds != null) allAgreedIds.addAll(optionalIds);

        List<Terms> agreedTerms = termsRepository.findAllById(allAgreedIds);
        List<AuthTermsAgreement> agreements = agreedTerms.stream()
                .map(term -> AuthTermsAgreement.builder()
                        .auth(auth)
                        .terms(term)
                        .version(term.getVersion())
                        .build())
                .toList();

        authTermsAgreementRepository.saveAll(agreements);
    }
}

