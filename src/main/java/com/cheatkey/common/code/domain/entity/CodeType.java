package com.cheatkey.common.code.domain.entity;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum CodeType {
    AGE_GROUP,      // 연령대 옵션
    GENDER,         // 성별 옵션
    TRADE_METHOD,   // 거래 방식 옵션
    TRADE_ITEM,     // 거래 품목 옵션
    REPORT          // 커뮤니티 신고하기 옵션
}
