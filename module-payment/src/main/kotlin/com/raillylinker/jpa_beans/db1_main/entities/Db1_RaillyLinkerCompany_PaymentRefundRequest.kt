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
    name = "payment_refund_request",
    catalog = "railly_linker_company"
)
@Comment("결제 환불 요청 정보(결제 정보가 완료된 이후, 취소/부분 취소의 개념)")
class Db1_RaillyLinkerCompany_PaymentRefundRequest(
    @ManyToOne
    @JoinColumn(name = "payment_request_uid", nullable = false)
    @Comment("결제 요청 정보 고유번호(railly_linker_company.payment_request.uid)")
    var paymentRequest: Db1_RaillyLinkerCompany_PaymentRequest,

    @Column(name = "refund_amount", nullable = true, columnDefinition = "DECIMAL(15, 2)")
    @Comment("환불 금액(통화 코드는 결제 정보 테이블과 동일합니다. null 이라면 전액 환불입니다.)")
    var refundAmount: BigDecimal?,

    @Column(name = "refund_reason", nullable = false, columnDefinition = "VARCHAR(300)")
    @Comment("환불 요청 이유")
    var refundReason: String,

    @Column(name = "refund_fail_reason", nullable = true, columnDefinition = "VARCHAR(300)")
    @Comment("환불 실패 이유(환불 실패라면 Not Null)")
    var refundFailReason: String?,

    @Column(name = "refund_end_datetime", nullable = true, columnDefinition = "DATETIME(3)")
    @Comment("환불 프로세스 종결일시(refund_fail_reason  이 null 이라면 완료일, not null 이라면 실패일)")
    var refundEndDatetime: LocalDateTime?,

    @Column(name = "refund_bank_name", nullable = true, columnDefinition = "VARCHAR(60)")
    @Comment("환불 은행명")
    var refundBankName: String?,

    @Column(name = "refund_bank_account", nullable = true, columnDefinition = "VARCHAR(60)")
    @Comment("환불 은행 계좌번호")
    var refundBankAccount: String?,

    @Column(name = "refund_holder_name", nullable = true, columnDefinition = "VARCHAR(60)")
    @Comment("환불 받을 대상의 이름")
    var refundHolderName: String?
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


    // ---------------------------------------------------------------------------------------------
    // <중첩 클래스 공간>

}