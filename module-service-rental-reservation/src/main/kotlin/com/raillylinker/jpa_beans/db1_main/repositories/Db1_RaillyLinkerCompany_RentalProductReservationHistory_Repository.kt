package com.raillylinker.jpa_beans.db1_main.repositories

import com.raillylinker.jpa_beans.db1_main.entities.Db1_RaillyLinkerCompany_RentalProductReservation
import com.raillylinker.jpa_beans.db1_main.entities.Db1_RaillyLinkerCompany_RentalProductReservationHistory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface Db1_RaillyLinkerCompany_RentalProductReservationHistory_Repository :
    JpaRepository<Db1_RaillyLinkerCompany_RentalProductReservationHistory, Long> {
    fun findAllByRentalProductReservationAndRowDeleteDateStrOrderByRowCreateDateDesc(
        rentalProductReservation: Db1_RaillyLinkerCompany_RentalProductReservation,
        rowDeleteDateStr: String
    ): List<Db1_RaillyLinkerCompany_RentalProductReservationHistory>

    fun findByUidAndRowDeleteDateStr(
        uid: Long,
        rowDeleteDateStr: String
    ): Db1_RaillyLinkerCompany_RentalProductReservationHistory?
}