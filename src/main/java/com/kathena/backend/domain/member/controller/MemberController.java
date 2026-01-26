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
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

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

        // 1. Refresh Token을 HttpOnly Cookie로 설정
        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", tokenDto.getRefreshToken())
                .httpOnly(true)
                .secure(true) // HTTPS 환경에서만 전송 (로컬 개발 시 필요하면 false로 변경)
                .path("/")
                .maxAge(7 * 24 * 60 * 60) // 7일 (Token 만료시간과 동일하게 설정)
                .sameSite("None")
                .build();

        response.addHeader("Set-Cookie", refreshCookie.toString());

        // 2. Body에는 Access Token만 내려주기 (보안 강화: Refresh Token 제거)
        TokenDto responseDto = TokenDto.builder()
                .grantType(tokenDto.getGrantType())
                .accessToken(tokenDto.getAccessToken())
                .accessTokenExpiresIn(tokenDto.getAccessTokenExpiresIn())
                .refreshToken(null) // ★ 핵심: 응답 바디에서 제외 (@JsonInclude.NON_NULL 덕분에 필드 자체가 사라짐)
                .refreshTokenExpiresIn(tokenDto.getRefreshTokenExpiresIn())
                .build();

        return ApiResponse.success("로그인에 성공했습니다.", responseDto);
    }

    //토큰 재발급
    @PostMapping("/reissue")
    public ApiResponse<TokenDto> reissue(@CookieValue(name = "refresh_token", required = false) String refreshToken,
                                         HttpServletResponse response) {
        // 쿠키가 없는 경우
        if (refreshToken == null) {
            expireCookie(response, "refresh_token"); // 쿠키 삭제
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        try {
            TokenDto tokenDto = memberService.reissue(refreshToken);

            // 성공: 새 쿠키 발급
            ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", tokenDto.getRefreshToken())
                    .httpOnly(true)
                    .secure(true)
                    .path("/")
                    .maxAge(7 * 24 * 60 * 60) // 7일
                    .sameSite("None")
                    .build();
            response.addHeader("Set-Cookie", refreshCookie.toString());

            // 성공: Body 보호 (Access Token만 반환)
            TokenDto responseDto = TokenDto.builder()
                    .grantType(tokenDto.getGrantType())
                    .accessToken(tokenDto.getAccessToken())
                    .accessTokenExpiresIn(tokenDto.getAccessTokenExpiresIn())
                    .refreshToken(null) // ★ 핵심
                    .refreshTokenExpiresIn(tokenDto.getRefreshTokenExpiresIn())
                    .build();

            return ApiResponse.success("토큰 재발급이 완료되었습니다.", responseDto);

        } catch (CustomException e) {
            // 실패: 쿠키 삭제 (잘못된 토큰 등)
            expireCookie(response, "refresh_token");
            throw e;
        }
    }

    // 로그아웃 (신규 추가)
    @PostMapping("/logout")
    public ApiResponse<Void> logout(Authentication authentication, HttpServletResponse response) {
        // 1. Redis에서 Refresh Token 삭제
        // (로그인 상태가 아닐 수도 있으므로 authentication null 체크)
        if (authentication != null && authentication.getName() != null) {
            memberService.logout(authentication.getName());
        }

        // 2. 쿠키 삭제
        expireCookie(response, "refresh_token");

        return ApiResponse.success("로그아웃 되었습니다.", null);
    }

    // 쿠키 삭제 헬퍼 메서드
    private void expireCookie(HttpServletResponse response, String cookieName) {
        ResponseCookie cookie = ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0) // 수명 0으로
                .sameSite("None")
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }
}
