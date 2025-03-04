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
    name = "payment_request_detail_bank_transfer",
    catalog = "railly_linker_company"
)
@Comment("결제 요청 상세(결제 테이블 결제 타입이 수동 계좌이체 일 때)")
class Db1_RaillyLinkerCompany_PaymentRequestDetailBankTransfer(
    @ManyToOne
    @JoinColumn(name = "payment_request_uid", nullable = false)
    @Comment("결제 요청 정보 고유번호(railly_linker_company.payment_request.uid)")
    var paymentRequest: Db1_RaillyLinkerCompany_PaymentRequest,

    @Column(name = "receive_bank_name", nullable = false, columnDefinition = "VARCHAR(60)")
    @Comment("입금 받을 은행명")
    var receiveBankName: String,

    @Column(name = "receive_bank_account", nullable = false, columnDefinition = "VARCHAR(60)")
    @Comment("입금 받을 은행 계좌번호")
    var receiveBankAccount: String,

    @Column(name = "depositor_name", nullable = true, columnDefinition = "VARCHAR(60)")
    @Comment("입금자 이름")
    var depositorName: String?
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