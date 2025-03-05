package com.raillylinker.jpa_beans.db1_main.repositories

import com.raillylinker.jpa_beans.db1_main.entities.Db1_RaillyLinkerCompany_PaymentRequest
import com.raillylinker.jpa_beans.db1_main.entities.Db1_RaillyLinkerCompany_RentalProductReservation
import com.raillylinker.jpa_beans.db1_main.entities.Db1_RaillyLinkerCompany_RentalProductReservationPayment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface Db1_RaillyLinkerCompany_RentalProductReservationPayment_Repository :
    JpaRepository<Db1_RaillyLinkerCompany_RentalProductReservationPayment, Long> {
    fun findByUidAndRowDeleteDateStr(
        uid: Long,
        rowDeleteDateStr: String
    ): Db1_RaillyLinkerCompany_RentalProductReservationPayment?

    fun findAllByRentalProductReservationAndRowDeleteDateStr(
        rentalProductReservation: Db1_RaillyLinkerCompany_RentalProductReservation,
        rowDeleteDateStr: String
    ): List<Db1_RaillyLinkerCompany_RentalProductReservationPayment>

    fun findAllByPaymentRequestAndRowDeleteDateStr(
        paymentRequest: Db1_RaillyLinkerCompany_PaymentRequest,
        rowDeleteDateStr: String
    ): List<Db1_RaillyLinkerCompany_RentalProductReservationPayment>
}