package com.raillylinker.jpa_beans.db1_main.entities

import jakarta.persistence.*
import org.hibernate.annotations.ColumnDefault
import org.hibernate.annotations.Comment
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@Entity
@Table(
    name = "total_auth_member",
    catalog = "railly_linker_company",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["account_id", "row_delete_date_str"])
    ]
)
@Comment("통합 로그인 계정 회원 정보 테이블")
class Db1_RaillyLinkerCompany_TotalAuthMember(
    @Column(name = "account_id", nullable = false, columnDefinition = "VARCHAR(100)")
    @Comment("계정 아이디")
    var accountId: String,

    @Column(name = "account_password", nullable = true, columnDefinition = "VARCHAR(100)")
    @Comment("계정 로그인시 사용하는 비밀번호 (계정 아이디, 이메일, 전화번호 로그인에 모두 사용됨. OAuth2 만 등록했다면 null)")
    var accountPassword: String?
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
    // [@OneToMany 변수들]
    // Row 삭제시 삭제 처리
    @OneToMany(mappedBy = "totalAuthMember", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    var totalAuthMemberRoleList: MutableList<Db1_RaillyLinkerCompany_TotalAuthMemberRole> = mutableListOf()

    // Row 삭제시 삭제 처리
    @OneToMany(mappedBy = "totalAuthMember", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    var totalAuthMemberProfileList: MutableList<Db1_RaillyLinkerCompany_TotalAuthMemberProfile> =
        mutableListOf()

    // Row 삭제시 삭제 처리
    @OneToMany(mappedBy = "totalAuthMember", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    var totalAuthMemberPhoneList: MutableList<Db1_RaillyLinkerCompany_TotalAuthMemberPhone> = mutableListOf()

    // Row 삭제시 삭제 처리
    @OneToMany(mappedBy = "totalAuthMember", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    var totalAuthMemberEmailList: MutableList<Db1_RaillyLinkerCompany_TotalAuthMemberEmail> = mutableListOf()


    // ---------------------------------------------------------------------------------------------
    // <중첩 클래스 공간>

}