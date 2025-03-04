package com.raillylinker.jpa_beans.db1_main.entities

import jakarta.persistence.*
import org.hibernate.annotations.ColumnDefault
import org.hibernate.annotations.Comment
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@Entity
@Table(
    name = "total_auth_login_token_history",
    catalog = "railly_linker_company",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["token_type", "access_token", "row_delete_date_str"])
    ]
)
@Comment("통합 로그인 계정 인증 토큰 발행 히스토리 테이블")
class Db1_RaillyLinkerCompany_TotalAuthLogInTokenHistory(
    @ManyToOne
    @JoinColumn(name = "total_auth_member_uid", nullable = false)
    @Comment("멤버 고유번호(railly_linker_company.total_auth_member.uid)")
    var totalAuthMember: Db1_RaillyLinkerCompany_TotalAuthMember,

    @Column(name = "token_type", nullable = false, columnDefinition = "VARCHAR(50)")
    @Comment("토큰 타입 (ex : Bearer)")
    var tokenType: String,

    @Column(name = "login_date", nullable = false, columnDefinition = "DATETIME(3)")
    @Comment("로그인 시간")
    var loginDate: LocalDateTime,

    @Column(name = "access_token", nullable = false, columnDefinition = "VARCHAR(500)")
    @Comment("발행된 액세스 토큰")
    var accessToken: String,

    @Column(name = "access_token_expire_when", nullable = false, columnDefinition = "DATETIME(3)")
    @Comment("액세스 토큰 만료 일시")
    var accessTokenExpireWhen: LocalDateTime,

    @Column(name = "refresh_token", nullable = false, columnDefinition = "VARCHAR(500)")
    @Comment("발행된 리플레시 토큰")
    var refreshToken: String,

    @Column(name = "refresh_token_expire_when", nullable = false, columnDefinition = "DATETIME(3)")
    @Comment("리플레시 토큰 만료 일시")
    var refreshTokenExpireWhen: LocalDateTime,

    @Column(name = "logout_date", nullable = true, columnDefinition = "DATETIME(3)")
    @Comment("로그아웃 일시")
    var logoutDate: LocalDateTime?
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "uid", columnDefinition = "BIGINT")
    @Comment("행 고유값")
    var uid: Long? = null

    @Column(name = "row_create_date", nullable = false, columnDefinition = "DATETIME(3)")
    @CreationTimestamp
    @Comment("행 생성일")
    var rowCreateDate: LocalDateTime? = null

    @Column(name = "row_update_date", nullable = false, columnDefinition = "DATETIME(3)")
    @UpdateTimestamp
    @Comment("행 수정일")
    var rowUpdateDate: LocalDateTime? = null

    @Column(name = "row_delete_date_str", nullable = false, columnDefinition = "VARCHAR(50)")
    @ColumnDefault("'/'")
    @Comment("행 삭제일(yyyy_MM_dd_T_HH_mm_ss_SSS_z, 삭제되지 않았다면 /)")
    var rowDeleteDateStr: String = "/"


    // ---------------------------------------------------------------------------------------------
    // <중첩 클래스 공간>

}