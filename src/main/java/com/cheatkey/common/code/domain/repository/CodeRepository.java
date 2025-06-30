package com.cheatkey.common.code.domain.repository;

import com.cheatkey.common.code.domain.entity.Code;
import com.cheatkey.common.code.domain.entity.CodeType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CodeRepository extends JpaRepository<Code, Long> {
    List<Code> findAllByType(CodeType type);
}
