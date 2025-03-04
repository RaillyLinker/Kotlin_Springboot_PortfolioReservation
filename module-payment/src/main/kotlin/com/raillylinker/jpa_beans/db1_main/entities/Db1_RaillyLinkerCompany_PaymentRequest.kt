package com.raillylinker.jpa_beans.db1_main.entities

import jakarta.persistence.*
import org.hibernate.annotations.ColumnDefault
import org.hibernate.annotations.Comment
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(
    name = "payment_request",
    catalog = "railly_linker_company",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["payment_code", "row_delete_date_str"])
    ]
)
@Comment("결제 요청 정보 테이블")
class Db1_RaillyLinkerCompany_PaymentRequest(
    @ManyToOne
    @JoinColumn(name = "member_uid", nullable = true)
    @Comment("멤버 고유번호(railly_linker_company.total_auth_member.uid)")
    var totalAuthMember: Db1_RaillyLinkerCompany_TotalAuthMember?,

    @Column(name = "payment_detail_type", nullable = false, columnDefinition = "TINYINT UNSIGNED")
    @Comment("결제 타입. 결제 상세 테이블의 종류를 의미합니다. (1 : 수동 계좌이체, 2 : 토스 페이)")
    var paymentDetailType: Short,

    @Column(name = "payment_amount", nullable = false, columnDefinition = "DECIMAL(15, 2)")
    @Comment("결제 금액")
    var paymentAmount: BigDecimal,

    @Column(name = "payment_currency_code", nullable = false, columnDefinition = "CHAR(3)")
    @Comment("결제 금액 통화 코드(IOS 4217, ex : KRW, USD, EUR...)")
    var paymentCurrencyCode: String,

    @Column(name = "payment_reason", nullable = false, columnDefinition = "VARCHAR(300)")
    @Comment("결제 이유")
    var paymentReason: String,

    @Column(name = "payment_fail_reason", nullable = true, columnDefinition = "VARCHAR(300)")
    @Comment("결제 실패 이유(결제 실패라면 Not Null)")
    var paymentFailReason: String?,

    @Column(name = "payment_end_datetime", nullable = true, columnDefinition = "DATETIME(3)")
    @Comment("결제 프로세스 종결일시(payment_fail_reason 이 null 이라면 완료일, not null 이라면 실패일)")
    var paymentEndDatetime: LocalDateTime?
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
    @OneToMany(mappedBy = "paymentRequest", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    var paymentRefundRequestList: MutableList<Db1_RaillyLinkerCompany_PaymentRefundRequest> = mutableListOf()

    // Row 삭제시 삭제 처리
    @OneToMany(mappedBy = "paymentRequest", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    var paymentRequestDetailBankTransferList: MutableList<Db1_RaillyLinkerCompany_PaymentRequestDetailBankTransfer> =
        mutableListOf()

    // Row 삭제시 삭제 처리
    @OneToMany(mappedBy = "paymentRequest", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    var paymentRequestDetailTossPaymentsList: MutableList<Db1_RaillyLinkerCompany_PaymentRequestDetailTossPayments> =
        mutableListOf()


    // ---------------------------------------------------------------------------------------------
    // <중첩 클래스 공간>

}