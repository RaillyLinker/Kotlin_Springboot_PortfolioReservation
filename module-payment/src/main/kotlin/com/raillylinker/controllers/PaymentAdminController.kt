package com.raillylinker.controllers

import com.fasterxml.jackson.annotation.JsonProperty
import com.raillylinker.services.PaymentAdminService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.headers.Header
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*


@Tag(name = "/payment-admin APIs", description = "결제 API 컨트롤러")
@Controller
@RequestMapping("/payment-admin")
class PaymentAdminController(
    private val service: PaymentAdminService
) {
    // <멤버 변수 공간>


    // ---------------------------------------------------------------------------------------------
    // <매핑 함수 공간>
    @Operation(
        summary = "결제 실패 처리 <'ADMIN'>",
        description = "결제 실패 처리<br>" +
                "(완료 처리가 되기 전에만 가능합니다.<br>" +
                "완료 처리가 된 이후에는 고객에게 완료 되었다는 정보가 전달되므로<br>" +
                "완료 처리 자체를 신중하게 고려하세요.)"
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
                                "1 : paymentRequestUid 에 해당하는 정보가 없습니다.<br>" +
                                "2 : 결제 실패 처리 된 정보입니다.<br>" +
                                "3 : 결제 완료 처리 된 정보입니다.",
                        schema = Schema(type = "string")
                    )
                ]
            ),
            ApiResponse(
                responseCode = "401",
                content = [Content()],
                description = "인증되지 않은 접근입니다."
            ),
            ApiResponse(
                responseCode = "403",
                content = [Content()],
                description = "인가되지 않은 접근입니다."
            )
        ]
    )
    @PutMapping(
        path = ["/payment-request/{paymentRequestUid}/fail"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.ALL_VALUE]
    )
    @PreAuthorize("isAuthenticated() and (hasRole('ROLE_ADMIN'))")
    @ResponseBody
    fun putPaymentRequestFail(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(hidden = true)
        @RequestHeader("Authorization")
        authorization: String?,
        @Parameter(name = "paymentRequestUid", description = "결제 요청 정보 고유값", example = "1")
        @PathVariable("paymentRequestUid")
        paymentRequestUid: Long,
        @RequestBody
        inputVo: PutPaymentRequestFailInputVo
    ) {
        service.putPaymentRequestFail(httpServletResponse, authorization!!, paymentRequestUid, inputVo)
    }

    data class PutPaymentRequestFailInputVo(
        @Schema(description = "결제 실패 이유", required = true, example = "기한 초과로 인한 실패")
        @JsonProperty("paymentFailReason")
        val paymentFailReason: String
    )


    // ----
    @Operation(
        summary = "결제 완료 처리 <'ADMIN'>",
        description = "결제 완료 처리"
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
                                "1 : paymentRequestUid 에 해당하는 정보가 없습니다.<br>" +
                                "2 : 결제 실패 처리 된 정보입니다.<br>" +
                                "3 : 결제 완료 처리 된 정보입니다.",
                        schema = Schema(type = "string")
                    )
                ]
            ),
            ApiResponse(
                responseCode = "401",
                content = [Content()],
                description = "인증되지 않은 접근입니다."
            ),
            ApiResponse(
                responseCode = "403",
                content = [Content()],
                description = "인가되지 않은 접근입니다."
            )
        ]
    )
    @PutMapping(
        path = ["/payment-request/{paymentRequestUid}/complete"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.ALL_VALUE]
    )
    @PreAuthorize("isAuthenticated() and (hasRole('ROLE_ADMIN'))")
    @ResponseBody
    fun putPaymentRequestComplete(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(hidden = true)
        @RequestHeader("Authorization")
        authorization: String?,
        @Parameter(name = "paymentRequestUid", description = "결제 요청 정보 고유값", example = "1")
        @PathVariable("paymentRequestUid")
        paymentRequestUid: Long
    ) {
        service.putPaymentRequestComplete(httpServletResponse, authorization!!, paymentRequestUid)
    }


    // ----
    @Operation(
        summary = "환불 거부 처리 <'ADMIN'>",
        description = "환불 거부 처리<br>" +
                "(완료 처리가 되기 전에만 가능합니다.)"
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
                                "1 : paymentRefundRequestUid 에 해당하는 정보가 없습니다.<br>" +
                                "2 : 환불 거부 처리 된 정보입니다.<br>" +
                                "3 : 환불 완료 처리 된 정보입니다.",
                        schema = Schema(type = "string")
                    )
                ]
            ),
            ApiResponse(
                responseCode = "401",
                content = [Content()],
                description = "인증되지 않은 접근입니다."
            ),
            ApiResponse(
                responseCode = "403",
                content = [Content()],
                description = "인가되지 않은 접근입니다."
            )
        ]
    )
    @PutMapping(
        path = ["/payment-refund-request/{paymentRefundRequestUid}/reject"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.ALL_VALUE]
    )
    @PreAuthorize("isAuthenticated() and (hasRole('ROLE_ADMIN'))")
    @ResponseBody
    fun putPaymentRefundRequestReject(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(hidden = true)
        @RequestHeader("Authorization")
        authorization: String?,
        @Parameter(name = "paymentRefundRequestUid", description = "결제 요청 정보 고유값", example = "1")
        @PathVariable("paymentRefundRequestUid")
        paymentRefundRequestUid: Long,
        @RequestBody
        inputVo: PutPaymentRefundRequestRejectInputVo
    ) {
        service.putPaymentRefundRequestReject(httpServletResponse, authorization!!, paymentRefundRequestUid, inputVo)
    }

    data class PutPaymentRefundRequestRejectInputVo(
        @Schema(description = "환불 거부 이유", required = true, example = "지급 불가 사유로 인한 거부")
        @JsonProperty("paymentRefundRejectReason")
        val paymentRefundRejectReason: String
    )


    // ----
    @Operation(
        summary = "환불 완료 처리 <'ADMIN'>",
        description = "환불 완료 처리"
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
                                "1 : paymentRefundRequestUid 에 해당하는 정보가 없습니다.<br>" +
                                "2 : 환불 실패 처리 된 정보입니다.<br>" +
                                "3 : 환불 완료 처리 된 정보입니다.",
                        schema = Schema(type = "string")
                    )
                ]
            ),
            ApiResponse(
                responseCode = "401",
                content = [Content()],
                description = "인증되지 않은 접근입니다."
            ),
            ApiResponse(
                responseCode = "403",
                content = [Content()],
                description = "인가되지 않은 접근입니다."
            )
        ]
    )
    @PutMapping(
        path = ["/payment-refund-request/{paymentRefundRequestUid}/complete"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.ALL_VALUE]
    )
    @PreAuthorize("isAuthenticated() and (hasRole('ROLE_ADMIN'))")
    @ResponseBody
    fun putPaymentRefundRequestComplete(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(hidden = true)
        @RequestHeader("Authorization")
        authorization: String?,
        @Parameter(name = "paymentRefundRequestUid", description = "환불 요청 정보 고유값", example = "1")
        @PathVariable("paymentRefundRequestUid")
        paymentRefundRequestUid: Long
    ) {
        service.putPaymentRefundRequestComplete(httpServletResponse, authorization!!, paymentRefundRequestUid)
    }
}