package com.raillylinker.jpa_beans.db1_main.repositories

import com.raillylinker.jpa_beans.db1_main.entities.Db1_RaillyLinkerCompany_RentalProduct
import com.raillylinker.jpa_beans.db1_main.entities.Db1_RaillyLinkerCompany_RentalProductReservation
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface Db1_RaillyLinkerCompany_RentalProductReservation_Repository :
    JpaRepository<Db1_RaillyLinkerCompany_RentalProductReservation, Long> {
    fun findByUidAndRowDeleteDateStr(
        uid: Long,
        rowDeleteDateStr: String
    ): Db1_RaillyLinkerCompany_RentalProductReservation?

    fun findAllByTotalAuthMemberUidAndRowDeleteDateStr(
        totalAuthMemberUid: Long,
        rowDeleteDateStr: String
    ): List<Db1_RaillyLinkerCompany_RentalProductReservation>

    fun existsByRentalProductAndRowDeleteDateStrAndProductReadyDatetime(
        rentalProduct: Db1_RaillyLinkerCompany_RentalProduct,
        rowDeleteDateStr: String,
        productReadyDatetime: LocalDateTime?
    ): Boolean
}