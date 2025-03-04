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
    name = "payment_request_detail_toss_payments",
    catalog = "railly_linker_company"
)
@Comment("결제 요청 상세(결제 테이블 결제 타입이 Toss Payments 일 때)")
class Db1_RaillyLinkerCompany_PaymentRequestDetailTossPayments(
    @ManyToOne
    @JoinColumn(name = "payment_request_uid", nullable = false)
    @Comment("결제 요청 정보 고유번호(railly_linker_company.payment_request.uid)")
    var paymentRequest: Db1_RaillyLinkerCompany_PaymentRequest,

    @Column(name = "toss_payment_key", nullable = false, columnDefinition = "VARCHAR(200)")
    @Comment("결제의 키값입니다. 최대 길이는 200자입니다. 결제를 식별하는 역할로, 중복되지 않는 고유한 값입니다.")
    var tossPaymentKey: String,

    @Column(name = "toss_order_id", nullable = false, columnDefinition = "VARCHAR(64)")
    @Comment("주문번호입니다. 결제 요청에서 내 상점이 직접 생성한 영문 대소문자, 숫자, 특수문자 -, _로 이루어진 6자 이상 64자 이하의 문자열입니다. 각 주문을 식별하는 역할입니다.")
    var tossOrderId: String,

    @Column(name = "toss_method", nullable = false, columnDefinition = "VARCHAR(45)")
    @Comment("결제수단입니다. 카드, 가상계좌, 간편결제, 휴대폰, 계좌이체, 문화상품권, 도서문화상품권, 게임문화상품권 중 하나입니다.")
    var tossMethod: String
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