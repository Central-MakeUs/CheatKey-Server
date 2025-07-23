package com.cheatkey.common.code.domain.service;

import com.cheatkey.common.code.domain.entity.CodeType;
import com.cheatkey.common.code.domain.repository.CodeRepository;
import com.cheatkey.common.code.interfaces.dto.OptionsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import static com.cheatkey.common.code.interfaces.dto.OptionsResponse.*;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CodeService {

    private final CodeRepository codeRepository;

    public List<Option> getOptionsByType(CodeType type) {
        return OptionsResponse.from(codeRepository.findAllByType(type));
    }
}
