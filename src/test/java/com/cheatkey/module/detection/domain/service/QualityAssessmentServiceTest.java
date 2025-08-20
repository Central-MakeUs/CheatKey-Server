package com.cheatkey.module.detection.domain.service;

import com.cheatkey.module.detection.domain.service.validation.QualityAssessmentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("QualityAssessmentService 테스트")
class QualityAssessmentServiceTest {

    @InjectMocks
    private QualityAssessmentService qualityAssessmentService;

    @Test
    @DisplayName("의미 없는 입력 체크 - 단일 문자")
    void 의미없는_입력_체크_단일문자() {
        // given
        String input = "똥";
        
        // when
        boolean result = qualityAssessmentService.isMeaninglessInput(input);
        
        // then
        assertThat(result).isTrue();
    }
    
    @Test
    @DisplayName("의미 없는 입력 체크 - 한글 자음/모음만")
    void 의미없는_입력_체크_한글자음모음() {
        // given
        String input = "ㄱㄴㄷ";
        
        // when
        boolean result = qualityAssessmentService.isMeaninglessInput(input);
        
        // then
        assertThat(result).isTrue();
    }
    
    @Test
    @DisplayName("의미 없는 입력 체크 - 반복 패턴")
    void 의미없는_입력_체크_반복패턴() {
        // given
        String input = "똥똥똥";
        
        // when
        boolean result = qualityAssessmentService.isMeaninglessInput(input);
        
        // then
        assertThat(result).isTrue();
    }
    
    @Test
    @DisplayName("의미 없는 입력 체크 - 인사말")
    void 의미없는_입력_체크_인사말() {
        // given
        String input = "안녕하세요";
        
        // when
        boolean result = qualityAssessmentService.isMeaninglessInput(input);
        
        // then
        assertThat(result).isTrue();
    }
    
    @Test
    @DisplayName("의미 없는 입력 체크 - 피싱 키워드가 있으면 허용")
    void 의미없는_입력_체크_피싱키워드_허용() {
        // given
        String input = "사기";
        
        // when
        boolean result = qualityAssessmentService.isMeaninglessInput(input);
        
        // then
        assertThat(result).isFalse();
    }
    
    @Test
    @DisplayName("의미 없는 입력 체크 - 피싱 키워드가 있으면 허용 (짧은 입력)")
    void 의미없는_입력_체크_피싱키워드_짧은입력_허용() {
        // given
        String input = "피싱";
        
        // when
        boolean result = qualityAssessmentService.isMeaninglessInput(input);
        
        // then
        assertThat(result).isFalse();
    }
    
    @Test
    @DisplayName("의미 없는 입력 체크 - 의미 있는 입력")
    void 의미없는_입력_체크_의미있는_입력() {
        // given
        String input = "오픈채팅에서 부업을 소개받았는데 의심스러워요";
        
        // when
        boolean result = qualityAssessmentService.isMeaninglessInput(input);
        
        // then
        assertThat(result).isFalse();
    }
    
    @Test
    @DisplayName("의미 없는 입력 체크 - 너무 짧은 입력")
    void 의미없는_입력_체크_너무짧은_입력() {
        // given
        String input = "ㅋ";
        
        // when
        boolean result = qualityAssessmentService.isMeaninglessInput(input);
        
        // then
        assertThat(result).isTrue();
    }
    
    @Test
    @DisplayName("의미 없는 입력 체크 - 너무 긴 입력")
    void 의미없는_입력_체크_너무긴_입력() {
        // given
        StringBuilder longInput = new StringBuilder();
        for (int i = 0; i < 201; i++) {
            longInput.append("a");
        }
        
        // when
        boolean result = qualityAssessmentService.isMeaninglessInput(longInput.toString());
        
        // then
        assertThat(result).isTrue();
    }
    
    @Test
    @DisplayName("의미 없는 입력 체크 - 특수문자만")
    void 의미없는_입력_체크_특수문자만() {
        // given
        String input = "!@#$%^&*()";
        
        // when
        boolean result = qualityAssessmentService.isMeaninglessInput(input);
        
        // then
        assertThat(result).isTrue();
    }
    
    @Test
    @DisplayName("의미 없는 입력 체크 - 2자 미만")
    void 의미없는_입력_체크_2자미만() {
        // given
        String input = "a";
        
        // when
        boolean result = qualityAssessmentService.isMeaninglessInput(input);
        
        // then
        assertThat(result).isTrue();
    }
    
    @Test
    @DisplayName("의미 없는 입력 체크 - 5자 미만 (피싱 키워드 없음)")
    void 의미없는_입력_체크_5자미만_피싱키워드없음() {
        // given
        String input = "안녕";
        
        // when
        boolean result = qualityAssessmentService.isMeaninglessInput(input);
        
        // then
        assertThat(result).isTrue();
    }
    
    @Test
    @DisplayName("의미 없는 입력 체크 - 5자 이상 (피싱 키워드 없음)")
    void 의미없는_입력_체크_5자이상_피싱키워드없음() {
        // given
        String input = "안녕하세요 반갑습니다";
        
        // when
        boolean result = qualityAssessmentService.isMeaninglessInput(input);
        
        // then
        assertThat(result).isTrue();
    }
}



