package com.cheatkey.module.terms.domain.service;

import com.cheatkey.module.terms.domain.entity.Terms;
import com.cheatkey.module.terms.domain.repository.TermsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TermsService {

    private final TermsRepository termsRepository;

    public List<Terms> getTermsForRegistration() {
        return termsRepository.findAllByOrderByRequiredDesc();
    }
}
