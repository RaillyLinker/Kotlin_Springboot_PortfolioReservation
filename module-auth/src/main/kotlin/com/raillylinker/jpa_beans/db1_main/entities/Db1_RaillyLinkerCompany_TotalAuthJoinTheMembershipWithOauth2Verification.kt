package com.raillylinker.jpa_beans.db1_main.entities

import jakarta.persistence.*
import org.hibernate.annotations.ColumnDefault
import org.hibernate.annotations.Comment
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@Entity
@Table(
    name = "total_auth_join_the_membership_with_oauth2_verification",
    catalog = "railly_linker_company"
)
@Comment("통합 로그인 계정 OAuth2 회원가입 검증 테이블")
class Db1_RaillyLinkerCompany_TotalAuthJoinTheMembershipWithOauth2Verification(
    @Column(name = "oauth2_type_code", nullable = false, columnDefinition = "TINYINT UNSIGNED")
    @Comment("oauth2 종류 (1 : GOOGLE, 2 : NAVER, 3 : KAKAO)")
    var oauth2TypeCode: Short,

    @Column(name = "oauth2_id", nullable = false, columnDefinition = "VARCHAR(50)")
    @Comment("OAuth2 로그인으로 얻어온 고유값")
    var oauth2Id: String,

    @Column(name = "verification_secret", nullable = false, columnDefinition = "VARCHAR(20)")
    @Comment("검증 비문")
    var verificationSecret: String,

    @Column(name = "verification_expire_when", nullable = false, columnDefinition = "DATETIME(3)")
    @Comment("검증 만료 일시")
    var verificationExpireWhen: LocalDateTime
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