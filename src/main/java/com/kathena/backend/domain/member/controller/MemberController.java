package com.kathena.backend.domain.member.controller;

import com.kathena.backend.domain.member.dto.LoginRequest;
import com.kathena.backend.domain.member.dto.MemberResponse;
import com.kathena.backend.domain.member.dto.SignUpRequest;
import com.kathena.backend.domain.member.dto.TokenDto;
import com.kathena.backend.domain.member.service.MemberService;
import com.kathena.backend.global.common.ApiResponse;
import com.kathena.backend.global.error.CustomException;
import com.kathena.backend.global.error.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @Value("${jwt.cookie-secure:false}")
    private boolean cookieSecure;

    @Value("${jwt.cookie-same-site:Lax}")
    private String cookieSameSite;


    //회원가입
    @PostMapping("/signup")
    public ApiResponse<MemberResponse> signUp(@Valid @RequestBody SignUpRequest request) {
        MemberResponse response = memberService.signUp(request);
        return ApiResponse.success("회원가입 신청이 완료되었습니다.", response);
    }

    //로그인
    // 로그인
    @PostMapping("/login")
    public ApiResponse<TokenDto> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        TokenDto tokenDto = memberService.login(request);

        // ★ 쿠키 설정 통합 메서드 사용
        setRefreshCookie(response, tokenDto.getRefreshToken(), 7 * 24 * 60 * 60);

        return ApiResponse.success("로그인에 성공했습니다.", convertToAccessOnlyDto(tokenDto));
    }

    //토큰 재발급
    @PostMapping("/reissue")
    public ApiResponse<TokenDto> reissue(@CookieValue(name = "refresh_token", required = false) String refreshToken,
                                         HttpServletResponse response) {
        if (refreshToken == null) {
            expireCookie(response); // ★ 여기도 설정 적용된 삭제 쿠키 사용
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        try {
            TokenDto tokenDto = memberService.reissue(refreshToken);
            setRefreshCookie(response, tokenDto.getRefreshToken(), 7 * 24 * 60 * 60); // ★

            return ApiResponse.success("토큰 재발급이 완료되었습니다.", convertToAccessOnlyDto(tokenDto));
        } catch (CustomException e) {
            expireCookie(response); // ★
            throw e;
        }
    }

    // 로그아웃 (신규 추가)
    @PostMapping("/logout")
    public ApiResponse<Void> logout(Authentication authentication, HttpServletResponse response) {
        if (authentication != null && authentication.getName() != null) {
            memberService.logout(authentication.getName());
        }
        expireCookie(response); // ★
        return ApiResponse.success("로그아웃 되었습니다.", null);
    }

    // --- Helper Methods (중복 제거 & 설정 통일) ---

    // 1. 쿠키 생성/설정 공통화
    private void setRefreshCookie(HttpServletResponse response, String token, long maxAge) {
        ResponseCookie cookie = ResponseCookie.from("refresh_token", token)
                .httpOnly(true)
                .secure(cookieSecure)      // application.yml 설정
                .path("/api/members")
                .maxAge(maxAge)
                .sameSite(cookieSameSite)  // application.yml 설정
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    // 2. 쿠키 삭제 공통화
    private void expireCookie(HttpServletResponse response) {
        setRefreshCookie(response, "", 0); // 내용 비우고 수명 0
    }

    // 3. DTO 변환 (Body에서 RefreshToken 제거)
    private TokenDto convertToAccessOnlyDto(TokenDto original) {
        return TokenDto.builder()
                .grantType(original.getGrantType())
                .accessToken(original.getAccessToken())
                .accessTokenExpiresIn(original.getAccessTokenExpiresIn())
                .refreshToken(null) // Body 제거
                .refreshTokenExpiresIn(original.getRefreshTokenExpiresIn())
                .build();
    }

}
