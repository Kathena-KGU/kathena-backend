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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CookieValue;

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
    @PostMapping("/login")
    public ApiResponse<TokenDto> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        TokenDto tokenDto = memberService.login(request);

        // Refresh Token을 HttpOnly Cookie로 설정
        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", tokenDto.getRefreshToken())
                .httpOnly(true)
                .secure(true) // HTTPS 환경에서만 전송
                .path("/")
                .maxAge(7 * 24 * 60 * 60) // 7일
                .sameSite("None")
                .build();

        response.addHeader("Set-Cookie", refreshCookie.toString());

        // Body에는 Refresh Token 같이 보냄
        return ApiResponse.success("로그인에 성공했습니다.", tokenDto);
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

            return ApiResponse.success("토큰 재발급이 완료되었습니다.", tokenDto);

        } catch (CustomException e) {
            // 실패: 쿠키 삭제
            expireCookie(response, "refresh_token");
            throw e; // 에러시 (401, 재발급 실패)
        }
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
