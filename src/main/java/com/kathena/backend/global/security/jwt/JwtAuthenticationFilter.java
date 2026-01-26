package com.kathena.backend.global.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kathena.backend.global.common.ApiResponse;
import com.kathena.backend.global.error.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = resolveToken(request);

        if (StringUtils.hasText(token)) {
            // 큰 유효성
            if (!jwtTokenProvider.validateToken(token)) {
                // 유효하지 않으면 그냥 인증 없이 진행 -> EntryPoint가 처리
                filterChain.doFilter(request, response);
                return;
            }

            // ACCESS만 인증으로 인정
            if (!jwtTokenProvider.isAccessToken(token)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");

                ApiResponse<Object> apiResponse = ApiResponse.onFailure(
                        ErrorCode.INVALID_TOKEN.getCode(),
                        "Refresh Token으로는 접근할 수 없습니다.",
                        null
                );
                response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
                return;
            }

            Authentication authentication = jwtTokenProvider.getAuthentication(token);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
