package com.raillylinker.jpa_beans.db1_main.entities

import jakarta.persistence.*
import org.hibernate.annotations.ColumnDefault
import org.hibernate.annotations.Comment
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDateTime

// Fk 관계 중 OneToOne 은 논리적 삭제를 적용할시 사용이 불가능합니다.
//     이때는, One to One 역시 Many to One 을 사용하며, 합성 Unique 로 FK 변수를 유니크 처리 한 후,
//     로직상으로 활성화된 행이 한개 뿐이라고 처리하면 됩니다.
@Entity
@Table(
    name = "rental_product",
    catalog = "railly_linker_company"
)
@Comment("예약 상품 정보")
class Db1_RaillyLinkerCompany_RentalProduct(
    @Column(name = "product_name", nullable = false, columnDefinition = "VARCHAR(90)")
    @Comment("상품명")
    var productName: String,

    @Column(name = "product_intro", nullable = false, columnDefinition = "VARCHAR(6000)")
    @Comment("상품 소개")
    var productIntro: String,

    @Column(name = "address_country", nullable = false, columnDefinition = "VARCHAR(60)")
    @Comment("상품이 위치한 주소 (대여 가능 위치의 기준으로 사용됨) - 국가")
    var addressCountry: String,

    @Column(name = "address_main", nullable = false, columnDefinition = "VARCHAR(300)")
    @Comment("상품이 위치한 주소(대여 가능 위치의 기준으로 사용됨) - 국가와 상세 주소를 제외")
    var addressMain: String,

    @Column(name = "address_detail", nullable = false, columnDefinition = "VARCHAR(300)")
    @Comment("상품이 위치한 주소(대여 가능 위치의 기준으로 사용됨) - 상세")
    var addressDetail: String,

    @Column(name = "first_reservable_datetime", nullable = false, columnDefinition = "DATETIME")
    @Comment("상품 예약이 가능한 최초 일시(콘서트 티켓 예매와 같은 서비스를 가정, 예약 러시 처리가 필요)")
    var firstReservableDatetime: LocalDateTime,

    @Column(name = "first_rental_datetime", nullable = false, columnDefinition = "DATETIME")
    @Comment("상품 대여가 가능한 최초 일시")
    var firstRentalDatetime: LocalDateTime,

    @Column(name = "last_rental_datetime", nullable = true, columnDefinition = "DATETIME")
    @Comment("상품 대여가 가능한 마지막 일시(null 이라면 제한 없음)")
    var lastRentalDatetime: LocalDateTime?,

    @Column(name = "reservation_unit_minute", nullable = false, columnDefinition = "BIGINT")
    @Comment("예약 추가 할 수 있는 최소 시간 단위 (분)")
    var reservationUnitMinute: Long,

    @Column(name = "minimum_reservation_unit_count", nullable = false, columnDefinition = "INT UNSIGNED")
    @Comment("단위 예약 시간을 대여일 기준에서 최소 몇번 추가 해야 하는지")
    var minimumReservationUnitCount: Long,

    @Column(name = "maximum_reservation_unit_count", nullable = true, columnDefinition = "INT UNSIGNED")
    @Comment("단위 예약 시간을 대여일 기준에서 최대 몇번 추가 가능한지 (Null 이라면 제한 없음)")
    var maximumReservationUnitCount: Long?,

    @Column(name = "reservation_unit_price", nullable = false, columnDefinition = "DECIMAL(15, 2)")
    @Comment("단위 예약 시간에 대한 가격 (예약 시간 / 단위 예약 시간 * 예약 단가 = 예약 최종가)")
    var reservationUnitPrice: BigDecimal,

    @Column(name = "reservation_unit_price_currency_code", nullable = false, columnDefinition = "CHAR(3)")
    @Comment("단위 예약 시간에 대한 가격 통화 코드(IOS 4217, ex : KRW, USD, EUR...)")
    var reservationUnitPriceCurrencyCode: String,

    @Column(name = "now_reservable", nullable = false, columnDefinition = "BIT(1)")
    @Comment("재고, 상품 상태와 상관 없이 현 시점 예약 가능한지에 대한 관리자의 설정 = 활성/비활성 플래그")
    var nowReservable: Boolean,

    @Column(name = "customer_payment_deadline_minute", nullable = false, columnDefinition = "BIGINT")
    @Comment("고객에게 이때까지 결제를 해야 한다고 통보하는 기한 설정값(예약일로부터 +N 분)")
    var customerPaymentDeadlineMinute: Long,

    @Column(name = "payment_check_deadline_minute", nullable = false, columnDefinition = "BIGINT")
    @Comment("관리자의 결제 확인 기한 설정값(고객 결제 기한 설정값으로 부터 +N 분)")
    var paymentCheckDeadlineMinute: Long,

    @Column(name = "approval_deadline_minute", nullable = false, columnDefinition = "BIGINT")
    @Comment("관리자의 예약 승인 기한 설정값(결제 확인 기한 설정값으로부터 +N분)")
    var approvalDeadlineMinute: Long,

    @Column(name = "cancel_deadline_minute", nullable = false, columnDefinition = "BIGINT")
    @Comment("고객이 예약 취소 가능한 기한 설정값(대여 시작일로부터 -N분이며, 그 결과가 관리자 승인 기한보다 커야함)")
    var cancelDeadlineMinute: Long,

    @Column(name = "product_state_desc", nullable = false, columnDefinition = "VARCHAR(2000)")
    @Comment("상품 상태 설명(예를 들어 손망실의 경우 now_reservable 이 false 이며, 이곳에 손망실 이유가 기재됩니다.)")
    var productStateDesc: String
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

    @Column(name = "version_seq", nullable = false, columnDefinition = "BIGINT")
    @ColumnDefault("0")
    @Comment("예약 상품 정보 버전 시퀀스(고객이 정보를 확인한 시점의 버전과 예약 신청하는 시점의 버전이 다르면 진행 불가)")
    var versionSeq: Long = 0L


    // ---------------------------------------------------------------------------------------------
    // <중첩 클래스 공간>
    // 상품 정보 삭제시 이미지 정보도 삭제
    @OneToMany(
        mappedBy = "rentalProduct",
        fetch = FetchType.LAZY,
        cascade = [CascadeType.ALL]
    )
    var rentalProductImageList: MutableList<Db1_RaillyLinkerCompany_RentalProductImage> =
        mutableListOf()

    // 상품 정보 삭제시 예약 내역에서 null 처리
    @OneToMany(
        mappedBy = "rentalProduct",
        fetch = FetchType.LAZY
    )
    var rentalProductReservationList: MutableList<Db1_RaillyLinkerCompany_RentalProductReservation> =
        mutableListOf()
}