package com.raillylinker.services

import com.raillylinker.configurations.SecurityConfig.AuthTokenFilterTotalAuth
import com.raillylinker.configurations.jpa_configs.Db1MainConfig
import com.raillylinker.controllers.PaymentController
import com.raillylinker.jpa_beans.db1_main.entities.Db1_RaillyLinkerCompany_PaymentRefundRequest
import com.raillylinker.jpa_beans.db1_main.entities.Db1_RaillyLinkerCompany_PaymentRequest
import com.raillylinker.jpa_beans.db1_main.entities.Db1_RaillyLinkerCompany_PaymentRequestDetailBankTransfer
import com.raillylinker.jpa_beans.db1_main.entities.Db1_RaillyLinkerCompany_PaymentRequestDetailTossPayments
import com.raillylinker.jpa_beans.db1_main.repositories.*
import com.raillylinker.kafka_components.producers.Kafka1MainProducer
import com.raillylinker.retrofit2_classes.RepositoryNetworkRetrofit2
import com.raillylinker.retrofit2_classes.request_apis.TossPaymentsRequestApi
import com.raillylinker.util_components.JwtTokenUtil
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
class PaymentService(
    // (프로젝트 실행시 사용 설정한 프로필명 (ex : dev8080, prod80, local8080, 설정 안하면 default 반환))
    @Value("\${spring.profiles.active:default}") private var activeProfile: String,
    private val authTokenFilterTotalAuth: AuthTokenFilterTotalAuth,

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

    // Retrofit2 요청 객체
    val networkRetrofit2: RepositoryNetworkRetrofit2 = RepositoryNetworkRetrofit2.getInstance()

    // tossPayments Authorization
    // !!!Toss Payments API 키 수정!!!
    val tossPaymentsAuthorization = "Basic dGVzdF9za196WExrS0V5cE5BcldtbzUwblgzbG1lYXhZRzVSOg=="


    // ---------------------------------------------------------------------------------------------
    // <공개 메소드 공간>
    // (수동 결제 요청)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun postBankTransferRequest(
        httpServletRequest: HttpServletRequest,
        httpServletResponse: HttpServletResponse,
        authorization: String?,
        inputVo: PaymentController.PostBankTransferRequestInputVo
    ): PaymentController.PostBankTransferRequestOutputVo? {
        val notLoggedIn = authTokenFilterTotalAuth.checkRequestAuthorization(httpServletRequest) == null

        val memberEntity =
            if (notLoggedIn) {
                null
            } else {
                val memberUid = jwtTokenUtil.getMemberUid(
                    authorization!!.split(" ")[1].trim(),
                    authTokenFilterTotalAuth.authJwtClaimsAes256InitializationVector,
                    authTokenFilterTotalAuth.authJwtClaimsAes256EncryptionKey
                )
                db1RaillyLinkerCompanyTotalAuthMemberRepository.findByUidAndRowDeleteDateStr(memberUid, "/")!!
            }

        if (inputVo.currencyCode.length != 3) {
            // 통화 코드값의 길이는 3이어야 합니다.
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return null
        }

        val paymentRequest =
            db1RaillyLinkerCompanyPaymentRequestRepository.save(
                Db1_RaillyLinkerCompany_PaymentRequest(
                    memberEntity,
                    1,
                    inputVo.paymentAmount,
                    inputVo.currencyCode.uppercase(),
                    inputVo.paymentReason,
                    null,
                    null
                )
            )

        db1RaillyLinkerCompanyPaymentRequestDetailBankTransferRepository.save(
            Db1_RaillyLinkerCompany_PaymentRequestDetailBankTransfer(
                paymentRequest,
                inputVo.receiveBankName,
                inputVo.receiveBankAccount,
                inputVo.depositoryName
            )
        )

        return PaymentController.PostBankTransferRequestOutputVo(
            paymentRequest.uid!!
        )
    }


    // ----
    // (수동 결제 전체 환불 요청)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun postRequestBankTransferRefundAll(
        httpServletResponse: HttpServletResponse,
        paymentRequestUid: Long,
        inputVo: PaymentController.PostRequestBankTransferRefundAllInputVo
    ): PaymentController.PostRequestBankTransferRefundAllOutputVo? {
        val paymentRequest =
            db1RaillyLinkerCompanyPaymentRequestRepository.findByUidAndRowDeleteDateStr(paymentRequestUid, "/")

        if (paymentRequest == null) {
            // 정보가 없습니다.
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return null
        }

        if (paymentRequest.paymentEndDatetime == null) {
            // 완료되지 않은 결제입니다.
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "2")
            return null
        }

        if (paymentRequest.paymentFailReason != null) {
            // 실패한 결제입니다.
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "3")
            return null
        }

        var refundOnProcess = false
        for (refundRequest in paymentRequest.paymentRefundRequestList) {
            if (refundRequest.rowDeleteDateStr != "/" ||
                (refundRequest.refundFailReason != null && refundRequest.refundEndDatetime != null)
            ) {
                // 삭제 처리된 데이터 skip, 거부 처리된 데이터 skip
                continue
            }

            //  전액 환불 존재 || 부분 환불 하나라도 존재
            refundOnProcess = true
            break
        }

        if (refundOnProcess) {
            // 환불 진행중
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "4")
            return null
        }

        val refundRequest =
            db1RaillyLinkerCompanyPaymentRefundRequestRepository.save(
                Db1_RaillyLinkerCompany_PaymentRefundRequest(
                    paymentRequest,
                    null,
                    inputVo.refundReason,
                    null,
                    null,
                    inputVo.refundBankName,
                    inputVo.refundBankAccount,
                    inputVo.refundHolderName
                )
            )

        return PaymentController.PostRequestBankTransferRefundAllOutputVo(
            refundRequest.uid!!
        )
    }


    // ----
    // (수동 결제 부분 환불 요청)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun postRequestBankTransferRefundPart(
        httpServletResponse: HttpServletResponse,
        paymentRequestUid: Long,
        inputVo: PaymentController.PostRequestBankTransferRefundPartInputVo
    ): PaymentController.PostRequestBankTransferRefundPartOutputVo? {
        val paymentRequest =
            db1RaillyLinkerCompanyPaymentRequestRepository.findByUidAndRowDeleteDateStr(paymentRequestUid, "/")

        if (paymentRequest == null) {
            // 정보가 없습니다.
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return null
        }

        if (paymentRequest.paymentEndDatetime == null) {
            // 완료되지 않은 결제입니다.
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "2")
            return null
        }

        if (paymentRequest.paymentFailReason != null) {
            // 실패한 결제입니다.
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "3")
            return null
        }

        val nowRefundAmount: BigDecimal = inputVo.refundAmount
        var refundOnProcess = false
        for (refundRequest in paymentRequest.paymentRefundRequestList) {
            if (refundRequest.rowDeleteDateStr != "/" ||
                (refundRequest.refundFailReason != null && refundRequest.refundEndDatetime != null)
            ) {
                // 삭제 처리된 데이터 skip, 거부 처리된 데이터 skip
                continue
            }

            if (refundRequest.refundAmount == null) {
                // 전액 환불 존재
                refundOnProcess = true
                break
            }

            // 기존 부분 환불 금액 확인
            nowRefundAmount.add(refundRequest.refundAmount)
        }

        if (refundOnProcess) {
            // 환불 진행중
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "4")
            return null
        }

        if (nowRefundAmount > paymentRequest.paymentAmount) {
            // 환불 가능 금액 초과
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "5")
            return null
        }

        val refundRequest =
            db1RaillyLinkerCompanyPaymentRefundRequestRepository.save(
                Db1_RaillyLinkerCompany_PaymentRefundRequest(
                    paymentRequest,
                    null,
                    inputVo.refundReason,
                    null,
                    null,
                    inputVo.refundBankName,
                    inputVo.refundBankAccount,
                    inputVo.refundHolderName
                )
            )

        return PaymentController.PostRequestBankTransferRefundPartOutputVo(
            refundRequest.uid!!
        )
    }


    // ----
    // (PG 결제 요청(Toss Payments))
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun postPgTossPaymentsRequest(
        httpServletRequest: HttpServletRequest,
        httpServletResponse: HttpServletResponse,
        authorization: String?,
        inputVo: PaymentController.PostPgTossPaymentsRequestInputVo
    ): PaymentController.PostPgTossPaymentsRequestOutputVo? {
        val notLoggedIn = authTokenFilterTotalAuth.checkRequestAuthorization(httpServletRequest) == null

        val memberEntity =
            if (notLoggedIn) {
                null
            } else {
                val memberUid = jwtTokenUtil.getMemberUid(
                    authorization!!.split(" ")[1].trim(),
                    authTokenFilterTotalAuth.authJwtClaimsAes256InitializationVector,
                    authTokenFilterTotalAuth.authJwtClaimsAes256EncryptionKey
                )
                db1RaillyLinkerCompanyTotalAuthMemberRepository.findByUidAndRowDeleteDateStr(memberUid, "/")!!
            }

        val requestPaymentResponse =
            networkRetrofit2.tossPaymentsRequestApi.postV1PaymentsConfirm(
                tossPaymentsAuthorization,
                TossPaymentsRequestApi.PostV1PaymentsConfirmInputVO(
                    inputVo.paymentKey,
                    inputVo.orderId,
                    inputVo.paymentAmount
                )
            ).execute()

        if (requestPaymentResponse.isSuccessful) {
            // 정상 응답 (200 OK)
            val successData = requestPaymentResponse.body()!!

            val paymentRequest =
                db1RaillyLinkerCompanyPaymentRequestRepository.save(
                    Db1_RaillyLinkerCompany_PaymentRequest(
                        memberEntity,
                        2,
                        BigDecimal.valueOf(inputVo.paymentAmount),
                        "KRW",
                        inputVo.paymentReason,
                        null,
                        null
                    )
                )

            db1RaillyLinkerCompanyPaymentRequestDetailTossPaymentsRepository.save(
                Db1_RaillyLinkerCompany_PaymentRequestDetailTossPayments(
                    paymentRequest,
                    inputVo.paymentKey,
                    inputVo.orderId,
                    successData.method
                )
            )

            return PaymentController.PostPgTossPaymentsRequestOutputVo(
                paymentRequest.uid!!
            )
        } else {
            // 오류 응답 (400, 500 등)
//            val errorBody = requestPaymentResponse.errorBody()?.string()
//            val errorData =
//                Gson().fromJson(errorBody, TossPaymentsRequestApi.PostV1PaymentsConfirmErrorOutputVO::class.java)

            // Toss Payments API 호출 실패
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return null
        }
    }


    // ----
    // (PG 결제(Toss Payments) 환불 요청)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun postRequestPgTossPaymentsRefundAll(
        httpServletResponse: HttpServletResponse,
        paymentRequestUid: Long,
        inputVo: PaymentController.PostRequestPgTossPaymentsRefundAllInputVo
    ): PaymentController.PostRequestPgTossPaymentsRefundAllOutputVo? {
        val paymentRequest =
            db1RaillyLinkerCompanyPaymentRequestRepository.findByUidAndRowDeleteDateStr(paymentRequestUid, "/")

        if (paymentRequest == null) {
            // 정보가 없습니다.
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return null
        }

        if (paymentRequest.paymentEndDatetime == null) {
            // 완료되지 않은 결제입니다.
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "2")
            return null
        }

        if (paymentRequest.paymentFailReason != null) {
            // 실패한 결제입니다.
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "3")
            return null
        }

        var refundOnProcess = false
        for (refundRequest in paymentRequest.paymentRefundRequestList) {
            if (refundRequest.rowDeleteDateStr != "/" ||
                (refundRequest.refundFailReason != null && refundRequest.refundEndDatetime != null)
            ) {
                // 삭제 처리된 데이터 skip, 거부 처리된 데이터 skip
                continue
            }

            //  전액 환불 존재 || 부분 환불 하나라도 존재
            refundOnProcess = true
            break
        }

        if (refundOnProcess) {
            // 환불 진행중
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "4")
            return null
        }

        val tossEntity =
            db1RaillyLinkerCompanyPaymentRequestDetailTossPaymentsRepository.findFirstByPaymentRequestUidAndRowDeleteDateStrOrderByRowCreateDateDesc(
                paymentRequest.uid!!,
                "/"
            )

        if (tossEntity == null) {
            // 필수 데이터 결여
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return null
        }

        var refundReceiveAccountInfoObj: TossPaymentsRequestApi.PostV1PaymentsCancelInputVO.RefundReceiveAccount? = null
        if (tossEntity.tossMethod == "가상계좌") {
            if (inputVo.refundReceiveAccountObj == null) {
                // 가상 계좌 결제이지만 필수 refundReceiveAccountObj 가 null 입니다.
                httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                httpServletResponse.setHeader("api-result-code", "5")
                return null
            }

            refundReceiveAccountInfoObj =
                TossPaymentsRequestApi.PostV1PaymentsCancelInputVO.RefundReceiveAccount(
                    inputVo.refundReceiveAccountObj.bank,
                    inputVo.refundReceiveAccountObj.accountNumber,
                    inputVo.refundReceiveAccountObj.holderName
                )
        }

        // api 요청. 실패시 api-result-code 6 반환
        val requestPaymentResponse =
            networkRetrofit2.tossPaymentsRequestApi.postV1PaymentsCancel(
                tossPaymentsAuthorization,
                tossEntity.tossPaymentKey,
                TossPaymentsRequestApi.PostV1PaymentsCancelInputVO(
                    inputVo.refundReason,
                    null,
                    refundReceiveAccountInfoObj
                )
            ).execute()

        if (requestPaymentResponse.isSuccessful) {
            // 정상 응답 (200 OK)
//            val successData = requestPaymentResponse.body()!!

            val refundRequest =
                db1RaillyLinkerCompanyPaymentRefundRequestRepository.save(
                    Db1_RaillyLinkerCompany_PaymentRefundRequest(
                        paymentRequest,
                        null,
                        inputVo.refundReason,
                        null,
                        null,
                        inputVo.refundReceiveAccountObj?.bank,
                        inputVo.refundReceiveAccountObj?.accountNumber,
                        inputVo.refundReceiveAccountObj?.holderName
                    )
                )

            return PaymentController.PostRequestPgTossPaymentsRefundAllOutputVo(
                refundRequest.uid!!
            )
        } else {
            // 오류 응답 (400, 500 등)
//            val errorBody = requestPaymentResponse.errorBody()?.string()
//            val errorData =
//                Gson().fromJson(errorBody, TossPaymentsRequestApi.PostV1PaymentsCancelErrorOutputVO::class.java)

            // Toss Payments API 호출 실패
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "6")
            return null
        }
    }


    // ----
    // (PG 결제(Toss Payments) 부분 환불 요청)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun postRequestPgTossPaymentsRefundPart(
        httpServletResponse: HttpServletResponse,
        paymentRequestUid: Long,
        inputVo: PaymentController.PostRequestPgTossPaymentsRefundPartInputVo
    ): PaymentController.PostRequestPgTossPaymentsRefundPartOutputVo? {
        val paymentRequest =
            db1RaillyLinkerCompanyPaymentRequestRepository.findByUidAndRowDeleteDateStr(paymentRequestUid, "/")

        if (paymentRequest == null) {
            // 정보가 없습니다.
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return null
        }

        if (paymentRequest.paymentEndDatetime == null) {
            // 완료되지 않은 결제입니다.
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "2")
            return null
        }

        if (paymentRequest.paymentFailReason != null) {
            // 실패한 결제입니다.
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "3")
            return null
        }

        val nowRefundAmount: BigDecimal = BigDecimal.valueOf(inputVo.refundAmount)
        var refundOnProcess = false
        for (refundRequest in paymentRequest.paymentRefundRequestList) {
            if (refundRequest.rowDeleteDateStr != "/" ||
                (refundRequest.refundFailReason != null && refundRequest.refundEndDatetime != null)
            ) {
                // 삭제 처리된 데이터 skip, 거부 처리된 데이터 skip
                continue
            }

            if (refundRequest.refundAmount == null) {
                // 전액 환불 존재
                refundOnProcess = true
                break
            }

            // 기존 부분 환불 금액 확인
            nowRefundAmount.add(refundRequest.refundAmount)
        }

        if (refundOnProcess) {
            // 환불 진행중
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "4")
            return null
        }

        if (nowRefundAmount > paymentRequest.paymentAmount) {
            // 환불 가능 금액 초과
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "7")
            return null
        }

        val tossEntity =
            db1RaillyLinkerCompanyPaymentRequestDetailTossPaymentsRepository.findFirstByPaymentRequestUidAndRowDeleteDateStrOrderByRowCreateDateDesc(
                paymentRequest.uid!!,
                "/"
            )

        if (tossEntity == null) {
            // 필수 데이터 결여
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return null
        }

        var refundReceiveAccountInfoObj: TossPaymentsRequestApi.PostV1PaymentsCancelInputVO.RefundReceiveAccount? = null
        if (tossEntity.tossMethod == "가상계좌") {
            if (inputVo.refundReceiveAccountObj == null) {
                // 가상 계좌 결제이지만 필수 refundReceiveAccountObj 가 null 입니다.
                httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                httpServletResponse.setHeader("api-result-code", "5")
                return null
            }

            refundReceiveAccountInfoObj =
                TossPaymentsRequestApi.PostV1PaymentsCancelInputVO.RefundReceiveAccount(
                    inputVo.refundReceiveAccountObj.bank,
                    inputVo.refundReceiveAccountObj.accountNumber,
                    inputVo.refundReceiveAccountObj.holderName
                )
        }

        // api 요청. 실패시 api-result-code 6 반환
        val requestPaymentResponse =
            networkRetrofit2.tossPaymentsRequestApi.postV1PaymentsCancel(
                tossPaymentsAuthorization,
                tossEntity.tossPaymentKey,
                TossPaymentsRequestApi.PostV1PaymentsCancelInputVO(
                    inputVo.refundReason,
                    inputVo.refundAmount,
                    refundReceiveAccountInfoObj
                )
            ).execute()

        if (requestPaymentResponse.isSuccessful) {
            // 정상 응답 (200 OK)
//            val successData = requestPaymentResponse.body()!!

            val refundRequest =
                db1RaillyLinkerCompanyPaymentRefundRequestRepository.save(
                    Db1_RaillyLinkerCompany_PaymentRefundRequest(
                        paymentRequest,
                        BigDecimal.valueOf(inputVo.refundAmount),
                        inputVo.refundReason,
                        null,
                        null,
                        inputVo.refundReceiveAccountObj?.bank,
                        inputVo.refundReceiveAccountObj?.accountNumber,
                        inputVo.refundReceiveAccountObj?.holderName
                    )
                )

            return PaymentController.PostRequestPgTossPaymentsRefundPartOutputVo(
                refundRequest.uid!!
            )
        } else {
            // 오류 응답 (400, 500 등)
//            val errorBody = requestPaymentResponse.errorBody()?.string()
//            val errorData =
//                Gson().fromJson(errorBody, TossPaymentsRequestApi.PostV1PaymentsCancelErrorOutputVO::class.java)

            // Toss Payments API 호출 실패
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "6")
            return null
        }
    }
}