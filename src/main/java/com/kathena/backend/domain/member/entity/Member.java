package com.kathena.backend.domain.member.entity;

import com.kathena.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE users SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "login_id", unique = true, length = 50)
    private String loginId;

    @Column(name = "password_hash")
    private String password;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "student_id", length = 20)
    private String studentId;

    @Column(length = 100)
    private String department;

    private Integer grade;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(nullable = false, length = 50)
    private String nickname;

    @Column(name = "is_military", nullable = false)
    private boolean isMilitary;

    @Enumerated(EnumType.STRING)
    @Column(name = "enrollment_status")
    private EnrollmentStatus enrollmentStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    @Column(name = "current_points", nullable = false)
    private int currentPoints;

    @Column(name = "profile_image_url", length = 512)
    private String profileImageUrl;

    @Builder
    public Member(String loginId, String password, String name, String studentId, String department, Integer grade, String phoneNumber, String nickname, boolean isMilitary, EnrollmentStatus enrollmentStatus, Role role, UserStatus status, int currentPoints, String profileImageUrl) {
        this.loginId = loginId;
        this.password = password;
        this.name = name;
        this.studentId = studentId;
        this.department = department;
        this.grade = grade;
        this.phoneNumber = phoneNumber;
        this.nickname = nickname;
        this.isMilitary = isMilitary;
        this.enrollmentStatus = enrollmentStatus;
        this.role = role;
        this.status = status;
        this.currentPoints = currentPoints;
        this.profileImageUrl = profileImageUrl;
    }
}
