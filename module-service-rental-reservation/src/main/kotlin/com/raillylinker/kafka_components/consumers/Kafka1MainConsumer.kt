package com.raillylinker.kafka_components.consumers

import com.google.gson.Gson
import com.raillylinker.configurations.jpa_configs.Db1MainConfig
import com.raillylinker.configurations.kafka_configs.Kafka1MainConfig
import com.raillylinker.jpa_beans.db1_main.repositories.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Component
class Kafka1MainConsumer(
    private val db1RaillyLinkerCompanyRentalProductReservationRepository: Db1_RaillyLinkerCompany_RentalProductReservation_Repository,
    private val db1RaillyLinkerCompanyRentalProductReservationHistoryRepository: Db1_RaillyLinkerCompany_RentalProductReservationHistory_Repository,
    private val db1RaillyLinkerCompanyRentalProductReservationImageRepository: Db1_RaillyLinkerCompany_RentalProductReservationImage_Repository
) {
    // <멤버 변수 공간>
    private val classLogger: Logger = LoggerFactory.getLogger(this::class.java)

    companion object {
        // !!!모듈 컨슈머 그룹 아이디!!!
        private const val CONSUMER_GROUP_ID = "com.raillylinker.service_rental_reservation"
    }

    // ---------------------------------------------------------------------------------------------
    // <공개 메소드 공간>
    // (Auth 모듈의 통합 멤버 정보 삭제 이벤트에 대한 리스너)
    // 이와 연관된 데이터 삭제 및 기타 처리
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    @KafkaListener(
        topics = ["from_auth_db_delete_from_railly_linker_company_total_auth_member"],
        groupId = CONSUMER_GROUP_ID,
        containerFactory = Kafka1MainConfig.CONSUMER_BEAN_NAME
    )
    fun fromAuthDbDeleteFromRaillyLinkerCompanyTotalAuthMemberListener(data: ConsumerRecord<String, String>) {
        classLogger.info(
            """
                KafkaConsumerLog>>
                {
                    "data" : {
                        "$data"
                    }
                }
            """.trimIndent()
        )

        val inputVo = Gson().fromJson(
            data.value(),
            FromAuthDbDeleteFromRaillyLinkerCompanyTotalAuthMemberListenerInputVo::class.java
        )

        val reservationInfoList =
            db1RaillyLinkerCompanyRentalProductReservationRepository.findAllByTotalAuthMemberUidAndRowDeleteDateStr(
                inputVo.deletedMemberUid,
                "/"
            )

        for (reservationInfo in reservationInfoList) {
            // 삭제된 멤버 정보와 연관된 정보 삭제 처리
            val reservationHistoryList = reservationInfo.rentalProductReservationHistoryList
            for (reservationHistory in reservationHistoryList) {
                reservationHistory.rowDeleteDateStr =
                    LocalDateTime.now().atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("yyyy_MM_dd_'T'_HH_mm_ss_SSS_z"))
                db1RaillyLinkerCompanyRentalProductReservationHistoryRepository.save(
                    reservationHistory
                )
            }

            val reservationImageList = reservationInfo.rentalProductReservationImageList
            for (reservationImage in reservationImageList) {
                reservationImage.rowDeleteDateStr =
                    LocalDateTime.now().atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("yyyy_MM_dd_'T'_HH_mm_ss_SSS_z"))
                db1RaillyLinkerCompanyRentalProductReservationImageRepository.save(
                    reservationImage
                )
            }

            reservationInfo.rowDeleteDateStr =
                LocalDateTime.now().atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("yyyy_MM_dd_'T'_HH_mm_ss_SSS_z"))
            db1RaillyLinkerCompanyRentalProductReservationRepository.save(
                reservationInfo
            )

            // 삭제한 이미지 파일 실제 삭제 처리 (상품 이미지에서 해당 이미지를 사용하지 않은 이미지 파일만 삭제하도록 처리)
        }
    }

    data class FromAuthDbDeleteFromRaillyLinkerCompanyTotalAuthMemberListenerInputVo(
        val deletedMemberUid: Long
    )
}