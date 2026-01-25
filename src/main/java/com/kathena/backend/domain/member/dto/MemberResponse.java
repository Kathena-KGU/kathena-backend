package com.kathena.backend.domain.member.dto;

import com.kathena.backend.domain.member.entity.EnrollmentStatus;
import com.kathena.backend.domain.member.entity.Member;
import com.kathena.backend.domain.member.entity.Role;
import com.kathena.backend.domain.member.entity.UserStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MemberResponse {

    private Long id;
    private String loginId;
    private String name;
    private String studentId;
    private String department;
    private Integer grade;
    private String phoneNumber;
    private String nickname;
    private boolean isMilitary;
    private EnrollmentStatus enrollmentStatus;
    private Role role;
    private UserStatus status;
    private int currentPoints;

    public static MemberResponse from(Member member) {
        return MemberResponse.builder()
                .id(member.getId())
                .loginId(member.getLoginId())
                .name(member.getName())
                .studentId(member.getStudentId())
                .department(member.getDepartment())
                .grade(member.getGrade())
                .phoneNumber(member.getPhoneNumber())
                .nickname(member.getNickname())
                .isMilitary(member.isMilitary())
                .enrollmentStatus(member.getEnrollmentStatus())
                .role(member.getRole())
                .status(member.getStatus())
                .currentPoints(member.getCurrentPoints())
                .build();
    }
}
