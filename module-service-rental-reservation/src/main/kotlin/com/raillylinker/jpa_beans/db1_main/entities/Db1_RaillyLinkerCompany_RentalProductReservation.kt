package com.raillylinker.jpa_beans.db1_main.entities

import jakarta.persistence.*
import org.hibernate.annotations.ColumnDefault
import org.hibernate.annotations.Comment
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.math.BigDecimal
import java.time.LocalDateTime

// Fk 관계 중 OneToOne 은 논리적 삭제를 적용할시 사용이 불가능합니다.
//     이때는, One to One 역시 Many to One 을 사용하며, 합성 Unique 로 FK 변수를 유니크 처리 한 후,
//     로직상으로 활성화된 행이 한개 뿐이라고 처리하면 됩니다.
@Entity
@Table(
    name = "rental_product_reservation",
    catalog = "railly_linker_company"
)
@Comment("상품 예약 정보")
class Db1_RaillyLinkerCompany_RentalProductReservation(
    @ManyToOne
    @JoinColumn(name = "rental_product_uid", nullable = true)
    @Comment("예약 상품 정보 행 고유키")
    var rentalProduct: Db1_RaillyLinkerCompany_RentalProduct?,

    @ManyToOne
    @JoinColumn(name = "customer_member_uid", nullable = false)
    @Comment("예약자 멤버 행 고유키")
    var totalAuthMember: Db1_RaillyLinkerCompany_TotalAuthMember,

    @Column(name = "real_paid_amount", nullable = false, columnDefinition = "DECIMAL(15, 2)")
    @Comment("실제 결제 금액(할인 등을 적용한 이후, 통화 코드는 예약 정보의 가격 정보와 동일)")
    var realPaidAmount: BigDecimal,

    @Column(name = "rental_start_datetime", nullable = false, columnDefinition = "DATETIME")
    @Comment("대여가 시작되는 일시")
    var rentalStartDatetime: LocalDateTime,

    @Column(name = "rental_end_datetime", nullable = false, columnDefinition = "DATETIME")
    @Comment("대여가 끝나는 일시 (회수 시간은 포함되지 않는 순수 서비스 이용 시간)")
    var rentalEndDatetime: LocalDateTime,

    @Column(name = "customer_payment_deadline_datetime", nullable = false, columnDefinition = "DATETIME")
    @Comment("고객에게 이때까지 결제를 해야 한다고 통보한 기한")
    var customerPaymentDeadlineDatetime: LocalDateTime,

    @Column(name = "payment_check_deadline_datetime", nullable = false, columnDefinition = "DATETIME")
    @Comment("예약 결제 확인 기한 (예약 요청일로부터 생성, 이 시점이 지났고, payment_complete_datetime 가 충족되지 않았다면 취소로 간주)")
    var paymentCheckDeadlineDatetime: LocalDateTime,

    @Column(name = "approval_deadline_datetime", nullable = false, columnDefinition = "DATETIME")
    @Comment("관리자 승인 기한 (이 시점이 지났고, reservation_approval_datetime 가 충족되지 않았다면 취소로 간주)")
    var approvalDeadlineDatetime: LocalDateTime,

    @Column(name = "cancel_deadline_datetime", nullable = false, columnDefinition = "DATETIME")
    @Comment("예약 취소 가능 기한")
    var cancelDeadlineDatetime: LocalDateTime,

    @Column(name = "product_ready_datetime", nullable = true, columnDefinition = "DATETIME")
    @Comment("상품이 대여 반납 이후 준비가 완료된 시간(미리 설정도 가능, 히스토리 테이블 내역보다 우선됩니다.)")
    var productReadyDatetime: LocalDateTime?,

    @Column(name = "version_seq", nullable = false, columnDefinition = "BIGINT")
    @Comment("예약 상품 정보 버전 시퀀스")
    var versionSeq: Long,

    // 아래는 예약 당시 영수증으로서의 기능을 하는 컬럼
    @Column(name = "product_name", nullable = false, columnDefinition = "VARCHAR(90)")
    @Comment("고객에게 보일 상품명")
    var productName: String,

    @Column(name = "product_intro", nullable = false, columnDefinition = "VARCHAR(6000)")
    @Comment("고객에게 보일 상품 소개")
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

    @Column(name = "reservation_unit_minute", nullable = false, columnDefinition = "BIGINT")
    @Comment("예약 추가 할 수 있는 최소 시간 단위 (분)")
    var reservationUnitMinute: Long,

    @Column(name = "reservation_unit_price", nullable = false, columnDefinition = "DECIMAL(15, 2)")
    @Comment("단위 예약 시간에 대한 가격 (예약 시간 / 단위 예약 시간 * 예약 단가 = 예약 최종가)")
    var reservationUnitPrice: BigDecimal,

    @Column(name = "reservation_unit_price_currency_code", nullable = false, columnDefinition = "CHAR(3)")
    @Comment("단위 예약 시간에 대한 가격 통화 코드(IOS 4217, ex : KRW, USD, EUR...)")
    var reservationUnitPriceCurrencyCode: String
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "uid", columnDefinition = "BIGINT")
    @Comment("행 고유값")
    var uid: Long? = null

    @Column(name = "row_create_date", nullable = false, columnDefinition = "DATETIME(3)")
    @CreationTimestamp
    @Comment("행 생성일(= 예약 신청일)")
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
    // 예약 정보가 삭제되면 예약 정보 히스토리 삭제
    @OneToMany(
        mappedBy = "rentalProductReservation",
        fetch = FetchType.LAZY,
        cascade = [CascadeType.ALL]
    )
    var rentalProductReservationHistoryList: MutableList<Db1_RaillyLinkerCompany_RentalProductReservationHistory> =
        mutableListOf()

    // 상품 정보 삭제시 이미지 정보도 삭제
    @OneToMany(
        mappedBy = "rentalProductReservation",
        fetch = FetchType.LAZY,
        cascade = [CascadeType.ALL]
    )
    var rentalProductReservationImageList: MutableList<Db1_RaillyLinkerCompany_RentalProductReservationImage> =
        mutableListOf()
}