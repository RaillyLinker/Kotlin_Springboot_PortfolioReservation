package com.raillylinker.controllers

import com.fasterxml.jackson.annotation.JsonProperty
import com.raillylinker.services.RentalReservationService
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

@Tag(name = "/rental-reservation APIs", description = "대여 예약 서비스 API 컨트롤러")
@Controller
@RequestMapping("/rental-reservation")
class RentalReservationController(
    private val service: RentalReservationService
) {
    // <멤버 변수 공간>


    // ---------------------------------------------------------------------------------------------
    // <매핑 함수 공간>
    // 결제 금액 할인 등의 정보에 대한 처리는 이곳의 이 시점에 실행하여 예약 정보의 결제 금액에 적용하면 됩니다.
    @Operation(
        summary = "상품 예약 신청하기 <>",
        description = "상품에 대한 예약 신청을 합니다."
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
                                "1 : rentalProductUid 에 해당하는 정보가 데이터베이스에 존재하지 않습니다.<br>" +
                                "2 : 예약 상품 버전 시퀀스가 일치하지 않습니다.<br>" +
                                "3 : 예약 상품이 현재 예약 가능 상태가 아닙니다.<br>" +
                                "4 : 상품 예약 가능 일시 이전입니다.<br>" +
                                "5 : rentalUnitCount 가 대여 단위 예약 최소 횟수보다 작습니다.<br>" +
                                "6 : rentalUnitCount 가 대여 단위 예약 최대 횟수보다 큽니다.<br>" +
                                "7 : rentalUnitCount 와 rentalEndDatetime 가 일치하지 않습니다.<br>" +
                                "8 : 예약 상품 대여 가능 최초 일시가 대여 시작일보다 큽니다.<br>" +
                                "9 : 예약 상품 대여 가능 마지막 일시가 대여 마지막일보다 작습니다.<br>" +
                                "10 : 예약 상품이 현재 예약 중입니다.<br>" +
                                "11 : 대여 시작 일시가 현재 일시보다 작습니다.<br>" +
                                "12 : 대여 단위 예약 횟수가 음수면 안됩니다.",
                        schema = Schema(type = "string")
                    )
                ]
            ),
            ApiResponse(
                responseCode = "401",
                content = [Content()],
                description = "인증되지 않은 접근입니다."
            )
        ]
    )
    @PostMapping(
        path = ["/product-reservation"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @PreAuthorize("isAuthenticated()")
    @ResponseBody
    fun postProductReservation(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(hidden = true)
        @RequestHeader("Authorization")
        authorization: String?,
        @RequestBody
        inputVo: PostProductReservationInputVo
    ): PostProductReservationOutputVo? {
        return service.postProductReservation(
            httpServletResponse,
            authorization!!,
            inputVo
        )
    }

    data class PostProductReservationInputVo(
        @Schema(description = "예약 상품 고유번호", required = true, example = "1")
        @JsonProperty("rentalProductUid")
        val rentalProductUid: Long,
        @Schema(description = "예약 상품 버전 시퀀스(현재 상품 테이블 버전과 맞지 않는다면 진행 불가)", required = true, example = "1")
        @JsonProperty("rentableProductVersionSeq")
        val rentableProductVersionSeq: Long,
        @Schema(
            description = "대여 시작 일시(yyyy_MM_dd_'T'_HH_mm_ss_z)",
            required = true,
            example = "2024_05_02_T_15_14_49_KST"
        )
        @JsonProperty("rentalStartDatetime")
        val rentalStartDatetime: String,
        @Schema(
            description = "대여 단위 예약의 횟수(상품 정보의 단위 예약 시간과 곱하여 대여 끝 일시를 구할 것입니다.)",
            required = true,
            example = "2"
        )
        @JsonProperty("rentalUnitCount")
        val rentalUnitCount: Long,
        @Schema(
            description = "계산한 대여 마지막 일시(yyyy_MM_dd_'T'_HH_mm_ss_z)",
            required = true,
            example = "2024_05_02_T_15_14_49_KST"
        )
        @JsonProperty("rentalEndDatetime")
        val rentalEndDatetime: String
    )

    data class PostProductReservationOutputVo(
        @Schema(description = "rentalProductReservation 고유값", required = true, example = "1")
        @JsonProperty("rentalProductReservationUid")
        val rentalProductReservationUid: Long,
        @Schema(
            description = "고객 결제 기한 일시(yyyy_MM_dd_'T'_HH_mm_ss_z)",
            required = true,
            example = "2024_05_02_T_15_14_49_KST"
        )
        @JsonProperty("paymentDeadlineDatetime")
        val paymentDeadlineDatetime: String,
        @Schema(
            description = "취소 가능 기한 일시(yyyy_MM_dd_'T'_HH_mm_ss_z)",
            required = true,
            example = "2024_05_02_T_15_14_49_KST"
        )
        @JsonProperty("cancelableDeadlineDatetime")
        val cancelableDeadlineDatetime: String
    )


    @Operation(
        summary = "사용자 결제 처리 <>",
        description = "예약 신청 정보에 대하여 사용자 결제 처리를 합니다."
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
                                "1 : rentalProductReservationUid 에 해당하는 정보가 데이터베이스에 존재하지 않습니다.<br>" +
                                "2 : paymentRequestUid 에 해당하는 정보가 데이터베이스에 존재하지 않습니다.<br>" +
                                "3 : 이미 결제 처리되었습니다.<br>" +
                                "4 : 결제 요구 금액과 결제 완료된 금액이 다릅니다.",
                        schema = Schema(type = "string")
                    )
                ]
            ),
            ApiResponse(
                responseCode = "401",
                content = [Content()],
                description = "인증되지 않은 접근입니다."
            )
        ]
    )
    @PostMapping(
        path = ["/product-reservation-payment"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @PreAuthorize("isAuthenticated()")
    @ResponseBody
    fun postProductReservationPayment(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(hidden = true)
        @RequestHeader("Authorization")
        authorization: String?,
        @RequestBody
        inputVo: PostProductReservationPaymentInputVo
    ): PostProductReservationPaymentOutputVo? {
        return service.postProductReservationPayment(
            httpServletResponse,
            authorization!!,
            inputVo
        )
    }

    data class PostProductReservationPaymentInputVo(
        @Schema(description = "rentalProductReservation 고유값", required = true, example = "1")
        @JsonProperty("rentalProductReservationUid")
        val rentalProductReservationUid: Long,
        @Schema(description = "paymentRequest 고유값(무료 결제일 경우)", required = false, example = "1")
        @JsonProperty("paymentRequestUid")
        val paymentRequestUid: Long?
    )

    data class PostProductReservationPaymentOutputVo(
        @Schema(description = "rentalProductReservationPayment 고유값", required = true, example = "1")
        @JsonProperty("rentalProductReservationPaymentUid")
        val rentalProductReservationPaymentUid: Long
    )


    // ----
    @Operation(
        summary = "예약 취소 신청 <>",
        description = "예약 취소 신청을 합니다.<br>" +
                "결제 확인 및 예약 신청 승인 처리가 전부 완료되기 전이라면 자동적으로 예약 취소 승인 처리가 됩니다."
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
                                "1 : rentalProductReservationUid 에 해당하는 정보가 데이터베이스에 존재하지 않습니다.<br>" +
                                "2 : 예약 취소 가능 기한 초과<br>" +
                                "3 : 예약 취소 승인 상태<br>" +
                                "4 : 예약 신청 거부 상태<br>" +
                                "5 : 미결제 상태 & 결제 기한 초과 상태(= 취소와 동일)<br>" +
                                "6 : 예약 취소 신청 상태",
                        schema = Schema(type = "string")
                    )
                ]
            ),
            ApiResponse(
                responseCode = "401",
                content = [Content()],
                description = "인증되지 않은 접근입니다."
            )
        ]
    )
    @PostMapping(
        path = ["/rental-product-reservation/{rentalProductReservationUid}/cancel-request"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @PreAuthorize("isAuthenticated()")
    @ResponseBody
    fun postCancelProductReservation(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(hidden = true)
        @RequestHeader("Authorization")
        authorization: String?,
        @Parameter(
            name = "rentalProductReservationUid",
            description = "rentableProductReservationInfo 고유값",
            example = "1"
        )
        @PathVariable("rentalProductReservationUid")
        rentalProductReservationUid: Long,
        @RequestBody
        inputVo: PostCancelProductReservationInputVo
    ): PostCancelProductReservationOutputVo? {
        return service.postCancelProductReservation(
            httpServletResponse,
            authorization!!,
            rentalProductReservationUid,
            inputVo
        )
    }

    data class PostCancelProductReservationInputVo(
        @Schema(description = "예약 취소 사유", required = true, example = "개인 사유")
        @JsonProperty("cancelReason")
        val cancelReason: String
    )

    data class PostCancelProductReservationOutputVo(
        @Schema(description = "reservationHistory 고유값", required = true, example = "1")
        @JsonProperty("reservationHistoryUid")
        val reservationHistoryUid: Long,
        @Schema(
            description = "예약 취소 즉시 승인시 reservationHistory 고유값(null 이라면 즉시 취소 승인이 아닙니다.)",
            required = false,
            example = "1"
        )
        @JsonProperty("reservationHistoryUidForApproved")
        val reservationHistoryUidForApproved: Long?,
    )


    // ----
    @Operation(
        summary = "예약 취소 신청 철회 <>",
        description = "예약 취소 신청을 철회 합니다."
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
                                "1 : rentalProductReservationUid 에 해당하는 정보가 데이터베이스에 존재하지 않습니다.<br>" +
                                "2 : 예약 취소 승인 상태<br>" +
                                "3 : 예약 신청 거부 상태<br>" +
                                "4 : 미결제 상태 & 결제 기한 초과 상태(= 취소와 동일)<br>" +
                                "5 : 예약 취소 신청 상태가 없습니다.",
                        schema = Schema(type = "string")
                    )
                ]
            ),
            ApiResponse(
                responseCode = "401",
                content = [Content()],
                description = "인증되지 않은 접근입니다."
            )
        ]
    )
    @PostMapping(
        path = ["/rental-product-reservation/{rentalProductReservationUid}/cancel-request-cancel"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @PreAuthorize("isAuthenticated()")
    @ResponseBody
    fun postCancelProductReservationCancel(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(hidden = true)
        @RequestHeader("Authorization")
        authorization: String?,
        @Parameter(
            name = "rentalProductReservationUid",
            description = "rentableProductReservationInfo 고유값",
            example = "1"
        )
        @PathVariable("rentalProductReservationUid")
        rentalProductReservationUid: Long
    ): PostCancelProductReservationCancelOutputVo? {
        return service.postCancelProductReservationCancel(
            httpServletResponse,
            authorization!!,
            rentalProductReservationUid
        )
    }

    data class PostCancelProductReservationCancelOutputVo(
        @Schema(description = "reservationHistory 고유값", required = true, example = "1")
        @JsonProperty("reservationHistoryUid")
        val reservationHistoryUid: Long
    )


    // ----
    @Operation(
        summary = "개별 상품 조기 반납 신고 <>",
        description = "개별 상품에 대해 조기 반납 신고 처리를 합니다."
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
                                "1 : rentalProductReservationUid 에 해당하는 정보가 데이터베이스에 존재하지 않습니다.<br>" +
                                "2 : 결제 확인 완료 아님 || 예약 신청 거부 = 대여 진행 상태가 아님<br>" +
                                "3 : 상품 대여일이 도래하지 않았습니다.<br>" +
                                "4 : 상품 반납일이 도래하였습니다.<br>" +
                                "5 : 개별 상품 반납 확인이 되었습니다.<br>" +
                                "6 : 개별 상품 조기 반납 신고 된 상태입니다.",
                        schema = Schema(type = "string")
                    )
                ]
            ),
            ApiResponse(
                responseCode = "401",
                content = [Content()],
                description = "인증되지 않은 접근입니다."
            )
        ]
    )
    @PostMapping(
        path = ["/rental-product-reservation/{rentalProductReservationUid}/early-return"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @PreAuthorize("isAuthenticated()")
    @ResponseBody
    fun postRentableProductStockReservationInfoEarlyReturn(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(hidden = true)
        @RequestHeader("Authorization")
        authorization: String?,
        @Parameter(
            name = "rentalProductReservationUid",
            description = "rentalProductReservation 고유값",
            example = "1"
        )
        @PathVariable("rentalProductReservationUid")
        rentalProductReservationUid: Long,
        @RequestBody
        inputVo: PostRentableProductStockReservationInfoEarlyReturnInputVo
    ): PostRentableProductStockReservationInfoEarlyReturnOutputVo? {
        return service.postRentableProductStockReservationInfoEarlyReturn(
            httpServletResponse,
            authorization!!,
            rentalProductReservationUid,
            inputVo
        )
    }

    data class PostRentableProductStockReservationInfoEarlyReturnInputVo(
        @Schema(description = "상태 변경 상세 설명", required = true, example = "이상무")
        @JsonProperty("stateChangeDesc")
        val stateChangeDesc: String
    )

    data class PostRentableProductStockReservationInfoEarlyReturnOutputVo(
        @Schema(description = "reservationHistory 고유값", required = true, example = "1")
        @JsonProperty("reservationHistoryUid")
        val reservationHistoryUid: Long
    )


    // ----
    @Operation(
        summary = "개별 상품 조기 반납 신고 취소 <>",
        description = "개별 상품에 대해 조기 반납 신고 취소 처리를 합니다."
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
                                "1 : rentalProductReservationUid 에 해당하는 정보가 데이터베이스에 존재하지 않습니다.<br>" +
                                "2 : 개별 상품 조기 반납 신고 내역이 없습니다.<br>" +
                                "3 : 개별 상품 조기 반납 신고 취소 상태입니다.",
                        schema = Schema(type = "string")
                    )
                ]
            ),
            ApiResponse(
                responseCode = "401",
                content = [Content()],
                description = "인증되지 않은 접근입니다."
            )
        ]
    )
    @PostMapping(
        path = ["/rental-product-reservation/{rentalProductReservationUid}/early-return-cancel"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @PreAuthorize("isAuthenticated()")
    @ResponseBody
    fun postRentableProductStockReservationInfoEarlyReturnCancel(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(hidden = true)
        @RequestHeader("Authorization")
        authorization: String?,
        @Parameter(
            name = "rentalProductReservationUid",
            description = "rentalProductReservation 고유값",
            example = "1"
        )
        @PathVariable("rentalProductReservationUid")
        rentalProductReservationUid: Long,
        @RequestBody
        inputVo: PostRentableProductStockReservationInfoEarlyReturnCancelInputVo
    ): PostRentableProductStockReservationInfoEarlyReturnCancelOutputVo? {
        return service.postRentableProductStockReservationInfoEarlyReturnCancel(
            httpServletResponse,
            authorization!!,
            rentalProductReservationUid,
            inputVo
        )
    }

    data class PostRentableProductStockReservationInfoEarlyReturnCancelInputVo(
        @Schema(description = "상태 변경 상세 설명", required = true, example = "이상무")
        @JsonProperty("stateChangeDesc")
        val stateChangeDesc: String
    )

    data class PostRentableProductStockReservationInfoEarlyReturnCancelOutputVo(
        @Schema(description = "reservationHistory 고유값", required = true, example = "1")
        @JsonProperty("reservationHistoryUid")
        val reservationHistoryUid: Long
    )


    // ----
    @Operation(
        summary = "예약 연장 신청 <>",
        description = "예약 연장 신청을 합니다."
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
                                "1 : rentalProductReservationUid 에 해당하는 정보가 데이터베이스에 존재하지 않습니다.<br>" +
                                "2 : 결제 확인 완료 아님 || 예약 신청 거부 = 대여 진행 상태가 아님<br>" +
                                "3 : 상품 대여일이 도래하지 않았습니다.<br>" +
                                "4 : 상품 반납을 확인하였습니다.<br>" +
                                "5 : 상품 조기 반납 신고 된 상태입니다.<br>" +
                                "6 : 예약 연장 신청 상태입니다.<br>" +
                                "7 : rentalEndDatetime 가 기존 시간보다 작습니다.",
                        schema = Schema(type = "string")
                    )
                ]
            ),
            ApiResponse(
                responseCode = "401",
                content = [Content()],
                description = "인증되지 않은 접근입니다."
            )
        ]
    )
    @PostMapping(
        path = ["/rental-product-reservation/{rentalProductReservationUid}/rental-extend"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @PreAuthorize("isAuthenticated()")
    @ResponseBody
    fun postRentableProductStockReservationInfoRentalExtend(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(hidden = true)
        @RequestHeader("Authorization")
        authorization: String?,
        @Parameter(
            name = "rentalProductReservationUid",
            description = "rentalProductReservation 고유값",
            example = "1"
        )
        @PathVariable("rentalProductReservationUid")
        rentalProductReservationUid: Long,
        @RequestBody
        inputVo: PostRentableProductStockReservationInfoRentalExtendInputVo
    ): PostRentableProductStockReservationInfoRentalExtendOutputVo? {
        return service.postRentableProductStockReservationInfoRentalExtend(
            httpServletResponse,
            authorization!!,
            rentalProductReservationUid,
            inputVo
        )
    }

    data class PostRentableProductStockReservationInfoRentalExtendInputVo(
        @Schema(description = "상태 변경 상세 설명", required = true, example = "이상무")
        @JsonProperty("stateChangeDesc")
        val stateChangeDesc: String,
        @Schema(
            description = "연장하려는 대여 마지막 일시(yyyy_MM_dd_'T'_HH_mm_ss_z)",
            required = true,
            example = "2024_05_02_T_15_14_49_KST"
        )
        @JsonProperty("rentalEndDatetime")
        val rentalEndDatetime: String
    )

    data class PostRentableProductStockReservationInfoRentalExtendOutputVo(
        @Schema(description = "reservationHistory 고유값", required = true, example = "1")
        @JsonProperty("reservationHistoryUid")
        val reservationHistoryUid: Long
    )


    // ----
    @Operation(
        summary = "예약 연장 신청 취소 <>",
        description = "예약 연장 신청을 취소 합니다."
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
                                "1 : rentalProductReservationUid 에 해당하는 정보가 데이터베이스에 존재하지 않습니다.<br>" +
                                "2 : 결제 확인 완료 아님 || 예약 신청 거부 = 대여 진행 상태가 아님<br>" +
                                "3 : 상품 대여일이 도래하지 않았습니다.<br>" +
                                "4 : 예약 연장 신청 상태가 아닙니다.<br>" +
                                "5 : 이미 반납 확인된 상태입니다.",
                        schema = Schema(type = "string")
                    )
                ]
            ),
            ApiResponse(
                responseCode = "401",
                content = [Content()],
                description = "인증되지 않은 접근입니다."
            )
        ]
    )
    @PostMapping(
        path = ["/rental-product-reservation/{rentalProductReservationUid}/rental-extend-cancel"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @PreAuthorize("isAuthenticated()")
    @ResponseBody
    fun postRentableProductStockReservationInfoRentalExtendCancel(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(hidden = true)
        @RequestHeader("Authorization")
        authorization: String?,
        @Parameter(
            name = "rentalProductReservationUid",
            description = "rentalProductReservation 고유값",
            example = "1"
        )
        @PathVariable("rentalProductReservationUid")
        rentalProductReservationUid: Long,
        @RequestBody
        inputVo: PostRentableProductStockReservationInfoRentalExtendCancelInputVo
    ): PostRentableProductStockReservationInfoRentalExtendCancelOutputVo? {
        return service.postRentableProductStockReservationInfoRentalExtendCancel(
            httpServletResponse,
            authorization!!,
            rentalProductReservationUid,
            inputVo
        )
    }

    data class PostRentableProductStockReservationInfoRentalExtendCancelInputVo(
        @Schema(description = "상태 변경 상세 설명", required = true, example = "이상무")
        @JsonProperty("stateChangeDesc")
        val stateChangeDesc: String
    )

    data class PostRentableProductStockReservationInfoRentalExtendCancelOutputVo(
        @Schema(description = "reservationHistory 고유값", required = true, example = "1")
        @JsonProperty("reservationHistoryUid")
        val reservationHistoryUid: Long
    )

    // 정보 조회 API 는 화면 기획이 나오는 시점에 추가
}