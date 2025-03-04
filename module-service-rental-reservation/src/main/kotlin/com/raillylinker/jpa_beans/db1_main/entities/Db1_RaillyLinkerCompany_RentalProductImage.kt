package com.raillylinker.jpa_beans.db1_main.entities

import jakarta.persistence.*
import org.hibernate.annotations.ColumnDefault
import org.hibernate.annotations.Comment
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@Entity
@Table(
    name = "rental_product_image",
    catalog = "railly_linker_company"
)
@Comment("예약 상품 이미지 테이블")
class Db1_RaillyLinkerCompany_RentalProductImage(
    @ManyToOne
    @JoinColumn(name = "rental_product_uid", nullable = false)
    @Comment("예약 상품 정보 행 고유키")
    var rentalProduct: Db1_RaillyLinkerCompany_RentalProduct,

    @Column(name = "image_full_url", nullable = false, columnDefinition = "VARCHAR(200)")
    @Comment("상품 이미지 Full URL")
    var imageFullUrl: String,

    @Column(name = "priority", nullable = false, columnDefinition = "MEDIUMINT UNSIGNED")
    @Comment("가중치(높을수록 전면에 표시되며, 동일 가중치의 경우 최신 정보가 우선됩니다.)")
    var priority: Int
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