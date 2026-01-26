package com.kathena.backend.domain.member.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TokenDto {
    private String grantType;   // Bearer
    private String accessToken;
    private String refreshToken;
    private Long accessTokenExpiresIn; // accessToken 만료 시간
    private Long refreshTokenExpiresIn; // 리프레시 토큰 만료 시간
}