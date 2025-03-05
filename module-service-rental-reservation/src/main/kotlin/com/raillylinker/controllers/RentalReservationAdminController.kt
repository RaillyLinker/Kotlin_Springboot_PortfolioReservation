package com.raillylinker.controllers

import com.fasterxml.jackson.annotation.JsonProperty
import com.raillylinker.services.RentalReservationAdminService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.headers.Header
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.io.Resource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.math.BigDecimal


@Tag(name = "/rental-reservation-admin APIs", description = "대여 예약 서비스 관리자 API 컨트롤러")
@Controller
@RequestMapping("/rental-reservation-admin")
class RentalReservationAdminController(
    private val service: RentalReservationAdminService
) {
    // <멤버 변수 공간>


    // ---------------------------------------------------------------------------------------------
    // <매핑 함수 공간>
    @Operation(
        summary = "예약 상품 등록 <ADMIN>",
        description = "예약 상품 정보를 등록합니다."
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
                                "1 : 최소 예약 횟수는 최대 예약 횟수보다 작거나 같아야 합니다.<br>" +
                                "2 : 결제 통보 기한 설정이 결제 승인 기한 설정보다 크면 안됩니다.<br>" +
                                "3 : 결제 승인 기한 설정이 예약 승인 기한 설정보다 크면 안됩니다.",
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
    @PostMapping(
        path = ["/rental-product"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @PreAuthorize("isAuthenticated() and (hasRole('ROLE_ADMIN'))")
    @ResponseBody
    fun postRentalProduct(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(hidden = true)
        @RequestHeader("Authorization")
        authorization: String?,
        @RequestBody
        inputVo: PostRentalProductInputVo
    ): PostRentalProductOutputVo? {
        return service.postRentalProduct(httpServletResponse, authorization!!, inputVo)
    }

    data class PostRentalProductInputVo(
        @Schema(description = "고객에게 보일 상품명", required = true, example = "testString")
        @JsonProperty("productName")
        val productName: String,
        @Schema(
            description = "고객에게 보일 상품 소개",
            required = true,
            example = "예약해주세요."
        )
        @JsonProperty("productIntro")
        val productIntro: String,
        @Schema(
            description = "상품이 위치한 주소(대여 가능 위치의 기준으로 사용됨) - 국가",
            required = true,
            example = "대한민국"
        )
        @JsonProperty("addressCountry")
        val addressCountry: String,
        @Schema(
            description = "상품이 위치한 주소(대여 가능 위치의 기준으로 사용됨) - 국가와 상세 주소를 제외",
            required = true,
            example = "서울시 은평구 불광동 미래혁신센터"
        )
        @JsonProperty("addressMain")
        val addressMain: String,
        @Schema(
            description = "상품이 위치한 주소(대여 가능 위치의 기준으로 사용됨) - 상세",
            required = true,
            example = "200 동 109 호"
        )
        @JsonProperty("addressDetail")
        val addressDetail: String,
        @Schema(
            description = "상품 예약이 가능한 최초 일시(콘서트 티켓 예매 선공개 기능을 가정)(yyyy_MM_dd_'T'_HH_mm_ss_z)",
            required = true,
            example = "2024_05_02_T_15_14_49_KST"
        )
        @JsonProperty("firstReservableDatetime")
        val firstReservableDatetime: String,
        @Schema(
            description = "상품 대여가 가능한 최초 일시(yyyy_MM_dd_'T'_HH_mm_ss_z)",
            required = true,
            example = "2024_05_02_T_15_14_49_KST"
        )
        @JsonProperty("firstRentalDatetime")
        val firstRentalDatetime: String,
        @Schema(
            description = "상품 대여가 가능한 마지막 일시(null 이라면 제한 없음)(yyyy_MM_dd_'T'_HH_mm_ss_z)",
            required = false,
            example = "2024_05_02_T_15_14_49_KST"
        )
        @JsonProperty("lastRentalDatetime")
        val lastRentalDatetime: String?,
        @Schema(
            description = "예약 추가 할 수 있는 최소 시간 단위 (분)",
            required = true,
            example = "60"
        )
        @JsonProperty("reservationUnitMinute")
        val reservationUnitMinute: Long,
        @Schema(
            description = "단위 예약 시간을 대여일 기준에서 최소 몇번 추가 해야 하는지",
            required = true,
            example = "1"
        )
        @JsonProperty("minimumReservationUnitCount")
        val minimumReservationUnitCount: Long,
        @Schema(
            description = "단위 예약 시간을 대여일 기준에서 최대 몇번 추가 가능한지 (Null 이라면 제한 없음)",
            required = false,
            example = "3"
        )
        @JsonProperty("maximumReservationUnitCount")
        val maximumReservationUnitCount: Long?,
        @Schema(
            description = "단위 예약 시간에 대한 가격 (예약 시간 / 단위 예약 시간 * 예약 단가 = 예약 최종가)",
            required = true,
            example = "10000"
        )
        @JsonProperty("reservationUnitPrice")
        val reservationUnitPrice: BigDecimal,
        @Schema(
            description = "단위 예약 시간에 대한 가격 통화 코드(IOS 4217)",
            required = true,
            example = "KRW"
        )
        @JsonProperty("reservationUnitPriceCurrencyCode")
        val reservationUnitPriceCurrencyCode: CurrencyCodeEnum,
        @Schema(
            description = "예약 가능 설정 (재고, 상품 상태와 상관 없이 현 시점 예약 가능한지에 대한 관리자의 설정)",
            required = true,
            example = "true"
        )
        @JsonProperty("nowReservable")
        val nowReservable: Boolean,
        @Schema(
            description = "고객에게 이때까지 결제를 해야 한다고 통보하는 기한 설정값(예약일로부터 +N 분)",
            required = true,
            example = "30"
        )
        @JsonProperty("customerPaymentDeadlineMinute")
        val customerPaymentDeadlineMinute: Long,
        @Schema(
            description = "관리자의 결제 확인 기한 설정값(예약일로 부터 +N 분, 고객 결제 기한 설정값보다 크거나 같음)",
            required = true,
            example = "30"
        )
        @JsonProperty("paymentCheckDeadlineMinute")
        val paymentCheckDeadlineMinute: Long,
        @Schema(
            description = "관리자의 예약 승인 기한 설정값(예약일로부터 +N분, 결제 확인 기한 설정값보다 크거나 같음)",
            required = true,
            example = "30"
        )
        @JsonProperty("approvalDeadlineMinute")
        val approvalDeadlineMinute: Long,
        @Schema(
            description = "고객이 예약 취소 가능한 기한 설정값(대여 시작일로부터 -N분으로 계산됨)",
            required = true,
            example = "30"
        )
        @JsonProperty("cancelDeadlineMinute")
        val cancelDeadlineMinute: Long,
        @Schema(
            description = "상품 상태 설명(예를 들어 손망실의 경우 now_reservable 이 false 이며, 이곳에 손망실 이유가 기재됩니다.)",
            required = true,
            example = "이상무"
        )
        @JsonProperty("productStateDesc")
        val productStateDesc: String
    ) {
        enum class CurrencyCodeEnum {
            KRW, USD
        }
    }

    data class PostRentalProductOutputVo(
        @Schema(description = "rentalProduct 고유값", required = true, example = "1")
        @JsonProperty("rentalProductUid")
        val rentalProductUid: Long
    )


    // ----
    @Operation(
        summary = "대여 가능 상품 수정 <ADMIN>",
        description = "대여 상품 정보를 수정합니다.<br>" +
                "상품 수정시 update_version_seq 가 1 증가하며, 예약 요청시 고객이 보내온 update_version 이 일치하지 않는다면 진행되지 않습니다."
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
                                "2 : 최소 예약 횟수는 최대 예약 횟수보다 작거나 같아야 합니다.<br>" +
                                "3 : 결제 통보 기한 설정이 결제 승인 기한 설정보다 크면 안됩니다.<br>" +
                                "4 : 결제 승인 기한 설정이 예약 승인 기한 설정보다 크면 안됩니다.",
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
        path = ["/rental-product/{rentalProductUid}"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.ALL_VALUE]
    )
    @PreAuthorize("isAuthenticated() and (hasRole('ROLE_ADMIN'))")
    @ResponseBody
    fun putRentalProduct(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(hidden = true)
        @RequestHeader("Authorization")
        authorization: String?,
        @Parameter(name = "rentalProductUid", description = "rentalProduct 고유값", example = "1")
        @PathVariable("rentalProductUid")
        rentalProductUid: Long,
        @RequestBody
        inputVo: PutRentalProductInputVo
    ) {
        service.putRentalProduct(httpServletResponse, authorization!!, rentalProductUid, inputVo)
    }

    data class PutRentalProductInputVo(
        @Schema(description = "고객에게 보일 상품명", required = true, example = "testString")
        @JsonProperty("productName")
        val productName: String,
        @Schema(
            description = "고객에게 보일 상품 소개",
            required = true,
            example = "예약해주세요."
        )
        @JsonProperty("productIntro")
        val productIntro: String,
        @Schema(
            description = "상품이 위치한 주소(대여 가능 위치의 기준으로 사용됨) - 국가",
            required = true,
            example = "대한민국"
        )
        @JsonProperty("addressCountry")
        val addressCountry: String,
        @Schema(
            description = "상품이 위치한 주소(대여 가능 위치의 기준으로 사용됨) - 국가와 상세 주소를 제외",
            required = true,
            example = "서울시 은평구 불광동 미래혁신센터"
        )
        @JsonProperty("addressMain")
        val addressMain: String,
        @Schema(
            description = "상품이 위치한 주소(대여 가능 위치의 기준으로 사용됨) - 상세",
            required = true,
            example = "200 동 109 호"
        )
        @JsonProperty("addressDetail")
        val addressDetail: String,
        @Schema(
            description = "상품 예약이 가능한 최초 일시(콘서트 티켓 예매 선공개 기능을 가정)(yyyy_MM_dd_'T'_HH_mm_ss_z)",
            required = true,
            example = "2024_05_02_T_15_14_49_KST"
        )
        @JsonProperty("firstReservableDatetime")
        val firstReservableDatetime: String,
        @Schema(
            description = "상품 대여가 가능한 최초 일시(yyyy_MM_dd_'T'_HH_mm_ss_z)",
            required = true,
            example = "2024_05_02_T_15_14_49_KST"
        )
        @JsonProperty("firstRentalDatetime")
        val firstRentalDatetime: String,
        @Schema(
            description = "상품 대여가 가능한 마지막 일시(null 이라면 제한 없음)(yyyy_MM_dd_'T'_HH_mm_ss_z)",
            required = false,
            example = "2024_05_02_T_15_14_49_KST"
        )
        @JsonProperty("lastRentalDatetime")
        val lastRentalDatetime: String?,
        @Schema(
            description = "예약 추가 할 수 있는 최소 시간 단위 (분)",
            required = true,
            example = "60"
        )
        @JsonProperty("reservationUnitMinute")
        val reservationUnitMinute: Long,
        @Schema(
            description = "단위 예약 시간을 대여일 기준에서 최소 몇번 추가 해야 하는지",
            required = true,
            example = "1"
        )
        @JsonProperty("minimumReservationUnitCount")
        val minimumReservationUnitCount: Long,
        @Schema(
            description = "단위 예약 시간을 대여일 기준에서 최대 몇번 추가 가능한지 (Null 이라면 제한 없음)",
            required = false,
            example = "3"
        )
        @JsonProperty("maximumReservationUnitCount")
        val maximumReservationUnitCount: Long?,
        @Schema(
            description = "단위 예약 시간에 대한 가격 (예약 시간 / 단위 예약 시간 * 예약 단가 = 예약 최종가)",
            required = true,
            example = "10000"
        )
        @JsonProperty("reservationUnitPrice")
        val reservationUnitPrice: BigDecimal,
        @Schema(
            description = "단위 예약 시간에 대한 가격 통화 코드(IOS 4217)",
            required = true,
            example = "KRW"
        )
        @JsonProperty("reservationUnitPriceCurrencyCode")
        val reservationUnitPriceCurrencyCode: CurrencyCodeEnum,
        @Schema(
            description = "예약 가능 설정 (재고, 상품 상태와 상관 없이 현 시점 예약 가능한지에 대한 관리자의 설정)",
            required = true,
            example = "true"
        )
        @JsonProperty("nowReservable")
        val nowReservable: Boolean,
        @Schema(
            description = "고객에게 이때까지 결제를 해야 한다고 통보하는 기한 설정값(예약일로부터 +N 분)",
            required = true,
            example = "30"
        )
        @JsonProperty("customerPaymentDeadlineMinute")
        val customerPaymentDeadlineMinute: Long,
        @Schema(
            description = "관리자의 결제 확인 기한 설정값(예약일로 부터 +N 분, 고객 결제 기한 설정값보다 크거나 같음)",
            required = true,
            example = "30"
        )
        @JsonProperty("paymentCheckDeadlineMinute")
        val paymentCheckDeadlineMinute: Long,
        @Schema(
            description = "관리자의 예약 승인 기한 설정값(예약일로부터 +N분, 결제 확인 기한 설정값보다 크거나 같음)",
            required = true,
            example = "30"
        )
        @JsonProperty("approvalDeadlineMinute")
        val approvalDeadlineMinute: Long,
        @Schema(
            description = "고객이 예약 취소 가능한 기한 설정값(대여 시작일로부터 -N분으로 계산됨)",
            required = true,
            example = "30"
        )
        @JsonProperty("cancelDeadlineMinute")
        val cancelDeadlineMinute: Long,
        @Schema(
            description = "상품 상태 설명(예를 들어 손망실의 경우 now_reservable 이 false 이며, 이곳에 손망실 이유가 기재됩니다.)",
            required = true,
            example = "이상무"
        )
        @JsonProperty("productStateDesc")
        val productStateDesc: String
    ) {
        enum class CurrencyCodeEnum {
            KRW, USD
        }
    }


    // ----
    @Operation(
        summary = "대여 가능 상품 삭제 <ADMIN>",
        description = "대여 상품을 삭제 처리 합니다."
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
                                "1 : rentalProductUid 에 해당하는 정보가 데이터베이스에 존재하지 않습니다.",
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
    @DeleteMapping(
        path = ["/rental-product/{rentalProductUid}"],
        consumes = [MediaType.ALL_VALUE],
        produces = [MediaType.ALL_VALUE]
    )
    @PreAuthorize("isAuthenticated() and (hasRole('ROLE_ADMIN'))")
    @ResponseBody
    fun deleteRentalProduct(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(hidden = true)
        @RequestHeader("Authorization")
        authorization: String?,
        @Parameter(name = "rentalProductUid", description = "rentalProduct 고유값", example = "1")
        @PathVariable("rentalProductUid")
        rentalProductUid: Long
    ) {
        service.deleteRentalProduct(httpServletResponse, authorization!!, rentalProductUid)
    }


    // ----
    @Operation(
        summary = "대여 가능 상품 추가 예약 가능 설정 수정 <ADMIN>",
        description = "대여 가능 상품을 현 시간부로 예약 가능하게 할 것인지에 대한 스위치 플래그 수정<br>" +
                "update_version_seq 증가는 하지 않습니다."
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
                                "1 : rentalProductUid 에 해당하는 정보가 데이터베이스에 존재하지 않습니다.",
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
    @PatchMapping(
        path = ["/rental-product/{rentalProductUid}/reservable"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.ALL_VALUE]
    )
    @PreAuthorize("isAuthenticated() and (hasRole('ROLE_ADMIN'))")
    @ResponseBody
    fun patchRentalProductReservable(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(hidden = true)
        @RequestHeader("Authorization")
        authorization: String?,
        @Parameter(name = "rentalProductUid", description = "rentableProductInfo 고유값", example = "1")
        @PathVariable("rentalProductUid")
        rentalProductUid: Long,
        @RequestBody
        inputVo: PatchRentalProductReservableInputVo
    ) {
        service.patchRentalProductReservable(
            httpServletResponse,
            authorization!!,
            rentalProductUid,
            inputVo
        )
    }

    data class PatchRentalProductReservableInputVo(
        @Schema(
            description = "예약 가능 설정 (재고, 상품 상태와 상관 없이 현 시점 예약 가능한지에 대한 관리자의 설정)",
            required = true,
            example = "true"
        )
        @JsonProperty("nowReservable")
        val nowReservable: Boolean
    )


    // ----
    @Operation(
        summary = "대여 가능 상품 최소 예약 횟수 설정 수정 <ADMIN>",
        description = "대여 가능 상품의 현 시간부로의 최소 예약 횟수 설정 수정<br>" +
                "update_version_seq 증가는 하지 않습니다."
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
                                "1 : rentableProductInfoUid 에 해당하는 정보가 데이터베이스에 존재하지 않습니다.<br>" +
                                "2 : 최소 예약 횟수는 0보다 크며, 최대 예약 횟수보다 작거나 같아야 합니다.",
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
    @PatchMapping(
        path = ["/rental-product/{rentalProductUid}/min-reservation-unit-count"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.ALL_VALUE]
    )
    @PreAuthorize("isAuthenticated() and (hasRole('ROLE_ADMIN'))")
    @ResponseBody
    fun patchRentalProductMinReservationUnitCount(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(hidden = true)
        @RequestHeader("Authorization")
        authorization: String?,
        @Parameter(name = "rentalProductUid", description = "rentableProductInfo 고유값", example = "1")
        @PathVariable("rentalProductUid")
        rentalProductUid: Long,
        @RequestBody
        inputVo: PatchRentalProductMinReservationUnitCountInputVo
    ) {
        service.patchRentalProductMinReservationUnitCount(
            httpServletResponse,
            authorization!!,
            rentalProductUid,
            inputVo
        )
    }

    data class PatchRentalProductMinReservationUnitCountInputVo(
        @Schema(
            description = "단위 예약 시간을 대여일 기준에서 최소 몇번 추가 해야 하는지",
            required = true,
            example = "1"
        )
        @JsonProperty("minimumReservationUnitCount")
        val minimumReservationUnitCount: Long
    )


    // ----
    @Operation(
        summary = "대여 가능 상품 최대 예약 횟수 설정 수정 <ADMIN>",
        description = "대여 가능 상품의 현 시간부로의 최대 예약 횟수 설정 수정<br>" +
                "update_version_seq 증가는 하지 않습니다."
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
                                "2 : 최대 예약 횟수는 0보다 크며, 최소 예약 횟수보다 크거나 같아야 합니다.",
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
    @PatchMapping(
        path = ["/rental-product/{rentalProductUid}/max-reservation-unit-count"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.ALL_VALUE]
    )
    @PreAuthorize("isAuthenticated() and (hasRole('ROLE_ADMIN'))")
    @ResponseBody
    fun patchRentalProductMaxReservationUnitCount(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(hidden = true)
        @RequestHeader("Authorization")
        authorization: String?,
        @Parameter(name = "rentalProductUid", description = "rentableProductInfo 고유값", example = "1")
        @PathVariable("rentalProductUid")
        rentalProductUid: Long,
        @RequestBody
        inputVo: PatchRentalProductMaxReservationUnitCountInputVo
    ) {
        service.patchRentalProductMaxReservationUnitCount(
            httpServletResponse,
            authorization!!,
            rentalProductUid,
            inputVo
        )
    }

    data class PatchRentalProductMaxReservationUnitCountInputVo(
        @Schema(
            description = "단위 예약 시간을 대여일 기준에서 최대 몇번 추가 해야 하는지",
            required = false,
            example = "1"
        )
        @JsonProperty("maximumReservationUnitCount")
        val maximumReservationUnitCount: Long?
    )


    // ----
    @Operation(
        summary = "대여 가능 상품 이미지 등록 <ADMIN>",
        description = "대여 상품 이미지를 등록합니다."
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
                                "1 : rentalProductUid 에 해당하는 정보가 데이터베이스에 존재하지 않습니다.",
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
    @PostMapping(
        path = ["/rental-product-image"],
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @PreAuthorize("isAuthenticated() and (hasRole('ROLE_ADMIN'))")
    @ResponseBody
    fun postRentalProductImage(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(hidden = true)
        @RequestHeader("Authorization")
        authorization: String?,
        @Parameter
        inputVo: PostRentalProductImageInputVo
    ): PostRentalProductImageOutputVo? {
        return service.postRentalProductImage(httpServletResponse, authorization!!, inputVo)
    }

    data class PostRentalProductImageInputVo(
        @Schema(description = "rentalProduct 고유값", example = "1", required = true)
        @JsonProperty("rentalProductUid")
        val rentalProductUid: Long,
        @Schema(description = "고객에게 보일 상품 썸네일 이미지", required = true)
        @JsonProperty("thumbnailImage")
        val thumbnailImage: MultipartFile,
        @Schema(description = "이미지 가중치(높을수록 전면에 표시되며, 동일 가중치의 경우 최신 정보가 우선됩니다.)", required = true)
        @JsonProperty("priority")
        val priority: Int
    )

    data class PostRentalProductImageOutputVo(
        @Schema(description = "rentalProductImage 고유값", required = true, example = "1")
        @JsonProperty("rentalProductImageUid")
        val rentalProductImageUid: Long,
        @Schema(description = "생성된 이미지 다운로드 경로", required = true, example = "https://testimage.com/sample.jpg")
        @JsonProperty("productImageFullUrl")
        val productImageFullUrl: String
    )


    // ----
    @Operation(
        summary = "대여 가능 상품 이미지 파일 다운받기",
        description = "대여 가능 상품 이미지를 by_product_files 위치에 저장했을 때 파일을 가져오기 위한 API"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정상 동작"
            )
        ]
    )
    @GetMapping(
        path = ["/product-image/{fileName}"],
        consumes = [MediaType.ALL_VALUE],
        produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE]
    )
    @ResponseBody
    fun getProductImageFile(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(name = "fileName", description = "by_product_files 폴더 안의 파일명", example = "test.jpg")
        @PathVariable("fileName")
        fileName: String
    ): ResponseEntity<Resource>? {
        return service.getProductImageFile(httpServletResponse, fileName)
    }


    // ----
    @Operation(
        summary = "대여 가능 상품 이미지 삭제 <ADMIN>",
        description = "대여 상품 이미지를 삭제합니다."
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
                                "1 : rentalProductImageUid 에 해당하는 정보가 데이터베이스에 존재하지 않습니다.",
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
    @DeleteMapping(
        path = ["/rental-product-image/{rentalProductImageUid}"],
        consumes = [MediaType.ALL_VALUE],
        produces = [MediaType.ALL_VALUE]
    )
    @PreAuthorize("isAuthenticated() and (hasRole('ROLE_ADMIN'))")
    @ResponseBody
    fun deleteRentalProductImage(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(hidden = true)
        @RequestHeader("Authorization")
        authorization: String?,
        @Parameter(name = "rentalProductImageUid", description = "rentalProductImage 고유값", example = "1")
        @PathVariable("rentalProductImageUid")
        rentalProductImageUid: Long
    ) {
        service.deleteRentalProductImage(httpServletResponse, authorization!!, rentalProductImageUid)
    }


    // ----
    @Operation(
        summary = "대여 가능 상품 이미지 가중치 수정 <ADMIN>",
        description = "대여 가능 상품 이미지 가중치를 수정합니다.<br>" +
                "update_version_seq 가 증가 하지 않습니다."
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
                                "1 : rentalProductImageUid 에 해당하는 정보가 데이터베이스에 존재하지 않습니다.",
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
    @PatchMapping(
        path = ["/rental-product-image/{rentalProductImageUid}/priority"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.ALL_VALUE]
    )
    @PreAuthorize("isAuthenticated() and (hasRole('ROLE_ADMIN'))")
    @ResponseBody
    fun patchRentalProductImagePriority(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(hidden = true)
        @RequestHeader("Authorization")
        authorization: String?,
        @Parameter(name = "rentalProductImageUid", description = "rentalProductImage 고유값", example = "1")
        @PathVariable("rentalProductImageUid")
        rentalProductImageUid: Long,
        @RequestBody
        inputVo: PatchRentalProductImagePriorityInputVo
    ) {
        service.patchRentalProductImagePriority(
            httpServletResponse,
            authorization!!,
            rentalProductImageUid,
            inputVo
        )
    }

    data class PatchRentalProductImagePriorityInputVo(
        @Schema(description = "이미지 가중치(높을수록 전면에 표시되며, 동일 가중치의 경우 최신 정보가 우선됩니다.)", required = true)
        @JsonProperty("priority")
        val priority: Int
    )


    // ----
    @Operation(
        summary = "예약 신청 승인 처리 <ADMIN>",
        description = "예약 정보를 예약 승인 처리합니다."
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
                                "2 : 예약 취소 상태입니다.<br>" +
                                "3 : 예약 신청 거부 상태입니다.<br>" +
                                "4 : 예약 승인 상태입니다.<br>" +
                                "5 : 미결제 상태 & 결제 기한 초과 상태(= 취소와 동일)",
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
    @PostMapping(
        path = ["/rental-product-reservation/{rentalProductReservationUid}/approve"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @PreAuthorize("isAuthenticated() and (hasRole('ROLE_ADMIN'))")
    @ResponseBody
    fun postReservationApprove(
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
        inputVo: PostReservationApproveInputVo
    ): PostReservationApproveOutputVo? {
        return service.postReservationApprove(
            httpServletResponse,
            authorization!!,
            rentalProductReservationUid,
            inputVo
        )
    }

    data class PostReservationApproveInputVo(
        @Schema(description = "상태 변경 상세 설명", required = true, example = "이상무")
        @JsonProperty("stateChangeDesc")
        val stateChangeDesc: String
    )

    data class PostReservationApproveOutputVo(
        @Schema(description = "reservationHistory 고유값", required = true, example = "1")
        @JsonProperty("reservationHistoryUid")
        val reservationHistoryUid: Long
    )


    // ----
    @Operation(
        summary = "예약 신청 승인 취소 <ADMIN>",
        description = "예약 정보를 예약 승인츨 취소합니다."
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
                                "2 : 예약 취소 상태입니다.<br>" +
                                "3 : 예약 신청 거부 상태입니다.<br>" +
                                "4 : 예약 승인 취소 상태입니다.<br>" +
                                "5 : 미결제 상태 & 결제 기한 초과 상태(= 취소와 동일)<br>" +
                                "6 : 예약 승인/거부 기한이 지났습니다.<br>" +
                                "7 : 예약 승인 내역이 없습니다.",
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
    @PostMapping(
        path = ["/rental-product-reservation/{rentalProductReservationUid}/approve-cancel"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @PreAuthorize("isAuthenticated() and (hasRole('ROLE_ADMIN'))")
    @ResponseBody
    fun postReservationApproveCancel(
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
        inputVo: PostReservationApproveCancelInputVo
    ): PostReservationApproveCancelOutputVo? {
        return service.postReservationApproveCancel(
            httpServletResponse,
            authorization!!,
            rentalProductReservationUid,
            inputVo
        )
    }

    data class PostReservationApproveCancelInputVo(
        @Schema(description = "상태 변경 상세 설명", required = true, example = "이상무")
        @JsonProperty("stateChangeDesc")
        val stateChangeDesc: String
    )

    data class PostReservationApproveCancelOutputVo(
        @Schema(description = "reservationHistory 고유값", required = true, example = "1")
        @JsonProperty("reservationHistoryUid")
        val reservationHistoryUid: Long
    )


    // ----
    @Operation(
        summary = "예약 신청 거부 처리 <ADMIN>",
        description = "대여 가능 상품 예약 정보를 예약 거부 처리합니다.<br>" +
                "상태 변경 철회 불가, 설명 수정은 가능"
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
                                "2 : 예약 승인 기한을 넘겼습니다.<br>" +
                                "3 : 예약 취소 승인 내역 있습니다.<br>" +
                                "4 : 예약 신청 거부 내역 있습니다.<br>" +
                                "5 : 예약 승인 내역 있습니다.<br>" +
                                "6 : 미결제 상태 & 결제 기한 초과 상태(= 취소와 동일)",
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
    @PostMapping(
        path = ["/rental-product-reservation/{rentalProductReservationUid}/reject"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @PreAuthorize("isAuthenticated() and (hasRole('ROLE_ADMIN'))")
    @ResponseBody
    fun postReservationReject(
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
        inputVo: PostReservationRejectInputVo
    ): PostReservationRejectOutputVo? {
        return service.postReservationReject(
            httpServletResponse,
            authorization!!,
            rentalProductReservationUid,
            inputVo
        )
    }

    data class PostReservationRejectInputVo(
        @Schema(description = "상태 변경 상세 설명", required = true, example = "이상무")
        @JsonProperty("stateChangeDesc")
        val stateChangeDesc: String
    )

    data class PostReservationRejectOutputVo(
        @Schema(description = "reservationHistory 고유값", required = true, example = "1")
        @JsonProperty("reservationHistoryUid")
        val reservationHistoryUid: Long
    )


    // ----
    @Operation(
        summary = "예약 취소 승인 처리 <ADMIN>",
        description = "대여 가능 상품 예약 정보를 예약 취소 승인 처리합니다.<br>" +
                "상태 변경 철회 불가, 설명 수정은 가능"
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
                                "2 : 대여 시작 기한 초과하였습니다.<br>" +
                                "3 : 예약 취소 승인 내역 있습니다.<br>" +
                                "4 : 예약 신청 거부 내역 있습니다.<br>" +
                                "5 : 미결제 상태 & 결제 기한 초과 상태(= 취소와 동일)<br>" +
                                "6 : 예약 취소 신청 내역이 없습니다.<br>" +
                                "7 : 기존 예약 취소 신청에 대한 예약 취소 거부 상태입니다.<br>" +
                                "8 : 기존 예약 취소 신청에 대한 취소 상태입니다.",
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
    @PostMapping(
        path = ["/rental-product-reservation/{rentalProductReservationUid}/cancel-approve"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @PreAuthorize("isAuthenticated() and (hasRole('ROLE_ADMIN'))")
    @ResponseBody
    fun postReservationCancelApprove(
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
        inputVo: PostReservationCancelApproveInputVo
    ): PostReservationCancelApproveOutputVo? {
        return service.postReservationCancelApprove(
            httpServletResponse,
            authorization!!,
            rentalProductReservationUid,
            inputVo
        )
    }

    data class PostReservationCancelApproveInputVo(
        @Schema(description = "상태 변경 상세 설명", required = true, example = "이상무")
        @JsonProperty("stateChangeDesc")
        val stateChangeDesc: String
    )

    data class PostReservationCancelApproveOutputVo(
        @Schema(description = "reservationHistory 고유값", required = true, example = "1")
        @JsonProperty("reservationHistoryUid")
        val reservationHistoryUid: Long
    )


    // ----
    @Operation(
        summary = "예약 취소 거부 처리 <ADMIN>",
        description = "대여 가능 상품 예약 정보를 예약 취소 거부 처리합니다.<br>" +
                "상태 변경 철회 불가, 설명 수정은 가능<br>" +
                "rentable_product_reservation_state_change_history 에 예약 취소 거부 히스토리를 추가합니다."
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
                                "2 : 대여 시작 시간을 초과하였습니다.<br>" +
                                "3 : 예약 취소 승인 내역 있습니다.<br>" +
                                "4 : 예약 신청 거부 내역 있습니다.<br>" +
                                "5 : 미결제 상태 & 결제 기한 초과 상태(= 취소와 동일)<br>" +
                                "6 : 예약 취소 신청 내역이 없습니다.<br>" +
                                "7 : 기존 예약 취소 신청에 대한 예약 취소 거부 상태입니다.<br>" +
                                "8 : 기존 예약 취소 신청에 대한 취소 상태입니다.",
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
    @PostMapping(
        path = ["/rental-product-reservation/{rentalProductReservationUid}/cancel-reject"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @PreAuthorize("isAuthenticated() and (hasRole('ROLE_ADMIN'))")
    @ResponseBody
    fun postReservationCancelReject(
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
        inputVo: PostReservationCancelRejectInputVo
    ): PostReservationCancelRejectOutputVo? {
        return service.postReservationCancelReject(
            httpServletResponse,
            authorization!!,
            rentalProductReservationUid,
            inputVo
        )
    }

    data class PostReservationCancelRejectInputVo(
        @Schema(description = "상태 변경 상세 설명", required = true, example = "이상무")
        @JsonProperty("stateChangeDesc")
        val stateChangeDesc: String
    )

    data class PostReservationCancelRejectOutputVo(
        @Schema(description = "reservationHistory 고유값", required = true, example = "1")
        @JsonProperty("reservationHistoryUid")
        val reservationHistoryUid: Long
    )


    // ----
    @Operation(
        summary = "결제 확인 처리 <ADMIN>",
        description = "대여 가능 상품 예약 정보를 결제 확인 처리합니다."
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
                                "2 : 결제 확인 내역이 존재합니다.<br>" +
                                "3 : 결제 확인 가능 기한을 초과하였습니다.",
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
    @PostMapping(
        path = ["/rental-product-reservation/{rentalProductReservationUid}/payment-complete"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @PreAuthorize("isAuthenticated() and (hasRole('ROLE_ADMIN'))")
    @ResponseBody
    fun postRentableProductReservationInfoPaymentComplete(
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
        inputVo: PostRentableProductReservationInfoPaymentCompleteInputVo
    ): PostRentableProductReservationInfoPaymentCompleteOutputVo? {
        return service.postRentableProductReservationInfoPaymentComplete(
            httpServletResponse,
            authorization!!,
            rentalProductReservationUid,
            inputVo
        )
    }

    data class PostRentableProductReservationInfoPaymentCompleteInputVo(
        @Schema(description = "상태 변경 상세 설명", required = true, example = "이상무")
        @JsonProperty("stateChangeDesc")
        val stateChangeDesc: String
    )

    data class PostRentableProductReservationInfoPaymentCompleteOutputVo(
        @Schema(description = "reservationHistory 고유값", required = true, example = "1")
        @JsonProperty("reservationHistoryUid")
        val reservationHistoryUid: Long
    )


    // ----
    @Operation(
        summary = "결제 확인 취소 <ADMIN>",
        description = "대여 가능 상품 예약 정보를 결제 확인 취소 처리합니다."
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
                                "2 : 결제 확인 취소 상태입니다.<br>" +
                                "3 : 결제 확인 가능 기한을 초과하였습니다.<br>" +
                                "4 : 결제 가격이 0원인 상품은 결제 확인을 취소할 수 없습니다.",
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
    @PostMapping(
        path = ["/rental-product-reservation/{rentalProductReservationUid}/payment-complete-cancel"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @PreAuthorize("isAuthenticated() and (hasRole('ROLE_ADMIN'))")
    @ResponseBody
    fun postRentableProductReservationInfoPaymentCompleteCancel(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(hidden = true)
        @RequestHeader("Authorization")
        authorization: String?,
        @Parameter(
            name = "rentalProductReservationUid",
            description = "rentableProductReservation 고유값",
            example = "1"
        )
        @PathVariable("rentalProductReservationUid")
        rentalProductReservationUid: Long,
        @RequestBody
        inputVo: PostRentableProductReservationInfoPaymentCompleteCancelInputVo
    ): PostRentableProductReservationInfoPaymentCompleteCancelOutputVo? {
        return service.postRentableProductReservationInfoPaymentCompleteCancel(
            httpServletResponse,
            authorization!!,
            rentalProductReservationUid,
            inputVo
        )
    }

    data class PostRentableProductReservationInfoPaymentCompleteCancelInputVo(
        @Schema(description = "상태 변경 상세 설명", required = true, example = "이상무")
        @JsonProperty("stateChangeDesc")
        val stateChangeDesc: String
    )

    data class PostRentableProductReservationInfoPaymentCompleteCancelOutputVo(
        @Schema(description = "reservationHistory 고유값", required = true, example = "1")
        @JsonProperty("reservationHistoryUid")
        val reservationHistoryUid: Long
    )


    // ----
    @Operation(
        summary = "상품 반납 확인 처리 <ADMIN>",
        description = "개별 상품에 대해 반납 확인 처리를 합니다.<br>" +
                "상품 준비시간 설정은 독립적이기에 영향을 받지 않습니다."
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
                                "2 : 예약 거부 상태<br>" +
                                "3 : 조기 반납 신고 상태가 아니고(내역이 없거나 취소 상태), 상품 반납일도 안됨<br>" +
                                "4 : 반납 확인 상태입니다.<br>" +
                                "5 : 결제 확인 완료 아님 = 대여 진행 상태가 아님",
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
    @PostMapping(
        path = ["/rental-product-reservation/{rentalProductReservationUid}/return-check"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @PreAuthorize("isAuthenticated() and (hasRole('ROLE_ADMIN'))")
    @ResponseBody
    fun postRentableProductStockReservationInfoReturnCheck(
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
        inputVo: PostRentableProductStockReservationInfoReturnCheckInputVo
    ): PostRentableProductStockReservationInfoReturnCheckOutputVo? {
        return service.postRentableProductStockReservationInfoReturnCheck(
            httpServletResponse,
            authorization!!,
            rentalProductReservationUid,
            inputVo
        )
    }

    data class PostRentableProductStockReservationInfoReturnCheckInputVo(
        @Schema(description = "상태 변경 상세 설명", required = true, example = "이상무")
        @JsonProperty("stateChangeDesc")
        val stateChangeDesc: String
    )

    data class PostRentableProductStockReservationInfoReturnCheckOutputVo(
        @Schema(description = "reservationHistory 고유값", required = true, example = "1")
        @JsonProperty("reservationHistoryUid")
        val reservationHistoryUid: Long
    )


    // ----
    @Operation(
        summary = "상품 준비 시간 설정 <ADMIN>",
        description = "개별 상품에 대해 준비 완료 일시를 설정 합니다.<br>" +
                "readyDatetime 변수를 미래로 설정하는 식으로 미리 준비 설정을 할 수도 있습니다.<br>" +
                "다른 모든 상태 정보에 앞서며, 연체 처리, 손망실 처리를 하면 이 설정이 지워집니다."
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
                                "1 : rentalProductReservationUid 에 해당하는 정보가 데이터베이스에 존재하지 않습니다.",
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
    @PatchMapping(
        path = ["/rental-product-reservation/{rentalProductReservationUid}/ready"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.ALL_VALUE]
    )
    @PreAuthorize("isAuthenticated() and (hasRole('ROLE_ADMIN'))")
    @ResponseBody
    fun patchRentableProductStockReservationInfoReady(
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
        inputVo: PatchRentableProductStockReservationInfoReadyInputVo
    ) {
        service.patchRentableProductStockReservationInfoReady(
            httpServletResponse,
            authorization!!,
            rentalProductReservationUid,
            inputVo
        )
    }

    data class PatchRentableProductStockReservationInfoReadyInputVo(
        @Schema(
            description = "상품 준비 일시(yyyy_MM_dd_'T'_HH_mm_ss_z, null 이라면 설정 해제)",
            required = true,
            example = "2024_05_02_T_15_14_49_KST"
        )
        @JsonProperty("readyDatetime")
        val readyDatetime: String?
    )


    // ----
    @Operation(
        summary = "상품 연체 상태 설정 처리 <ADMIN>",
        description = "개별 상품에 대해 연체 상태로 변경 처리를 합니다.<br>" +
                "연체 상태가 되어도 기존 상품 준비일은 변경 없습니다."
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
                                "2 : 예약 거부 상태<br>" +
                                "3 : 상품 반납일이 도래하지 않았습니다.<br>" +
                                "4 : 이미 연체 상태입니다.<br>" +
                                "5 : 이미 반납 확인을 한 상태입니다.<br>" +
                                "6 : 결제 확인 완료 아님 = 대여 진행 상태가 아님",
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
    @PostMapping(
        path = ["/rental-product-reservation/{rentalProductReservationUid}/overdue"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @PreAuthorize("isAuthenticated() and (hasRole('ROLE_ADMIN'))")
    @ResponseBody
    fun postRentableProductStockReservationInfoOverdue(
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
        inputVo: PostRentableProductStockReservationInfoOverdueInputVo
    ): PostRentableProductStockReservationInfoOverdueOutputVo? {
        return service.postRentableProductStockReservationInfoOverdue(
            httpServletResponse,
            authorization!!,
            rentalProductReservationUid,
            inputVo
        )
    }

    data class PostRentableProductStockReservationInfoOverdueInputVo(
        @Schema(description = "상태 변경 상세 설명", required = true, example = "이상무")
        @JsonProperty("stateChangeDesc")
        val stateChangeDesc: String
    )

    data class PostRentableProductStockReservationInfoOverdueOutputVo(
        @Schema(description = "reservationHistory 고유값", required = true, example = "1")
        @JsonProperty("reservationHistoryUid")
        val reservationHistoryUid: Long
    )


    // ----
    @Operation(
        summary = "상품 연체 상태 설정 취소 처리 <ADMIN>",
        description = "개별 상품에 대해 연체 상태 변경 취소 처리를 합니다."
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
                                "2 : 예약 거부 상태<br>" +
                                "3 : 연체 상태 설정 내역이 없습니다.<br>" +
                                "4 : 연제 상태 변경 취소 상태입니다.<br>" +
                                "5 : 결제 확인 완료 아님 = 대여 진행 상태가 아님",
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
    @PostMapping(
        path = ["/rental-product-reservation/{rentalProductReservationUid}/overdue-cancel"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @PreAuthorize("isAuthenticated() and (hasRole('ROLE_ADMIN'))")
    @ResponseBody
    fun postRentableProductStockReservationInfoOverdueCancel(
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
        inputVo: PostRentableProductStockReservationInfoOverdueCancelInputVo
    ): PostRentableProductStockReservationInfoOverdueCancelOutputVo? {
        return service.postRentableProductStockReservationInfoOverdueCancel(
            httpServletResponse,
            authorization!!,
            rentalProductReservationUid,
            inputVo
        )
    }

    data class PostRentableProductStockReservationInfoOverdueCancelInputVo(
        @Schema(description = "상태 변경 상세 설명", required = true, example = "이상무")
        @JsonProperty("stateChangeDesc")
        val stateChangeDesc: String
    )

    data class PostRentableProductStockReservationInfoOverdueCancelOutputVo(
        @Schema(description = "reservationHistory 고유값", required = true, example = "1")
        @JsonProperty("reservationHistoryUid")
        val reservationHistoryUid: Long
    )


    // ----
    @Operation(
        summary = "예약 연장 신청 승인 <Admin>",
        description = "예약 연장 신청을 승인 합니다."
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
        path = ["/rental-product-reservation/{rentalProductReservationUid}/rental-extend-apply"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @PreAuthorize("isAuthenticated() and (hasRole('ROLE_ADMIN'))")
    @ResponseBody
    fun postRentableProductStockReservationInfoRentalExtendApply(
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
        inputVo: PostRentableProductStockReservationInfoRentalExtendApplyInputVo
    ): PostRentableProductStockReservationInfoRentalExtendApplyOutputVo? {
        return service.postRentableProductStockReservationInfoRentalExtendApply(
            httpServletResponse,
            authorization!!,
            rentalProductReservationUid,
            inputVo
        )
    }

    data class PostRentableProductStockReservationInfoRentalExtendApplyInputVo(
        @Schema(description = "상태 변경 상세 설명", required = true, example = "이상무")
        @JsonProperty("stateChangeDesc")
        val stateChangeDesc: String
    )

    data class PostRentableProductStockReservationInfoRentalExtendApplyOutputVo(
        @Schema(description = "reservationHistory 고유값", required = true, example = "1")
        @JsonProperty("reservationHistoryUid")
        val reservationHistoryUid: Long
    )


    // ----
    @Operation(
        summary = "예약 연장 신청 거부 <Admin>",
        description = "예약 연장 신청을 거부 합니다."
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
        path = ["/rental-product-reservation/{rentalProductReservationUid}/rental-extend-deny"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @PreAuthorize("isAuthenticated() and (hasRole('ROLE_ADMIN'))")
    @ResponseBody
    fun postRentableProductStockReservationInfoRentalExtendDeny(
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
        inputVo: PostRentableProductStockReservationInfoRentalExtendDenyInputVo
    ): PostRentableProductStockReservationInfoRentalExtendDenyOutputVo? {
        return service.postRentableProductStockReservationInfoRentalExtendDeny(
            httpServletResponse,
            authorization!!,
            rentalProductReservationUid,
            inputVo
        )
    }

    data class PostRentableProductStockReservationInfoRentalExtendDenyInputVo(
        @Schema(description = "상태 변경 상세 설명", required = true, example = "이상무")
        @JsonProperty("stateChangeDesc")
        val stateChangeDesc: String
    )

    data class PostRentableProductStockReservationInfoRentalExtendDenyOutputVo(
        @Schema(description = "reservationHistory 고유값", required = true, example = "1")
        @JsonProperty("reservationHistoryUid")
        val reservationHistoryUid: Long
    )


    // ----
    @Operation(
        summary = "대여 가능 상품 예약 상태 테이블의 상세 설명 수정 <ADMIN>",
        description = "대여 가능 상품 예약 상태 테이블의 상세 설명을 수정 처리합니다.<br>" +
                "한번 결정된 상태 코드는 변하지 않습니다."
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
                                "1 : reservationHistoryUid 에 해당하는 정보가 데이터베이스에 존재하지 않습니다.",
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
    @PatchMapping(
        path = ["/reservation-history/{reservationHistoryUid}/history-desc"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.ALL_VALUE]
    )
    @PreAuthorize("isAuthenticated() and (hasRole('ROLE_ADMIN'))")
    @ResponseBody
    fun patchReservationStateChangeHistoryStateChangeDesc(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse,
        @Parameter(hidden = true)
        @RequestHeader("Authorization")
        authorization: String?,
        @Parameter(
            name = "reservationHistoryUid",
            description = "reservationHistory 고유값",
            example = "1"
        )
        @PathVariable("reservationHistoryUid")
        reservationHistoryUid: Long,
        @RequestBody
        inputVo: PatchReservationStateChangeHistoryStateChangeDescInputVo
    ) {
        service.patchReservationStateChangeHistoryStateChangeDesc(
            httpServletResponse,
            authorization!!,
            reservationHistoryUid,
            inputVo
        )
    }

    data class PatchReservationStateChangeHistoryStateChangeDescInputVo(
        @Schema(description = "히스토리 상세 설명", required = true, example = "이상무")
        @JsonProperty("historyDesc")
        val historyDesc: String
    )


    // ----
    @Operation(
        summary = "예약 취소 신청 <Admin>",
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
    @PreAuthorize("isAuthenticated() and (hasRole('ROLE_ADMIN'))")
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
        summary = "예약 취소 신청 철회 <Admin>",
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
    @PreAuthorize("isAuthenticated() and (hasRole('ROLE_ADMIN'))")
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
        summary = "개별 상품 조기 반납 신고 <Admin>",
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
    @PreAuthorize("isAuthenticated() and (hasRole('ROLE_ADMIN'))")
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
        summary = "개별 상품 조기 반납 신고 취소 <Admin>",
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
    @PreAuthorize("isAuthenticated() and (hasRole('ROLE_ADMIN'))")
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
        summary = "예약 연장 신청 <Admin>",
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
    @PreAuthorize("isAuthenticated() and (hasRole('ROLE_ADMIN'))")
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
        summary = "예약 연장 신청 취소 <Admin>",
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
    @PreAuthorize("isAuthenticated() and (hasRole('ROLE_ADMIN'))")
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