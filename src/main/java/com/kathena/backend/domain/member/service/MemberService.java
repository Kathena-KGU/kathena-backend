package com.kathena.backend.domain.member.service;

import com.kathena.backend.domain.member.dto.LoginRequest;
import com.kathena.backend.domain.member.dto.MemberResponse;
import com.kathena.backend.domain.member.dto.SignUpRequest;
import com.kathena.backend.domain.member.dto.TokenDto;
import com.kathena.backend.domain.member.entity.Member;
import com.kathena.backend.domain.member.entity.UserStatus;
import com.kathena.backend.domain.member.repository.MemberRepository;
import com.kathena.backend.global.error.CustomException;
import com.kathena.backend.global.error.ErrorCode;
import com.kathena.backend.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 회원가입
     */
    @Transactional
    public MemberResponse signUp(SignUpRequest request) {
        // 중복 검사
        if (memberRepository.existsByLoginId(request.getLoginId())) {
            throw new CustomException(ErrorCode.DUPLICATE_LOGIN_ID);
        }
        if (memberRepository.existsByNickname(request.getNickname())) {
            throw new CustomException(ErrorCode.DUPLICATE_NICKNAME);
        }
        if (request.getStudentId() != null && memberRepository.existsByStudentId(request.getStudentId())) {
            throw new CustomException(ErrorCode.DUPLICATE_STUDENT_ID);
        }

        // 비밀번호 암호화 및 저장
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        Member member = request.toEntity(encodedPassword);

        Member savedMember = memberRepository.save(member);

        return MemberResponse.from(savedMember);
    }
    /**
     * 로그인
     */
    @Transactional
    public TokenDto login(LoginRequest request) {
        // 1. ID 존재 여부 확인
        Member member = memberRepository.findByLoginId(request.getLoginId())
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        // 2. 비밀번호 일치 여부 확인
        if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        // 3. 계정 상태 확인
        if (member.getStatus() == UserStatus.PENDING) {
            throw new CustomException(ErrorCode.ACCOUNT_PENDING);
        }
        if (member.getStatus() == UserStatus.REJECTED || member.getStatus() == UserStatus.SUSPENDED) {
            throw new CustomException(ErrorCode.ACCOUNT_INACTIVE);
        }

        // 4. 인증 객체 생성 (직접 생성)
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                member.getLoginId(),
                null,
                Collections.singleton(new SimpleGrantedAuthority("ROLE_" + member.getRole().name()))
        );

        // 5. 토큰 발급
        TokenDto tokenDto = jwtTokenProvider.generateTokenDto(authentication);

        // 6. Refresh Token Redis에 저장
        redisTemplate.opsForValue().set(
                "RT:" + member.getLoginId(),
                tokenDto.getRefreshToken(),
                tokenDto.getRefreshTokenExpiresIn(), // 7일
                TimeUnit.MILLISECONDS
        );

        return tokenDto;
    }

    /**
     * 토큰 재발급
     */
    @Transactional
    public TokenDto reissue(String refreshToken) {
        // 1. Refresh Token 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 2. 토큰에서 유저 정보 가져오기
        Authentication authentication = jwtTokenProvider.getAuthentication(refreshToken);

        // 3. Redis에서 저장된 Refresh Token 가져오기
        String redisKey = "RT:" + authentication.getName();
        String redisRefreshToken = redisTemplate.opsForValue().get(redisKey);

        // 4. Redis에 토큰이 없거나, 일치하지 않는 경우 에러 처리 (로그아웃됨)
        if (redisRefreshToken == null || !redisRefreshToken.equals(refreshToken)) {
            redisTemplate.delete(redisKey); // 서버에 저장된 토큰이 있다면 지움
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN); // 여기서 예외를 던지면 -> Controller가 쿠키 삭제
        }

        // 5. 새 토큰 생성
        TokenDto tokenDto = jwtTokenProvider.generateTokenDto(authentication);

        // 6. 새 Refresh Token 저장
        redisTemplate.opsForValue().set(
                "RT:" + authentication.getName(),
                tokenDto.getRefreshToken(),
                tokenDto.getRefreshTokenExpiresIn(),
                TimeUnit.MILLISECONDS
        );

        return tokenDto;
    }

    // 로그아웃
    @Transactional
    public void logout(String loginId) {
        String redisKey = "RT:" + loginId;
        if (redisTemplate.opsForValue().get(redisKey) != null) {
            redisTemplate.delete(redisKey);
        }
    }

}
