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
    name = "rental_product_reservation_image",
    catalog = "railly_linker_company"
)
@Comment("상품 예약 결제 정보 테이블(본 테이블이 존재한다는 것으로 해당 결제 정보에 결제 완료가 되었다는 것입니다. 가장 최신의 데이터를 인정합니다.)")
class Db1_RaillyLinkerCompany_RentalProductReservationPayment(
    @ManyToOne
    @JoinColumn(name = "rental_product_reservation_uid", nullable = false)
    @Comment("상품 예약 정보 행 고유키")
    var rentalProductReservation: Db1_RaillyLinkerCompany_RentalProductReservation,

    @ManyToOne
    @JoinColumn(name = "payment_uid", nullable = true)
    @Comment("결제 모듈 테이블 행 고유키")
    var paymentRequest: Db1_RaillyLinkerCompany_PaymentRequest?
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