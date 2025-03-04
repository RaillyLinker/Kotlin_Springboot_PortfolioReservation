package com.raillylinker.controllers

import com.fasterxml.jackson.annotation.JsonProperty
import com.raillylinker.services.PaymentService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.headers.Header
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal

// PG 사 별 API 는 별도로 분리합니다.
// 이유는, PG 사별 요구하는 데이터 종류, 정책, 처리 방식 등이 다를 수 있기 때문입니다.
@Tag(name = "/payment APIs", description = "결제 API 컨트롤러")
@Controller
@RequestMapping("/payment")
class PaymentController(
    private val service: PaymentService
) {
    // <멤버 변수 공간>


    // ---------------------------------------------------------------------------------------------
    // <매핑 함수 공간>
    @Operation(
        summary = "수동 결제 요청 <>?",
        description = "수동 결제 요청 API"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정상 동작"
            ),
            ApiResponse(
                responseCode = "204",
                content = [Content()],
                description = "Response Body 가 없습니다.<br>" +
                        "Response Headers 를 확인하세요.",
                headers = [
                    Header(
                        name = "api-result-code",
                        description = "(Response Code 반환 원인) - Required<br>" +
                                "1 : 통화 코드값의 길이는 3이어야 합니다.",
                        schema = Schema(type = "string")
                    )
                ]
            )
        ]
    )
    @PostMapping(
        path = ["/bank-transfer/request"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @ResponseBody
    fun postBankTransferRequest(
        @Parameter(hidden = true)
        httpServletRequest: HttpServletRequest,
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(hidden = true)
        @RequestHeader("Authorization")
        authorization: String?,
        @RequestBody
        inputVo: PostBankTransferRequestInputVo
    ): PostBankTransferRequestOutputVo? {
        return service.postBankTransferRequest(
            httpServletRequest,
            httpServletResponse,
            authorization,
            inputVo
        )
    }

    data class PostBankTransferRequestInputVo(
        @Schema(description = "결제 금액", required = true, example = "1000")
        @JsonProperty("paymentAmount")
        val paymentAmount: BigDecimal,
        @Schema(
            description = "결제 금액 통화 코드(IOS 4217, ex : KRW, USD, EUR...)",
            required = true,
            example = "KRW"
        )
        @JsonProperty("currencyCode")
        val currencyCode: String,
        @Schema(description = "결제 이유", required = true, example = "상품 구입")
        @JsonProperty("paymentReason")
        val paymentReason: String,
        @Schema(description = "입금 받을 은행명", required = true, example = "서울은행")
        @JsonProperty("receiveBankName")
        val receiveBankName: String,
        @Schema(description = "입금 받을 은행 계좌번호", required = true, example = "11-11111-1111")
        @JsonProperty("receiveBankAccount")
        val receiveBankAccount: String,
        @Schema(description = "입금자 이름", required = false, example = "홍길동")
        @JsonProperty("depositoryName")
        val depositoryName: String?
    )

    data class PostBankTransferRequestOutputVo(
        @Schema(description = "payment request 고유값", required = true, example = "1")
        @JsonProperty("paymentRequestUid")
        val paymentRequestUid: Long
    )


    // ----
    @Operation(
        summary = "수동 결제 전체 환불 요청",
        description = "수동 결제 전체 환불 요청 API"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정상 동작"
            ),
            ApiResponse(
                responseCode = "204",
                content = [Content()],
                description = "Response Body 가 없습니다.<br>" +
                        "Response Headers 를 확인하세요.",
                headers = [
                    Header(
                        name = "api-result-code",
                        description = "(Response Code 반환 원인) - Required<br>" +
                                "1 : 정보가 없습니다.<br>" +
                                "2 : 완료되지 않은 결제입니다.<br>" +
                                "3 : 실패한 결제입니다.<br>" +
                                "4 : 환불 내역이 존재합니다.",
                        schema = Schema(type = "string")
                    )
                ]
            )
        ]
    )
    @PostMapping(
        path = ["/request/{paymentRequestUid}/bank-transfer-refund-all"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @ResponseBody
    fun postRequestBankTransferRefundAll(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(name = "paymentRequestUid", description = "결제 요청 정보 고유값", example = "1")
        @PathVariable("paymentRequestUid")
        paymentRequestUid: Long,
        @RequestBody
        inputVo: PostRequestBankTransferRefundAllInputVo
    ): PostRequestBankTransferRefundAllOutputVo? {
        return service.postRequestBankTransferRefundAll(
            httpServletResponse,
            paymentRequestUid,
            inputVo
        )
    }

    data class PostRequestBankTransferRefundAllInputVo(
        @Schema(description = "환불 이유", required = true, example = "상품 하자")
        @JsonProperty("refundReason")
        val refundReason: String,
        @Schema(
            description = "환불 받을 은행명",
            required = true,
            example = "경남은행"
        )
        @JsonProperty("refundBankName")
        val refundBankName: String,
        @Schema(
            description = "환불 받을 은행 계좌번호",
            required = true,
            example = "02-0000-0000"
        )
        @JsonProperty("refundBankAccount")
        val refundBankAccount: String,
        @Schema(
            description = "환불 받을 은행 예금주",
            required = true,
            example = "홍길동"
        )
        @JsonProperty("refundHolderName")
        val refundHolderName: String
    )

    data class PostRequestBankTransferRefundAllOutputVo(
        @Schema(description = "payment refund 고유값", required = true, example = "1")
        @JsonProperty("paymentRefundUid")
        val paymentRefundUid: Long
    )


    // ----
    @Operation(
        summary = "수동 결제 부분 환불 요청",
        description = "수동 결제 부분 환불 요청 API"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정상 동작"
            ),
            ApiResponse(
                responseCode = "204",
                content = [Content()],
                description = "Response Body 가 없습니다.<br>" +
                        "Response Headers 를 확인하세요.",
                headers = [
                    Header(
                        name = "api-result-code",
                        description = "(Response Code 반환 원인) - Required<br>" +
                                "1 : 정보가 없습니다.<br>" +
                                "2 : 완료되지 않은 결제입니다.<br>" +
                                "3 : 실패한 결제입니다.<br>" +
                                "4 : 전액 환불 내역이 존재합니다.<br>" +
                                "5 : 환불 가능 금액을 넘어섰습니다.",
                        schema = Schema(type = "string")
                    )
                ]
            )
        ]
    )
    @PostMapping(
        path = ["/request/{paymentRequestUid}/bank-transfer-refund-part"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @ResponseBody
    fun postRequestBankTransferRefundPart(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(name = "paymentRequestUid", description = "결제 요청 정보 고유값", example = "1")
        @PathVariable("paymentRequestUid")
        paymentRequestUid: Long,
        @RequestBody
        inputVo: PostRequestBankTransferRefundPartInputVo
    ): PostRequestBankTransferRefundPartOutputVo? {
        return service.postRequestBankTransferRefundPart(
            httpServletResponse,
            paymentRequestUid,
            inputVo
        )
    }

    data class PostRequestBankTransferRefundPartInputVo(
        @Schema(description = "환불 금액(통화 코드는 결제 금액과 같다고 간주합니다.)", required = true, example = "10000")
        @JsonProperty("refundAmount")
        val refundAmount: BigDecimal,
        @Schema(description = "환불 이유", required = true, example = "상품 하자")
        @JsonProperty("refundReason")
        val refundReason: String,
        @Schema(
            description = "환불 받을 은행명",
            required = true,
            example = "경남은행"
        )
        @JsonProperty("refundBankName")
        val refundBankName: String,
        @Schema(
            description = "환불 받을 은행 계좌번호",
            required = true,
            example = "02-0000-0000"
        )
        @JsonProperty("refundBankAccount")
        val refundBankAccount: String,
        @Schema(
            description = "환불 받을 은행 예금주",
            required = true,
            example = "홍길동"
        )
        @JsonProperty("refundHolderName")
        val refundHolderName: String
    )

    data class PostRequestBankTransferRefundPartOutputVo(
        @Schema(description = "payment refund 고유값", required = true, example = "1")
        @JsonProperty("paymentRefundUid")
        val paymentRefundUid: Long
    )


    // ----
    @Operation(
        summary = "PG 결제 요청(Toss Payments) <>?",
        description = "PG 결제 요청(Toss Payments) API"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정상 동작"
            ),
            ApiResponse(
                responseCode = "204",
                content = [Content()],
                description = "Response Body 가 없습니다.<br>" +
                        "Response Headers 를 확인하세요.",
                headers = [
                    Header(
                        name = "api-result-code",
                        description = "(Response Code 반환 원인) - Required<br>" +
                                "1 : Toss Payments API 호출 실패",
                        schema = Schema(type = "string")
                    )
                ]
            )
        ]
    )
    @PostMapping(
        path = ["/pg-toss-payments/request"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @ResponseBody
    fun postPgTossPaymentsRequest(
        @Parameter(hidden = true)
        httpServletRequest: HttpServletRequest,
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(hidden = true)
        @RequestHeader("Authorization")
        authorization: String?,
        @RequestBody
        inputVo: PostPgTossPaymentsRequestInputVo
    ): PostPgTossPaymentsRequestOutputVo? {
        return service.postPgTossPaymentsRequest(
            httpServletRequest,
            httpServletResponse,
            authorization,
            inputVo
        )
    }

    data class PostPgTossPaymentsRequestInputVo(
        @Schema(description = "결제 금액(통화 코드는 KRW 로 간주합니다.)", required = true, example = "1000")
        @JsonProperty("paymentAmount")
        val paymentAmount: Long,
        @Schema(description = "결제 이유", required = true, example = "상품 구입")
        @JsonProperty("paymentReason")
        val paymentReason: String,
        @Schema(description = "Toss Payments 결제 키값", required = true, example = "qwer1234")
        @JsonProperty("paymentKey")
        val paymentKey: String,
        @Schema(description = "Toss Payments 주문 아이디", required = true, example = "qwer1234")
        @JsonProperty("orderId")
        val orderId: String
    )

    data class PostPgTossPaymentsRequestOutputVo(
        @Schema(description = "payment request 고유값", required = true, example = "1")
        @JsonProperty("paymentRequestUid")
        val paymentRequestUid: Long
    )


    // ----
    @Operation(
        summary = "PG 결제(Toss Payments) 환불 요청",
        description = "PG 결제(Toss Payments) 전체 환불 요청 API"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정상 동작"
            ),
            ApiResponse(
                responseCode = "204",
                content = [Content()],
                description = "Response Body 가 없습니다.<br>" +
                        "Response Headers 를 확인하세요.",
                headers = [
                    Header(
                        name = "api-result-code",
                        description = "(Response Code 반환 원인) - Required<br>" +
                                "1 : 정보가 없습니다.<br>" +
                                "2 : 완료되지 않은 결제입니다.<br>" +
                                "3 : 실패한 결제입니다.<br>" +
                                "4 : 환불 내역이 존재합니다.<br>" +
                                "5 : 가상 계좌 결제이지만 필수 refundReceiveAccountObj 가 null 입니다.<br>" +
                                "6 : Toss Payments 환불 API 호출 실패",
                        schema = Schema(type = "string")
                    )
                ]
            )
        ]
    )
    @PostMapping(
        path = ["/request/{paymentRequestUid}/pg-toss-payments-refund-all"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @ResponseBody
    fun postRequestPgTossPaymentsRefundAll(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(name = "paymentRequestUid", description = "결제 요청 정보 고유값", example = "1")
        @PathVariable("paymentRequestUid")
        paymentRequestUid: Long,
        @RequestBody
        inputVo: PostRequestPgTossPaymentsRefundAllInputVo
    ): PostRequestPgTossPaymentsRefundAllOutputVo? {
        return service.postRequestPgTossPaymentsRefundAll(
            httpServletResponse,
            paymentRequestUid,
            inputVo
        )
    }

    data class PostRequestPgTossPaymentsRefundAllInputVo(
        @Schema(description = "환불 이유", required = true, example = "상품 하자")
        @JsonProperty("refundReason")
        val refundReason: String,
        @Schema(description = "결제 취소 후 금액이 환불될 계좌의 정보(가상계좌 결제에만 필수)", required = false)
        @JsonProperty("refundReceiveAccountObj")
        val refundReceiveAccountObj: RefundReceiveAccount?
    ) {
        @Schema(description = "결제 취소 후 금액이 환불될 계좌의 정보(가상계좌 결제에만 필수)")
        data class RefundReceiveAccount(
            // 은행 코드 : https://docs.tosspayments.com/codes/org-codes#%EC%9D%80%ED%96%89-%EC%BD%94%EB%93%9C
            // 증권사 코드 : https://docs.tosspayments.com/codes/org-codes#%EC%A6%9D%EA%B6%8C%EC%82%AC-%EC%BD%94%EB%93%9C
            @Schema(
                description = "취소 금액을 환불받을 계좌의 은행 코드입니다.",
                required = true,
                example = "경남"
            )
            @JsonProperty("bank")
            val bank: String,
            @Schema(
                description = "취소 금액을 환불받을 계좌의 계좌번호입니다. - 없이 숫자만 넣어야 합니다. 최대 길이는 20자입니다.",
                required = true,
                example = "1123456789120"
            )
            @JsonProperty("accountNumber")
            val accountNumber: String,
            @Schema(
                description = "취소 금액을 환불받을 계좌의 예금주입니다. 최대 길이는 60자입니다.",
                required = true,
                example = "홍길동"
            )
            @JsonProperty("holderName")
            val holderName: String
        )
    }

    data class PostRequestPgTossPaymentsRefundAllOutputVo(
        @Schema(description = "payment refund 고유값", required = true, example = "1")
        @JsonProperty("paymentRefundUid")
        val paymentRefundUid: Long
    )


    // ----
    @Operation(
        summary = "PG 결제(Toss Payments) 부분 환불 요청",
        description = "PG 결제(Toss Payments) 부분 환불 요청 API"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정상 동작"
            ),
            ApiResponse(
                responseCode = "204",
                content = [Content()],
                description = "Response Body 가 없습니다.<br>" +
                        "Response Headers 를 확인하세요.",
                headers = [
                    Header(
                        name = "api-result-code",
                        description = "(Response Code 반환 원인) - Required<br>" +
                                "1 : 정보가 없습니다.<br>" +
                                "2 : 완료되지 않은 결제입니다.<br>" +
                                "3 : 실패한 결제입니다.<br>" +
                                "4 : 전액 환불 내역이 존재합니다.<br>" +
                                "5 : 가상 계좌 결제이지만 필수 refundReceiveAccountObj 가 null 입니다.<br>" +
                                "6 : Toss Payments 환불 API 호출 실패<br>" +
                                "7 : 환불 가능 금액을 넘어섰습니다.",
                        schema = Schema(type = "string")
                    )
                ]
            )
        ]
    )
    @PostMapping(
        path = ["/request/{paymentRequestUid}/pg-toss-payments-refund-part"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @ResponseBody
    fun postRequestPgTossPaymentsRefundPart(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(name = "paymentRequestUid", description = "결제 요청 정보 고유값", example = "1")
        @PathVariable("paymentRequestUid")
        paymentRequestUid: Long,
        @RequestBody
        inputVo: PostRequestPgTossPaymentsRefundPartInputVo
    ): PostRequestPgTossPaymentsRefundPartOutputVo? {
        return service.postRequestPgTossPaymentsRefundPart(
            httpServletResponse,
            paymentRequestUid,
            inputVo
        )
    }

    data class PostRequestPgTossPaymentsRefundPartInputVo(
        @Schema(description = "결제 금액(통화 코드는 KRW 로 간주합니다.)", required = true, example = "1000")
        @JsonProperty("prefundAmount")
        val refundAmount: Long,
        @Schema(description = "환불 이유", required = true, example = "상품 하자")
        @JsonProperty("refundReason")
        val refundReason: String,
        @Schema(description = "결제 취소 후 금액이 환불될 계좌의 정보(가상계좌 결제에만 필수)", required = false)
        @JsonProperty("refundReceiveAccountObj")
        val refundReceiveAccountObj: RefundReceiveAccount?
    ) {
        @Schema(description = "결제 취소 후 금액이 환불될 계좌의 정보(가상계좌 결제에만 필수)")
        data class RefundReceiveAccount(
            // 은행 코드 : https://docs.tosspayments.com/codes/org-codes#%EC%9D%80%ED%96%89-%EC%BD%94%EB%93%9C
            // 증권사 코드 : https://docs.tosspayments.com/codes/org-codes#%EC%A6%9D%EA%B6%8C%EC%82%AC-%EC%BD%94%EB%93%9C
            @Schema(
                description = "취소 금액을 환불받을 계좌의 은행 코드입니다.",
                required = true,
                example = "경남"
            )
            @JsonProperty("bank")
            val bank: String,
            @Schema(
                description = "취소 금액을 환불받을 계좌의 계좌번호입니다. - 없이 숫자만 넣어야 합니다. 최대 길이는 20자입니다.",
                required = true,
                example = "1123456789120"
            )
            @JsonProperty("accountNumber")
            val accountNumber: String,
            @Schema(
                description = "취소 금액을 환불받을 계좌의 예금주입니다. 최대 길이는 60자입니다.",
                required = true,
                example = "홍길동"
            )
            @JsonProperty("holderName")
            val holderName: String
        )
    }

    data class PostRequestPgTossPaymentsRefundPartOutputVo(
        @Schema(description = "payment refund 고유값", required = true, example = "1")
        @JsonProperty("paymentRefundUid")
        val paymentRefundUid: Long
    )

    // PG 결제 요청 billing pay 의 경우는 별도 결제 타입, 별도 테이블, 별도 api 를 추가하세요.
}