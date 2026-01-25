package com.kathena.backend.domain.member.controller;

import com.kathena.backend.domain.member.dto.MemberResponse;
import com.kathena.backend.domain.member.dto.SignUpRequest;
import com.kathena.backend.domain.member.service.MemberService;
import com.kathena.backend.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @PostMapping("/signup")
    public ApiResponse<MemberResponse> signUp(@Valid @RequestBody SignUpRequest request) {
        MemberResponse response = memberService.signUp(request);
        return ApiResponse.success("회원가입 신청이 완료되었습니다.", response);
    }
}
