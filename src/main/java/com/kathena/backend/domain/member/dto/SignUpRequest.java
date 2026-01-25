package com.kathena.backend.domain.member.dto;

import com.kathena.backend.domain.member.entity.EnrollmentStatus;
import com.kathena.backend.domain.member.entity.Member;
import com.kathena.backend.domain.member.entity.Role;
import com.kathena.backend.domain.member.entity.UserStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SignUpRequest {

    @NotBlank(message = "아이디는 필수입니다.")
    private String loginId;

    @NotBlank(message = "비밀번호는 필수입니다.")
    private String password;

    @NotBlank(message = "이름은 필수입니다.")
    private String name;

    private String studentId;

    private String department;

    private Integer grade;

    @NotBlank(message = "전화번호는 필수입니다.")
    @Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$", message = "전화번호 형식이 올바르지 않습니다.")
    private String phoneNumber;

    @NotBlank(message = "닉네임은 필수입니다.")
    private String nickname;

    @NotNull(message = "군필 여부는 필수입니다.")
    private Boolean isMilitary;

    private EnrollmentStatus enrollmentStatus;

    public Member toEntity(String encodedPassword) {
        return Member.builder()
                .loginId(loginId)
                .password(encodedPassword)
                .name(name)
                .studentId(studentId)
                .department(department)
                .grade(grade)
                .phoneNumber(phoneNumber)
                .nickname(nickname)
                .isMilitary(isMilitary)
                .enrollmentStatus(enrollmentStatus)
                .role(Role.USER) // 기본값 USER
                .status(UserStatus.PENDING) // 기본값 PENDING
                .currentPoints(0)
                .build();
    }
}
