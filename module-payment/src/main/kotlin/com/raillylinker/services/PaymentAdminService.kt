package com.raillylinker.services

import com.raillylinker.configurations.jpa_configs.Db1MainConfig
import com.raillylinker.controllers.PaymentAdminController
import com.raillylinker.jpa_beans.db1_main.repositories.*
import com.raillylinker.kafka_components.producers.Kafka1MainProducer
import com.raillylinker.util_components.JwtTokenUtil
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class PaymentAdminService(
    // (프로젝트 실행시 사용 설정한 프로필명 (ex : dev8080, prod80, local8080, 설정 안하면 default 반환))
    @Value("\${spring.profiles.active:default}") private var activeProfile: String,

    private val jwtTokenUtil: JwtTokenUtil,
    private val db1RaillyLinkerCompanyTotalAuthMemberRepository: Db1_RaillyLinkerCompany_TotalAuthMember_Repository,

    private val db1RaillyLinkerCompanyPaymentRequestRepository: Db1_RaillyLinkerCompany_PaymentRequest_Repository,
    private val db1RaillyLinkerCompanyPaymentRefundRequestRepository: Db1_RaillyLinkerCompany_PaymentRefundRequest_Repository,
    private val db1RaillyLinkerCompanyPaymentRequestDetailBankTransferRepository: Db1_RaillyLinkerCompany_PaymentRequestDetailBankTransfer_Repository,
    private val db1RaillyLinkerCompanyPaymentRequestDetailTossPaymentsRepository: Db1_RaillyLinkerCompany_PaymentRequestDetailTossPayments_Repository,

    private val kafka1MainProducer: Kafka1MainProducer
) {
    // <멤버 변수 공간>
    private val classLogger: Logger = LoggerFactory.getLogger(this::class.java)

    // (현 프로젝트 동작 서버의 외부 접속 주소)
    // 프로필 이미지 로컬 저장 및 다운로드 주소 지정을 위해 필요
    // !!!프로필별 접속 주소 설정하기!!
    // ex : http://127.0.0.1:8080
    private val externalAccessAddress: String
        get() {
            return when (activeProfile) {
                "prod80" -> {
                    "http://127.0.0.1"
                }

                "dev8080" -> {
                    "http://127.0.0.1:8080"
                }

                else -> {
                    "http://127.0.0.1:8080"
                }
            }
        }


    // ---------------------------------------------------------------------------------------------
    // <공개 메소드 공간>
    // (결제 실패 처리 <'ADMIN'>)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun putPaymentRequestFail(
        httpServletResponse: HttpServletResponse,
        authorization: String,
        paymentRequestUid: Long,
        inputVo: PaymentAdminController.PutPaymentRequestFailInputVo
    ) {
        val paymentRequest =
            db1RaillyLinkerCompanyPaymentRequestRepository.findByUidAndRowDeleteDateStr(paymentRequestUid, "/")

        if (paymentRequest == null) {
            // 정보가 없음
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return
        }

        if (paymentRequest.paymentFailReason != null && paymentRequest.paymentEndDatetime != null) {
            // 이미 실패 처리 됨
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "2")
            return
        }

        if (paymentRequest.paymentEndDatetime != null) {
            // 결제 완료 처리 됨
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "3")
            return
        }

        // 결제 실패 처리
        paymentRequest.paymentFailReason = inputVo.paymentFailReason
        paymentRequest.paymentEndDatetime = LocalDateTime.now()
        db1RaillyLinkerCompanyPaymentRequestRepository.save(paymentRequest)

        // kafka 발송
        kafka1MainProducer.sendMessageFromPaymentPaymentFailed(
            Kafka1MainProducer.SendMessageFromPaymentPaymentFailedInputVo(
                paymentRequestUid
            )
        )

        httpServletResponse.status = HttpStatus.OK.value()
    }


    // ----
    // (결제 완료 처리 <'ADMIN'>)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun putPaymentRequestComplete(
        httpServletResponse: HttpServletResponse,
        authorization: String,
        paymentRequestUid: Long
    ) {
        val paymentRequest =
            db1RaillyLinkerCompanyPaymentRequestRepository.findByUidAndRowDeleteDateStr(paymentRequestUid, "/")

        if (paymentRequest == null) {
            // 정보가 없음
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return
        }

        if (paymentRequest.paymentFailReason != null && paymentRequest.paymentEndDatetime != null) {
            // 이미 실패 처리 됨
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "2")
            return
        }

        if (paymentRequest.paymentEndDatetime != null) {
            // 결제 완료 처리 됨
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "3")
            return
        }

        // 결제 완료 처리
        paymentRequest.paymentEndDatetime = LocalDateTime.now()
        db1RaillyLinkerCompanyPaymentRequestRepository.save(paymentRequest)

        // kafka 발송
        kafka1MainProducer.sendMessageFromPaymentPaymentCompleted(
            Kafka1MainProducer.SendMessageFromPaymentPaymentCompletedInputVo(
                paymentRequestUid
            )
        )

        httpServletResponse.status = HttpStatus.OK.value()
    }


    // ----
    // (환불 거부 처리 <'ADMIN'>)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun putPaymentRefundRequestReject(
        httpServletResponse: HttpServletResponse,
        authorization: String,
        paymentRefundRequestUid: Long,
        inputVo: PaymentAdminController.PutPaymentRefundRequestRejectInputVo
    ) {
        val paymentRefundRequest =
            db1RaillyLinkerCompanyPaymentRefundRequestRepository.findByUidAndRowDeleteDateStr(
                paymentRefundRequestUid,
                "/"
            )

        if (paymentRefundRequest == null) {
            // 정보가 없음
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return
        }

        if (paymentRefundRequest.refundFailReason != null && paymentRefundRequest.refundEndDatetime != null) {
            // 이미 실패 처리 됨
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "2")
            return
        }

        if (paymentRefundRequest.refundEndDatetime != null) {
            // 환불 완료 처리 됨
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "3")
            return
        }

        // 결제 실패 처리
        paymentRefundRequest.refundFailReason = inputVo.paymentRefundRejectReason
        paymentRefundRequest.refundEndDatetime = LocalDateTime.now()
        db1RaillyLinkerCompanyPaymentRefundRequestRepository.save(paymentRefundRequest)

        // kafka 발송
        kafka1MainProducer.sendMessageFromPaymentPaymentRefundFailed(
            Kafka1MainProducer.SendMessageFromPaymentPaymentRefundFailedInputVo(
                paymentRefundRequestUid
            )
        )

        httpServletResponse.status = HttpStatus.OK.value()
    }


    // ----
    // (환불 완료 처리 <'ADMIN'>)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun putPaymentRefundRequestComplete(
        httpServletResponse: HttpServletResponse,
        authorization: String,
        paymentRefundRequestUid: Long
    ) {
        val paymentRefundRequest =
            db1RaillyLinkerCompanyPaymentRefundRequestRepository.findByUidAndRowDeleteDateStr(
                paymentRefundRequestUid,
                "/"
            )

        if (paymentRefundRequest == null) {
            // 정보가 없음
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return
        }

        if (paymentRefundRequest.refundFailReason != null && paymentRefundRequest.refundEndDatetime != null) {
            // 이미 실패 처리 됨
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "2")
            return
        }

        if (paymentRefundRequest.refundEndDatetime != null) {
            // 환불 완료 처리 됨
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "3")
            return
        }

        // 결제 완료 처리
        paymentRefundRequest.refundEndDatetime = LocalDateTime.now()
        db1RaillyLinkerCompanyPaymentRefundRequestRepository.save(paymentRefundRequest)

        // kafka 발송
        kafka1MainProducer.sendMessageFromPaymentPaymentRefundCompleted(
            Kafka1MainProducer.SendMessageFromPaymentPaymentRefundCompletedInputVo(
                paymentRefundRequestUid
            )
        )

        httpServletResponse.status = HttpStatus.OK.value()
    }
}