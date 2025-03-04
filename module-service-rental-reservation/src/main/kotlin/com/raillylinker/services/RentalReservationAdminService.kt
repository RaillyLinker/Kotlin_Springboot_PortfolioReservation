package com.raillylinker.services

import com.raillylinker.util_components.JwtTokenUtil
import com.raillylinker.configurations.jpa_configs.Db1MainConfig
import com.raillylinker.controllers.RentalReservationAdminController
import com.raillylinker.jpa_beans.db1_main.entities.Db1_RaillyLinkerCompany_RentalProductImage
import com.raillylinker.jpa_beans.db1_main.entities.Db1_RaillyLinkerCompany_RentalProduct
import com.raillylinker.jpa_beans.db1_main.entities.Db1_RaillyLinkerCompany_RentalProductReservation
import com.raillylinker.jpa_beans.db1_main.entities.Db1_RaillyLinkerCompany_RentalProductReservationHistory
import com.raillylinker.jpa_beans.db1_main.repositories.*
import com.raillylinker.redis_map_components.redis1_main.Redis1_Lock_RentalProductInfo
import com.raillylinker.util_components.CustomUtil
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.Resource
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.File
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter


@Service
class RentalReservationAdminService(
    // (프로젝트 실행시 사용 설정한 프로필명 (ex : dev8080, prod80, local8080, 설정 안하면 default 반환))
    @Value("\${spring.profiles.active:default}") private var activeProfile: String,

    private val jwtTokenUtil: JwtTokenUtil,

    private val customUtil: CustomUtil,

    private val db1RaillyLinkerCompanyRentalProductImageRepository: Db1_RaillyLinkerCompany_RentalProductImage_Repository,
    private val db1RaillyLinkerCompanyRentalProductRepository: Db1_RaillyLinkerCompany_RentalProduct_Repository,
    private val db1RaillyLinkerCompanyRentalProductReservationRepository: Db1_RaillyLinkerCompany_RentalProductReservation_Repository,
    private val db1RaillyLinkerCompanyRentalProductReservationHistoryRepository: Db1_RaillyLinkerCompany_RentalProductReservationHistory_Repository,
    private val db1RaillyLinkerCompanyRentableProductReservationInfoRepository: Db1_RaillyLinkerCompany_RentalProductReservation_Repository,
    private val db1RaillyLinkerCompanyRentableProductReservationStateChangeHistoryRepository: Db1_RaillyLinkerCompany_RentalProductReservationHistory_Repository,

    private val redis1LockRentalProductInfo: Redis1_Lock_RentalProductInfo
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

                "dev13001" -> {
                    "http://127.0.0.1:13001"
                }

                else -> {
                    "http://127.0.0.1:13001"
                }
            }
        }


    // ---------------------------------------------------------------------------------------------
    // <공개 메소드 공간>
    // (대여 가능 상품 등록 <ADMIN>)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun postRentalProduct(
        httpServletResponse: HttpServletResponse,
        authorization: String,
        inputVo: RentalReservationAdminController.PostRentalProductInputVo
    ): RentalReservationAdminController.PostRentalProductOutputVo? {
//        val memberUid = jwtTokenUtil.getMemberUid(
//            authorization.split(" ")[1].trim(),
//            authTokenFilterTotalAuth.authJwtClaimsAes256InitializationVector,
//            authTokenFilterTotalAuth.authJwtClaimsAes256EncryptionKey
//        )

        if (inputVo.reservationUnitMinute < 0L ||
            inputVo.reservationUnitPrice < BigDecimal.valueOf(0L) ||
            inputVo.customerPaymentDeadlineMinute < 0L ||
            inputVo.paymentCheckDeadlineMinute < 0L ||
            inputVo.approvalDeadlineMinute < 0L ||
            inputVo.cancelDeadlineMinute < 0L
        ) {
            // reservationUnitMinute, reservationUnitPrice, customerPaymentDeadlineMinute, paymentCheckDeadlineMinute,
            // paymentCheckDeadlineMinute, approvalDeadlineMinute, cancelDeadlineMinute 는 음수가 될 수 없습니다.-> return
            httpServletResponse.status = HttpStatus.BAD_REQUEST.value()
            return null
        }

        if (inputVo.customerPaymentDeadlineMinute > inputVo.paymentCheckDeadlineMinute) {
            // 결제 통보 기한이 결제 승인 기한보다 클 경우 -> return
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "2")
            return null
        }

        if (inputVo.paymentCheckDeadlineMinute > inputVo.approvalDeadlineMinute) {
            // 결제 승인 기한이 예약 승인 기한보다 클 경우 -> return
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "3")
            return null
        }

        if (inputVo.maximumReservationUnitCount != null &&
            inputVo.minimumReservationUnitCount > inputVo.maximumReservationUnitCount
        ) {
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return null
        }

        val rentableProductInfo = db1RaillyLinkerCompanyRentalProductRepository.save(
            Db1_RaillyLinkerCompany_RentalProduct(
                inputVo.productName,
                inputVo.productIntro,
                inputVo.addressCountry,
                inputVo.addressMain,
                inputVo.addressDetail,
                ZonedDateTime.parse(
                    inputVo.firstReservableDatetime,
                    DateTimeFormatter.ofPattern("yyyy_MM_dd_'T'_HH_mm_ss_z")
                ).withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime(),
                ZonedDateTime.parse(
                    inputVo.firstRentalDatetime,
                    DateTimeFormatter.ofPattern("yyyy_MM_dd_'T'_HH_mm_ss_z")
                ).withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime(),
                if (inputVo.lastRentalDatetime == null) {
                    null
                } else {
                    ZonedDateTime.parse(
                        inputVo.lastRentalDatetime,
                        DateTimeFormatter.ofPattern("yyyy_MM_dd_'T'_HH_mm_ss_z")
                    ).withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()
                },
                inputVo.reservationUnitMinute,
                inputVo.minimumReservationUnitCount,
                inputVo.maximumReservationUnitCount,
                inputVo.reservationUnitPrice,
                inputVo.reservationUnitPriceCurrencyCode.name,
                inputVo.nowReservable,
                inputVo.customerPaymentDeadlineMinute,
                inputVo.paymentCheckDeadlineMinute,
                inputVo.approvalDeadlineMinute,
                inputVo.cancelDeadlineMinute,
                ""
            )
        )

        httpServletResponse.status = HttpStatus.OK.value()
        return RentalReservationAdminController.PostRentalProductOutputVo(
            rentableProductInfo.uid!!
        )
    }


    // ----
    // (대여 가능 상품 수정 <ADMIN>)
    // rentableProductInfoUid 관련 공유 락 처리 (예약하기 시점에 예약 정보에 영향을 끼치는 데이터 안정화)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun putRentalProduct(
        httpServletResponse: HttpServletResponse,
        authorization: String,
        rentableProductInfoUid: Long,
        inputVo: RentalReservationAdminController.PutRentalProductInputVo
    ) {
//        val memberUid = jwtTokenUtil.getMemberUid(
//            authorization.split(" ")[1].trim(),
//            authTokenFilterTotalAuth.authJwtClaimsAes256InitializationVector,
//            authTokenFilterTotalAuth.authJwtClaimsAes256EncryptionKey
//        )

        if (inputVo.reservationUnitMinute < 0L ||
            inputVo.reservationUnitPrice < BigDecimal.valueOf(0L) ||
            inputVo.customerPaymentDeadlineMinute < 0L ||
            inputVo.paymentCheckDeadlineMinute < 0L ||
            inputVo.approvalDeadlineMinute < 0L ||
            inputVo.cancelDeadlineMinute < 0L
        ) {
            // reservationUnitMinute, reservationUnitPrice, customerPaymentDeadlineMinute, paymentCheckDeadlineMinute,
            // paymentCheckDeadlineMinute, approvalDeadlineMinute, cancelDeadlineMinute 는 음수가 될 수 없습니다.-> return
            httpServletResponse.status = HttpStatus.BAD_REQUEST.value()
            return
        }

        if (inputVo.customerPaymentDeadlineMinute > inputVo.paymentCheckDeadlineMinute) {
            // 결제 통보 기한이 결제 승인 기한보다 클 경우 -> return
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "3")
            return
        }

        if (inputVo.paymentCheckDeadlineMinute > inputVo.approvalDeadlineMinute) {
            // 결제 승인 기한이 예약 승인 기한보다 클 경우 -> return
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "4")
            return
        }

        if (inputVo.maximumReservationUnitCount != null &&
            inputVo.minimumReservationUnitCount > inputVo.maximumReservationUnitCount
        ) {
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "2")
            return
        }

        // rentableProductInfoUid 관련 공유 락 처리 (예약하기 시점에 예약 정보에 영향을 끼치는 데이터 안정화)
        redis1LockRentalProductInfo.tryLockRepeat(
            "$rentableProductInfoUid",
            7000L,
            {
                val rentableProduct =
                    db1RaillyLinkerCompanyRentalProductRepository.findByUidAndRowDeleteDateStr(
                        rentableProductInfoUid,
                        "/"
                    )

                if (rentableProduct == null) {
                    httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                    httpServletResponse.setHeader("api-result-code", "1")
                    return@tryLockRepeat
                }

                rentableProduct.productName = inputVo.productName
                rentableProduct.productIntro = inputVo.productIntro
                rentableProduct.addressCountry = inputVo.addressCountry
                rentableProduct.addressMain = inputVo.addressMain
                rentableProduct.addressDetail = inputVo.addressDetail
                rentableProduct.firstReservableDatetime = ZonedDateTime.parse(
                    inputVo.firstReservableDatetime,
                    DateTimeFormatter.ofPattern("yyyy_MM_dd_'T'_HH_mm_ss_z")
                ).withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()
                rentableProduct.firstRentalDatetime = ZonedDateTime.parse(
                    inputVo.firstRentalDatetime,
                    DateTimeFormatter.ofPattern("yyyy_MM_dd_'T'_HH_mm_ss_z")
                ).withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()
                rentableProduct.lastRentalDatetime =
                    if (inputVo.lastRentalDatetime == null) {
                        null
                    } else {
                        ZonedDateTime.parse(
                            inputVo.lastRentalDatetime,
                            DateTimeFormatter.ofPattern("yyyy_MM_dd_'T'_HH_mm_ss_z")
                        ).withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()
                    }
                rentableProduct.reservationUnitMinute = inputVo.reservationUnitMinute
                rentableProduct.minimumReservationUnitCount = inputVo.minimumReservationUnitCount
                rentableProduct.maximumReservationUnitCount = inputVo.maximumReservationUnitCount
                rentableProduct.reservationUnitPrice = inputVo.reservationUnitPrice
                rentableProduct.reservationUnitPriceCurrencyCode = inputVo.reservationUnitPriceCurrencyCode.name
                rentableProduct.nowReservable = inputVo.nowReservable
                rentableProduct.customerPaymentDeadlineMinute = inputVo.customerPaymentDeadlineMinute
                rentableProduct.paymentCheckDeadlineMinute = inputVo.paymentCheckDeadlineMinute
                rentableProduct.approvalDeadlineMinute = inputVo.approvalDeadlineMinute
                rentableProduct.cancelDeadlineMinute = inputVo.cancelDeadlineMinute
                rentableProduct.versionSeq += 1

                db1RaillyLinkerCompanyRentalProductRepository.save(rentableProduct)

                httpServletResponse.status = HttpStatus.OK.value()
                return@tryLockRepeat
            }
        )
    }


    // ----
    // (대여 가능 상품 삭제 <ADMIN>)
    // rentableProductInfoUid 관련 공유 락 처리 (예약하기 시점에 예약 정보에 영향을 끼치는 데이터 안정화)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun deleteRentalProduct(
        httpServletResponse: HttpServletResponse,
        authorization: String,
        rentableProductInfoUid: Long
    ) {
//        val memberUid = jwtTokenUtil.getMemberUid(
//            authorization.split(" ")[1].trim(),
//            authTokenFilterTotalAuth.authJwtClaimsAes256InitializationVector,
//            authTokenFilterTotalAuth.authJwtClaimsAes256EncryptionKey
//        )

        redis1LockRentalProductInfo.tryLockRepeat(
            "$rentableProductInfoUid",
            7000L,
            {
                val rentableProductInfo: Db1_RaillyLinkerCompany_RentalProduct? =
                    db1RaillyLinkerCompanyRentalProductRepository.findByUidAndRowDeleteDateStr(
                        rentableProductInfoUid,
                        "/"
                    )

                if (rentableProductInfo == null) {
                    httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                    httpServletResponse.setHeader("api-result-code", "1")
                    return@tryLockRepeat
                }

                for (rentableProductReservationInfo in rentableProductInfo.rentalProductReservationList) {
                    // 예약 정보에서 고유값 null 처리
                    rentableProductReservationInfo.rentalProduct = null
                    db1RaillyLinkerCompanyRentalProductReservationRepository.save(rentableProductReservationInfo)
                }

                for (rentableProductImage in rentableProductInfo.rentalProductImageList) {
                    rentableProductImage.rowDeleteDateStr =
                        LocalDateTime.now().atZone(ZoneId.systemDefault())
                            .format(DateTimeFormatter.ofPattern("yyyy_MM_dd_'T'_HH_mm_ss_SSS_z"))

                    db1RaillyLinkerCompanyRentalProductImageRepository.save(
                        rentableProductImage
                    )
                }

                // 테이블 삭제 처리
                rentableProductInfo.rowDeleteDateStr =
                    LocalDateTime.now().atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("yyyy_MM_dd_'T'_HH_mm_ss_SSS_z"))
                db1RaillyLinkerCompanyRentalProductRepository.save(rentableProductInfo)

                // 필요하다면 이미지 삭제 처리

                httpServletResponse.status = HttpStatus.OK.value()
                return@tryLockRepeat
            }
        )
    }


    // ----
    // (대여 가능 상품 추가 예약 가능 설정 수정 <ADMIN>)
    // rentableProductInfoUid 관련 공유 락 처리 (예약하기 시점에 예약 정보에 영향을 끼치는 데이터 안정화)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun patchRentalProductReservable(
        httpServletResponse: HttpServletResponse,
        authorization: String,
        rentableProductInfoUid: Long,
        inputVo: RentalReservationAdminController.PatchRentalProductReservableInputVo
    ) {
//        val memberUid = jwtTokenUtil.getMemberUid(
//            authorization.split(" ")[1].trim(),
//            authTokenFilterTotalAuth.authJwtClaimsAes256InitializationVector,
//            authTokenFilterTotalAuth.authJwtClaimsAes256EncryptionKey
//        )

        redis1LockRentalProductInfo.tryLockRepeat(
            "$rentableProductInfoUid",
            7000L,
            {
                val rentableProduct = db1RaillyLinkerCompanyRentalProductRepository.findByUidAndRowDeleteDateStr(
                    rentableProductInfoUid,
                    "/"
                )

                if (rentableProduct == null) {
                    httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                    httpServletResponse.setHeader("api-result-code", "1")
                    return@tryLockRepeat
                }

                rentableProduct.nowReservable = inputVo.nowReservable

                db1RaillyLinkerCompanyRentalProductRepository.save(rentableProduct)

                httpServletResponse.status = HttpStatus.OK.value()
                return@tryLockRepeat
            }
        )
    }


    // ----
    // (대여 가능 상품 최소 예약 횟수 설정 수정 <ADMIN>)
    // rentableProductInfoUid 관련 공유 락 처리 (예약하기 시점에 예약 정보에 영향을 끼치는 데이터 안정화)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun patchRentalProductMinReservationUnitCount(
        httpServletResponse: HttpServletResponse,
        authorization: String,
        rentableProductInfoUid: Long,
        inputVo: RentalReservationAdminController.PatchRentalProductMinReservationUnitCountInputVo
    ) {
//        val memberUid = jwtTokenUtil.getMemberUid(
//            authorization.split(" ")[1].trim(),
//            authTokenFilterTotalAuth.authJwtClaimsAes256InitializationVector,
//            authTokenFilterTotalAuth.authJwtClaimsAes256EncryptionKey
//        )

        redis1LockRentalProductInfo.tryLockRepeat(
            "$rentableProductInfoUid",
            7000L,
            {
                val rentableProduct = db1RaillyLinkerCompanyRentalProductRepository.findByUidAndRowDeleteDateStr(
                    rentableProductInfoUid,
                    "/"
                )

                if (rentableProduct == null) {
                    httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                    httpServletResponse.setHeader("api-result-code", "1")
                    return@tryLockRepeat
                }

                if (inputVo.minimumReservationUnitCount < 0 ||
                    (rentableProduct.maximumReservationUnitCount != null &&
                            inputVo.minimumReservationUnitCount > rentableProduct.maximumReservationUnitCount!!)
                ) {
                    httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                    httpServletResponse.setHeader("api-result-code", "2")
                    return@tryLockRepeat
                }

                rentableProduct.minimumReservationUnitCount = inputVo.minimumReservationUnitCount

                db1RaillyLinkerCompanyRentalProductRepository.save(rentableProduct)

                httpServletResponse.status = HttpStatus.OK.value()
                return@tryLockRepeat
            }
        )
    }


    // ----
    // (대여 가능 상품 최대 예약 횟수 설정 수정 <ADMIN>)
    // rentableProductInfoUid 관련 공유 락 처리 (예약하기 시점에 예약 정보에 영향을 끼치는 데이터 안정화)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun patchRentalProductMaxReservationUnitCount(
        httpServletResponse: HttpServletResponse,
        authorization: String,
        rentableProductInfoUid: Long,
        inputVo: RentalReservationAdminController.PatchRentalProductMaxReservationUnitCountInputVo
    ) {
//        val memberUid = jwtTokenUtil.getMemberUid(
//            authorization.split(" ")[1].trim(),
//            authTokenFilterTotalAuth.authJwtClaimsAes256InitializationVector,
//            authTokenFilterTotalAuth.authJwtClaimsAes256EncryptionKey
//        )

        redis1LockRentalProductInfo.tryLockRepeat(
            "$rentableProductInfoUid",
            7000L,
            {
                val rentableProduct = db1RaillyLinkerCompanyRentalProductRepository.findByUidAndRowDeleteDateStr(
                    rentableProductInfoUid,
                    "/"
                )

                if (rentableProduct == null) {
                    httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                    httpServletResponse.setHeader("api-result-code", "1")
                    return@tryLockRepeat
                }

                if (inputVo.maximumReservationUnitCount != null &&
                    (inputVo.maximumReservationUnitCount < 0 ||
                            rentableProduct.minimumReservationUnitCount > inputVo.maximumReservationUnitCount)
                ) {
                    httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                    httpServletResponse.setHeader("api-result-code", "2")
                    return@tryLockRepeat
                }

                rentableProduct.maximumReservationUnitCount = inputVo.maximumReservationUnitCount

                db1RaillyLinkerCompanyRentalProductRepository.save(rentableProduct)

                httpServletResponse.status = HttpStatus.OK.value()
                return@tryLockRepeat
            }
        )
    }


    // ----
    // (대여 가능 상품 이미지 등록 <ADMIN>)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun postRentalProductImage(
        httpServletResponse: HttpServletResponse,
        authorization: String,
        inputVo: RentalReservationAdminController.PostRentalProductImageInputVo
    ): RentalReservationAdminController.PostRentalProductImageOutputVo? {
//        val memberUid = jwtTokenUtil.getMemberUid(
//            authorization.split(" ")[1].trim(),
//            authTokenFilterTotalAuth.authJwtClaimsAes256InitializationVector,
//            authTokenFilterTotalAuth.authJwtClaimsAes256EncryptionKey
//        )

        val rentableProduct = db1RaillyLinkerCompanyRentalProductRepository.findByUidAndRowDeleteDateStr(
            inputVo.rentalProductUid,
            "/"
        )

        if (rentableProduct == null) {
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return null
        }

        // 저장된 상품 이미지 파일을 다운로드 할 수 있는 URL
        val savedProductImageUrl: String

        // 상품 이미지 파일 저장

        //----------------------------------------------------------------------------------------------------------
        // 상품 이미지를 서버 스토리지에 저장할 때 사용하는 방식
        // AWS S3 를 사용하거나 로컬 파일 스토리지 모듈을 사용하면 됩니다.
        // 파일 저장 기본 디렉토리 경로
        val saveDirectoryPath: Path =
            Paths.get("./by_product_files/service_rental_reservation/rentable_product/images")
                .toAbsolutePath().normalize()

        val savedFileName = customUtil.multipartFileLocalSave(
            saveDirectoryPath,
            null,
            inputVo.thumbnailImage
        )

        savedProductImageUrl = "${externalAccessAddress}/rental-reservation-admin/product-image/$savedFileName"
        //----------------------------------------------------------------------------------------------------------

        val productImage = db1RaillyLinkerCompanyRentalProductImageRepository.save(
            Db1_RaillyLinkerCompany_RentalProductImage(
                rentableProduct,
                savedProductImageUrl,
                inputVo.priority
            )
        )

        rentableProduct.versionSeq += 1
        db1RaillyLinkerCompanyRentalProductRepository.save(rentableProduct)

        httpServletResponse.status = HttpStatus.OK.value()
        return RentalReservationAdminController.PostRentalProductImageOutputVo(
            productImage.uid!!,
            savedProductImageUrl
        )
    }


    // ----
    // (대여 가능 상품 이미지 파일 다운받기)
    fun getProductImageFile(
        httpServletResponse: HttpServletResponse,
        fileName: String
    ): ResponseEntity<Resource>? {
        // 프로젝트 루트 경로 (프로젝트 settings.gradle 이 있는 경로)
        val projectRootAbsolutePathString: String = File("").absolutePath

        // 파일 절대 경로 및 파일명
        val serverFilePathObject =
            Paths.get("$projectRootAbsolutePathString/by_product_files/service_rental_reservation/rentable_product/images/$fileName")

        when {
            Files.isDirectory(serverFilePathObject) -> {
                // 파일이 디렉토리일때
                httpServletResponse.status = HttpStatus.NOT_FOUND.value()
                return null
            }

            Files.notExists(serverFilePathObject) -> {
                // 파일이 없을 때
                httpServletResponse.status = HttpStatus.NOT_FOUND.value()
                return null
            }
        }

        httpServletResponse.status = HttpStatus.OK.value()
        return ResponseEntity<Resource>(
            InputStreamResource(Files.newInputStream(serverFilePathObject)),
            HttpHeaders().apply {
                this.contentDisposition = ContentDisposition.builder("attachment")
                    .filename(fileName, StandardCharsets.UTF_8)
                    .build()
                this.add(HttpHeaders.CONTENT_TYPE, Files.probeContentType(serverFilePathObject))
            },
            HttpStatus.OK
        )
    }


    // ----
    // (대여 가능 상품 이미지 삭제 <ADMIN>)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun deleteRentalProductImage(
        httpServletResponse: HttpServletResponse,
        authorization: String,
        rentableProductImageUid: Long
    ) {
//        val memberUid = jwtTokenUtil.getMemberUid(
//            authorization.split(" ")[1].trim(),
//            authTokenFilterTotalAuth.authJwtClaimsAes256InitializationVector,
//            authTokenFilterTotalAuth.authJwtClaimsAes256EncryptionKey
//        )

        val rentableProductImage = db1RaillyLinkerCompanyRentalProductImageRepository.findByUidAndRowDeleteDateStr(
            rentableProductImageUid,
            "/"
        )

        if (rentableProductImage == null) {
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return
        }

        rentableProductImage.rowDeleteDateStr =
            LocalDateTime.now().atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy_MM_dd_'T'_HH_mm_ss_SSS_z"))

        db1RaillyLinkerCompanyRentalProductImageRepository.save(
            rentableProductImage
        )

        val rentableProduct = rentableProductImage.rentalProduct
        rentableProduct.versionSeq += 1
        db1RaillyLinkerCompanyRentalProductRepository.save(rentableProduct)

        // 필요하다면 이미지 삭제 처리 (상품 예약 정보 이미지에서 백업으로 사용중이라면 실제 파일 삭제는 하지 않도록 처리)

        httpServletResponse.status = HttpStatus.OK.value()
    }


    // ----
    // (대여 가능 상품 이미지 가중치 수정 <ADMIN>)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun patchRentalProductImagePriority(
        httpServletResponse: HttpServletResponse,
        authorization: String,
        rentalProductImageUid: Long,
        inputVo: RentalReservationAdminController.PatchRentalProductImagePriorityInputVo
    ) {
//        val memberUid = jwtTokenUtil.getMemberUid(
//            authorization.split(" ")[1].trim(),
//            authTokenFilterTotalAuth.authJwtClaimsAes256InitializationVector,
//            authTokenFilterTotalAuth.authJwtClaimsAes256EncryptionKey
//        )

        val productImage = db1RaillyLinkerCompanyRentalProductImageRepository.findByUidAndRowDeleteDateStr(
            rentalProductImageUid,
            "/"
        )

        if (productImage == null) {
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return
        }

        productImage.priority = inputVo.priority

        db1RaillyLinkerCompanyRentalProductImageRepository.save(productImage)

        httpServletResponse.status = HttpStatus.OK.value()
    }


    // ----
    // (대여 가능 상품 예약 정보의 예약 승인 처리 <ADMIN>)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun postReservationApprove(
        httpServletResponse: HttpServletResponse,
        authorization: String,
        rentableProductReservationInfoUid: Long,
        inputVo: RentalReservationAdminController.PostReservationApproveInputVo
    ): RentalReservationAdminController.PostReservationApproveOutputVo? {
//        val memberUid = jwtTokenUtil.getMemberUid(
//            authorization.split(" ")[1].trim(),
//            authTokenFilterTotalAuth.authJwtClaimsAes256InitializationVector,
//            authTokenFilterTotalAuth.authJwtClaimsAes256EncryptionKey
//        )
        // rentableProductReservationInfoUid 정보 존재 여부 확인
        val rentableProductReservationInfo =
            db1RaillyLinkerCompanyRentalProductReservationRepository.findByUidAndRowDeleteDateStr(
                rentableProductReservationInfoUid,
                "/"
            )

        if (rentableProductReservationInfo == null) {
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return null
        }

        // 예약 상태 확인
        val nowDatetime = LocalDateTime.now()

        val historyList =
            db1RaillyLinkerCompanyRentalProductReservationHistoryRepository.findAllByRentalProductReservationAndRowDeleteDateStrOrderByRowCreateDateDesc(
                rentableProductReservationInfo,
                "/"
            )

        var notPaid = true
        var paymentNotChecked = true

        var notApproved = true
        var approveNotChecked = true
        for (history in historyList) {
            when (history.historyCode.toInt()) {
                1 -> {
                    // 예약 신청 거부 내역 있음 -> return
                    httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                    httpServletResponse.setHeader("api-result-code", "3")
                    return null
                }

                2 -> {
                    // 승인
                    if (approveNotChecked) {
                        notApproved = false
                        approveNotChecked = false
                    }
                }

                3 -> {
                    // 승인 취소
                    if (approveNotChecked) {
                        approveNotChecked = false
                    }
                }

                6 -> {
                    // 예약 취소 승인 내역 있음 -> return
                    httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                    httpServletResponse.setHeader("api-result-code", "2")
                    return null
                }

                8 -> {
                    // 결제 확인
                    if (paymentNotChecked) {
                        notPaid = false
                        paymentNotChecked = false
                    }
                }

                9 -> {
                    // 결제 확인 취소
                    if (paymentNotChecked) {
                        paymentNotChecked = false
                    }
                }
            }
        }

        if (!notApproved) {
            // 예약 승인 내역 있음 -> return
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "4")
            return null
        }

        if (notPaid && nowDatetime.isAfter(rentableProductReservationInfo.paymentCheckDeadlineDatetime)) {
            // 미결제 상태 & 결제 기한 초과 상태(= 취소와 동일) -> return
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "5")
            return null
        }

        // 예약 히스토리에 정보 기입
        val newReservationStateChangeHistory =
            db1RaillyLinkerCompanyRentalProductReservationHistoryRepository.save(
                Db1_RaillyLinkerCompany_RentalProductReservationHistory(
                    rentableProductReservationInfo,
                    2,
                    inputVo.stateChangeDesc
                )
            )

        httpServletResponse.status = HttpStatus.OK.value()
        return RentalReservationAdminController.PostReservationApproveOutputVo(
            newReservationStateChangeHistory.uid!!
        )
    }


    // ----
    // (예약 신청 승인 취소 <ADMIN>)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun postReservationApproveCancel(
        httpServletResponse: HttpServletResponse,
        authorization: String,
        rentalProductReservationUid: Long,
        inputVo: RentalReservationAdminController.PostReservationApproveCancelInputVo
    ): RentalReservationAdminController.PostReservationApproveCancelOutputVo? {
//        val memberUid = jwtTokenUtil.getMemberUid(
//            authorization.split(" ")[1].trim(),
//            authTokenFilterTotalAuth.authJwtClaimsAes256InitializationVector,
//            authTokenFilterTotalAuth.authJwtClaimsAes256EncryptionKey
//        )
        // rentableProductReservationInfoUid 정보 존재 여부 확인
        val rentableProductReservationInfo =
            db1RaillyLinkerCompanyRentalProductReservationRepository.findByUidAndRowDeleteDateStr(
                rentalProductReservationUid,
                "/"
            )

        if (rentableProductReservationInfo == null) {
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return null
        }

        // 예약 상태 확인
        val nowDatetime = LocalDateTime.now()

        if (nowDatetime.isAfter(rentableProductReservationInfo.approvalDeadlineDatetime)) {
            // 예약 승인 기한 초과(거부 권한이 없음) -> return
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "6")
            return null
        }

        val historyList =
            db1RaillyLinkerCompanyRentalProductReservationHistoryRepository.findAllByRentalProductReservationAndRowDeleteDateStrOrderByRowCreateDateDesc(
                rentableProductReservationInfo,
                "/"
            )

        var notPaid = true
        var paymentNotChecked = true

        var notApproved = true
        var notApproveCancel = true
        var approveNotChecked = true
        for (history in historyList) {
            when (history.historyCode.toInt()) {
                1 -> {
                    // 예약 신청 거부 내역 있음 -> return
                    httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                    httpServletResponse.setHeader("api-result-code", "3")
                    return null
                }

                2 -> {
                    // 승인
                    if (approveNotChecked) {
                        approveNotChecked = false
                        notApproved = false
                    }
                }

                3 -> {
                    // 승인 취소
                    if (approveNotChecked) {
                        approveNotChecked = false
                        notApproveCancel = false
                    }
                }

                6 -> {
                    // 예약 취소 승인 내역 있음 -> return
                    httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                    httpServletResponse.setHeader("api-result-code", "2")
                    return null
                }

                8 -> {
                    // 결제 확인
                    if (paymentNotChecked) {
                        notPaid = false
                        paymentNotChecked = false
                    }
                }

                9 -> {
                    // 결제 확인 취소
                    if (paymentNotChecked) {
                        paymentNotChecked = false
                    }
                }
            }
        }

        if (notPaid && nowDatetime.isAfter(rentableProductReservationInfo.paymentCheckDeadlineDatetime)) {
            // 미결제 상태 & 결제 기한 초과 상태(= 취소와 동일) -> return
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "5")
            return null
        }

        if (!notApproveCancel) {
            // 예약 승인 취소 내역 있음 -> return
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "4")
            return null
        }

        if (notApproved) {
            // 예약 승인 내역이 없음 -> return
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "7")
            return null
        }

        // 예약 히스토리에 정보 기입
        val newReservationStateChangeHistory =
            db1RaillyLinkerCompanyRentalProductReservationHistoryRepository.save(
                Db1_RaillyLinkerCompany_RentalProductReservationHistory(
                    rentableProductReservationInfo,
                    3,
                    inputVo.stateChangeDesc
                )
            )

        httpServletResponse.status = HttpStatus.OK.value()
        return RentalReservationAdminController.PostReservationApproveCancelOutputVo(
            newReservationStateChangeHistory.uid!!
        )
    }


    // ----
    // (대여 가능 상품 예약 정보의 예약 거부 처리 <ADMIN>)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun postReservationReject(
        httpServletResponse: HttpServletResponse,
        authorization: String,
        rentableProductReservationInfoUid: Long,
        inputVo: RentalReservationAdminController.PostReservationRejectInputVo
    ): RentalReservationAdminController.PostReservationRejectOutputVo? {
//        val memberUid = jwtTokenUtil.getMemberUid(
//            authorization.split(" ")[1].trim(),
//            authTokenFilterTotalAuth.authJwtClaimsAes256InitializationVector,
//            authTokenFilterTotalAuth.authJwtClaimsAes256EncryptionKey
//        )
        // rentableProductReservationInfoUid 정보 존재 여부 확인
        val rentableProductReservationInfo =
            db1RaillyLinkerCompanyRentalProductReservationRepository.findByUidAndRowDeleteDateStr(
                rentableProductReservationInfoUid,
                "/"
            )

        if (rentableProductReservationInfo == null) {
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return null
        }

        // 상태 확인
        val nowDatetime = LocalDateTime.now()

        if (nowDatetime.isAfter(rentableProductReservationInfo.approvalDeadlineDatetime)) {
            // 예약 승인 기한을 넘김
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "2")
            return null
        }

        val historyList =
            db1RaillyLinkerCompanyRentalProductReservationHistoryRepository.findAllByRentalProductReservationAndRowDeleteDateStrOrderByRowCreateDateDesc(
                rentableProductReservationInfo,
                "/"
            )

        var notPaid = true
        var paymentNotChecked = true

        var notApproved = true
//        var notApproveCancel = true
        var approveNotChecked = true
        for (history in historyList) {
            when (history.historyCode.toInt()) {
                1 -> {
                    // 예약 신청 거부 내역 있음 -> return
                    httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                    httpServletResponse.setHeader("api-result-code", "4")
                    return null
                }

                2 -> {
                    // 승인
                    if (approveNotChecked) {
                        approveNotChecked = false
                        notApproved = false
                    }
                }

                3 -> {
                    // 승인 취소
                    if (approveNotChecked) {
                        approveNotChecked = false
//                        notApproveCancel = false
                    }
                }

                6 -> {
                    // 예약 취소 승인 내역 있음 -> return
                    httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                    httpServletResponse.setHeader("api-result-code", "3")
                    return null
                }

                8 -> {
                    // 결제 확인
                    if (paymentNotChecked) {
                        notPaid = false
                        paymentNotChecked = false
                    }
                }

                9 -> {
                    // 결제 확인 취소
                    if (paymentNotChecked) {
                        paymentNotChecked = false
                    }
                }
            }
        }

        if (!notApproved) {
            // 예약 승인 내역 있음 -> return
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "5")
            return null
        }

        if (notPaid && nowDatetime.isAfter(rentableProductReservationInfo.paymentCheckDeadlineDatetime)) {
            // 미결제 상태 & 결제 기한 초과 상태(= 취소와 동일) -> return
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "6")
            return null
        }

        // 예약 히스토리에 정보 기입
        val newReservationStateChangeHistory =
            db1RaillyLinkerCompanyRentalProductReservationHistoryRepository.save(
                Db1_RaillyLinkerCompany_RentalProductReservationHistory(
                    rentableProductReservationInfo,
                    1,
                    inputVo.stateChangeDesc
                )
            )

        httpServletResponse.status = HttpStatus.OK.value()
        return RentalReservationAdminController.PostReservationRejectOutputVo(
            newReservationStateChangeHistory.uid!!
        )
    }


    // ----
    // (대여 가능 상품 예약 정보의 예약 취소 승인 처리 <ADMIN>)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun postReservationCancelApprove(
        httpServletResponse: HttpServletResponse,
        authorization: String,
        rentableProductReservationInfoUid: Long,
        inputVo: RentalReservationAdminController.PostReservationCancelApproveInputVo
    ): RentalReservationAdminController.PostReservationCancelApproveOutputVo? {
//        val memberUid = jwtTokenUtil.getMemberUid(
//            authorization.split(" ")[1].trim(),
//            authTokenFilterTotalAuth.authJwtClaimsAes256InitializationVector,
//            authTokenFilterTotalAuth.authJwtClaimsAes256EncryptionKey
//        )
        // rentableProductReservationInfoUid 정보 존재 여부 확인
        val rentableProductReservationInfo =
            db1RaillyLinkerCompanyRentalProductReservationRepository.findByUidAndRowDeleteDateStr(
                rentableProductReservationInfoUid,
                "/"
            )

        if (rentableProductReservationInfo == null) {
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return null
        }

        // 상태 확인
        val nowDatetime = LocalDateTime.now()

        if (nowDatetime.isAfter(rentableProductReservationInfo.rentalStartDatetime)) {
            // 대여 시작 기한 초과 -> return
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "2")
            return null
        }

        val historyList =
            db1RaillyLinkerCompanyRentalProductReservationHistoryRepository.findAllByRentalProductReservationAndRowDeleteDateStrOrderByRowCreateDateDesc(
                rentableProductReservationInfo,
                "/"
            )

        if (historyList.isEmpty()) {
            // 히스토리가 없음 = 결제 대기 상태
            // 예약 취소 신청 내역이 없음 -> return
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "6")
            return null
        }

        var notPaid = true
        var paymentNotChecked = true
        var noRequestCancel = true
//        var notRequestCancel = true
        var notRequestCancelDeny = true
        var notRequestCancelCancel = true
        var notCancelChecked = true
        for (history in historyList) {
            when (history.historyCode.toInt()) {
                1 -> {
                    // 예약 신청 거부 내역 있음 -> return
                    httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                    httpServletResponse.setHeader("api-result-code", "4")
                    return null
                }

                4 -> {
                    // 예약 취소 신청
                    noRequestCancel = false

                    if (notCancelChecked) {
                        notCancelChecked = false
//                        // 예약 취소 거부 내역이 최신인지
//                        notRequestCancel = false
                    }
                }

                5 -> {
                    // 예약 취소 신청 취소
                    if (notCancelChecked) {
                        notCancelChecked = false
                        // 예약 취소 거부 내역이 최신인지
                        notRequestCancelCancel = false
                    }
                }

                6 -> {
                    // 예약 취소 승인 내역 있음 -> return
                    httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                    httpServletResponse.setHeader("api-result-code", "3")
                    return null
                }

                7 -> {
                    // 예약 취소 거부
                    if (notCancelChecked) {
                        notCancelChecked = false
                        // 예약 취소 거부 내역이 최신인지
                        notRequestCancelDeny = false
                    }
                }

                8 -> {
                    // 결제 확인
                    if (paymentNotChecked) {
                        notPaid = false
                        paymentNotChecked = false
                    }
                }

                9 -> {
                    // 결제 확인 취소
                    if (paymentNotChecked) {
                        paymentNotChecked = false
                    }
                }
            }
        }

        if (notPaid && nowDatetime.isAfter(rentableProductReservationInfo.paymentCheckDeadlineDatetime)) {
            // 미결제 상태 & 결제 기한 초과 상태(= 취소와 동일) -> return
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "5")
            return null
        }

        if (noRequestCancel) {
            // 예약 취소 신청 내역이 없음 -> return
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "6")
            return null
        }

        if (!notRequestCancelDeny) {
            // 기존 예약 취소 신청에 대한 예약 취소 거부 상태입니다. -> return
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "7")
            return null
        }

        if (!notRequestCancelCancel) {
            // 기존 예약 취소 신청에 대한 취소 상태입니다. -> return
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "8")
            return null
        }

        // 예약 히스토리에 정보 기입
        val newReservationStateChangeHistory =
            db1RaillyLinkerCompanyRentalProductReservationHistoryRepository.save(
                Db1_RaillyLinkerCompany_RentalProductReservationHistory(
                    rentableProductReservationInfo,
                    6,
                    inputVo.stateChangeDesc
                )
            )

        httpServletResponse.status = HttpStatus.OK.value()
        return RentalReservationAdminController.PostReservationCancelApproveOutputVo(
            newReservationStateChangeHistory.uid!!
        )
    }


    // ----
    // (대여 가능 상품 예약 정보의 예약 취소 거부 처리 <ADMIN>)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun postReservationCancelReject(
        httpServletResponse: HttpServletResponse,
        authorization: String,
        rentableProductReservationInfoUid: Long,
        inputVo: RentalReservationAdminController.PostReservationCancelRejectInputVo
    ): RentalReservationAdminController.PostReservationCancelRejectOutputVo? {
//        val memberUid = jwtTokenUtil.getMemberUid(
//            authorization.split(" ")[1].trim(),
//            authTokenFilterTotalAuth.authJwtClaimsAes256InitializationVector,
//            authTokenFilterTotalAuth.authJwtClaimsAes256EncryptionKey
//        )
        // rentableProductReservationInfoUid 정보 존재 여부 확인
        val rentableProductReservationInfo =
            db1RaillyLinkerCompanyRentalProductReservationRepository.findByUidAndRowDeleteDateStr(
                rentableProductReservationInfoUid,
                "/"
            )

        if (rentableProductReservationInfo == null) {
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return null
        }

        // 상태 확인
        val nowDatetime = LocalDateTime.now()

        if (nowDatetime.isAfter(rentableProductReservationInfo.rentalStartDatetime)) {
            // 대여 시작 기한 초과 -> return
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "2")
            return null
        }

        val historyList =
            db1RaillyLinkerCompanyRentalProductReservationHistoryRepository.findAllByRentalProductReservationAndRowDeleteDateStrOrderByRowCreateDateDesc(
                rentableProductReservationInfo,
                "/"
            )

        if (historyList.isEmpty()) {
            // 결제 대기 상태
            // 예약 취소 신청 내역이 없음 -> return
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "6")
            return null
        }

        var notPaid = true
        var paymentNotChecked = true
        var noRequestCancel = true
//        var notRequestCancel = true
        var notRequestCancelDeny = true
        var notRequestCancelCancel = true
        var notCancelChecked = true
        for (history in historyList) {
            when (history.historyCode.toInt()) {
                1 -> {
                    // 예약 신청 거부 내역 있음 -> return
                    httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                    httpServletResponse.setHeader("api-result-code", "4")
                    return null
                }

                4 -> {
                    // 예약 취소 신청
                    noRequestCancel = false

                    if (notCancelChecked) {
                        notCancelChecked = false
//                        // 예약 취소 거부 내역이 최신인지
//                        notRequestCancel = false
                    }
                }

                5 -> {
                    // 예약 취소 신청 취소
                    if (notCancelChecked) {
                        notCancelChecked = false
                        // 예약 취소 거부 내역이 최신인지
                        notRequestCancelCancel = false
                    }
                }

                6 -> {
                    // 예약 취소 승인 내역 있음 -> return
                    httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                    httpServletResponse.setHeader("api-result-code", "3")
                    return null
                }

                7 -> {
                    // 예약 취소 거부
                    if (notCancelChecked) {
                        notCancelChecked = false
                        // 예약 취소 거부 내역이 최신인지
                        notRequestCancelDeny = false
                    }
                }

                8 -> {
                    // 결제 확인
                    if (paymentNotChecked) {
                        notPaid = false
                        paymentNotChecked = false
                    }
                }

                9 -> {
                    // 결제 확인 취소
                    if (paymentNotChecked) {
                        paymentNotChecked = false
                    }
                }
            }
        }

        if (notPaid && nowDatetime.isAfter(rentableProductReservationInfo.paymentCheckDeadlineDatetime)) {
            // 미결제 상태 & 결제 기한 초과 상태(= 취소와 동일) -> return
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "5")
            return null
        }

        if (noRequestCancel) {
            // 예약 취소 신청 내역이 없음 -> return
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "6")
            return null
        }

        if (!notRequestCancelDeny) {
            // 기존 예약 취소 신청에 대한 예약 취소 거부 상태입니다. -> return
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "7")
            return null
        }

        if (!notRequestCancelCancel) {
            // 기존 예약 취소 신청에 대한 취소 상태입니다. -> return
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "8")
            return null
        }

        // 예약 히스토리에 정보 기입
        val newReservationStateChangeHistory =
            db1RaillyLinkerCompanyRentalProductReservationHistoryRepository.save(
                Db1_RaillyLinkerCompany_RentalProductReservationHistory(
                    rentableProductReservationInfo,
                    7,
                    inputVo.stateChangeDesc
                )
            )

        httpServletResponse.status = HttpStatus.OK.value()
        return RentalReservationAdminController.PostReservationCancelRejectOutputVo(
            newReservationStateChangeHistory.uid!!
        )
    }


    // ----
    // (대여 가능 상품 예약 정보의 결제 확인 처리 <ADMIN>)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun postRentableProductReservationInfoPaymentComplete(
        httpServletResponse: HttpServletResponse,
        authorization: String,
        rentableProductReservationInfoUid: Long,
        inputVo: RentalReservationAdminController.PostRentableProductReservationInfoPaymentCompleteInputVo
    ): RentalReservationAdminController.PostRentableProductReservationInfoPaymentCompleteOutputVo? {
//        val memberUid = jwtTokenUtil.getMemberUid(
//            authorization.split(" ")[1].trim(),
//            authTokenFilterTotalAuth.authJwtClaimsAes256InitializationVector,
//            authTokenFilterTotalAuth.authJwtClaimsAes256EncryptionKey
//        )
        // rentableProductReservationInfoUid 정보 존재 여부 확인
        val rentableProductReservationInfo =
            db1RaillyLinkerCompanyRentalProductReservationRepository.findByUidAndRowDeleteDateStr(
                rentableProductReservationInfoUid,
                "/"
            )

        if (rentableProductReservationInfo == null) {
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return null
        }

        // 상태 확인
        val nowDatetime = LocalDateTime.now()

        if (nowDatetime.isAfter(rentableProductReservationInfo.paymentCheckDeadlineDatetime)) {
            // 결제 확인 기한 초과 -> return
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "3")
            return null
        }

        val historyList =
            db1RaillyLinkerCompanyRentalProductReservationHistoryRepository.findAllByRentalProductReservationAndRowDeleteDateStrOrderByRowCreateDateDesc(
                rentableProductReservationInfo,
                "/"
            )

        var notPaid = true
        var paymentNotChecked = true
        for (history in historyList) {
            when (history.historyCode.toInt()) {
                8 -> {
                    // 결제 확인
                    if (paymentNotChecked) {
                        notPaid = false
                        paymentNotChecked = false
                    }
                }

                9 -> {
                    // 결제 확인 취소
                    if (paymentNotChecked) {
                        paymentNotChecked = false
                    }
                }
            }
        }

        if (!notPaid) {
            // 결제 확인 내역 있음 -> return
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "2")
            return null
        }

        // 예약 히스토리에 정보 기입
        val newReservationStateChangeHistory =
            db1RaillyLinkerCompanyRentalProductReservationHistoryRepository.save(
                Db1_RaillyLinkerCompany_RentalProductReservationHistory(
                    rentableProductReservationInfo,
                    8,
                    inputVo.stateChangeDesc
                )
            )

        httpServletResponse.status = HttpStatus.OK.value()
        return RentalReservationAdminController.PostRentableProductReservationInfoPaymentCompleteOutputVo(
            newReservationStateChangeHistory.uid!!
        )
    }


    // ----
    // (결제 확인 취소 <ADMIN>)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun postRentableProductReservationInfoPaymentCompleteCancel(
        httpServletResponse: HttpServletResponse,
        authorization: String,
        rentalProductReservationUid: Long,
        inputVo: RentalReservationAdminController.PostRentableProductReservationInfoPaymentCompleteCancelInputVo
    ): RentalReservationAdminController.PostRentableProductReservationInfoPaymentCompleteCancelOutputVo? {
//        val memberUid = jwtTokenUtil.getMemberUid(
//            authorization.split(" ")[1].trim(),
//            authTokenFilterTotalAuth.authJwtClaimsAes256InitializationVector,
//            authTokenFilterTotalAuth.authJwtClaimsAes256EncryptionKey
//        )
        // rentableProductReservationInfoUid 정보 존재 여부 확인
        val rentableProductReservationInfo =
            db1RaillyLinkerCompanyRentalProductReservationRepository.findByUidAndRowDeleteDateStr(
                rentalProductReservationUid,
                "/"
            )

        if (rentableProductReservationInfo == null) {
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return null
        }

        // 상태 확인
        val nowDatetime = LocalDateTime.now()

        if (nowDatetime.isAfter(rentableProductReservationInfo.paymentCheckDeadlineDatetime)) {
            // 결제 확인 기한 초과 -> return
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "3")
            return null
        }

        val historyList =
            db1RaillyLinkerCompanyRentalProductReservationHistoryRepository.findAllByRentalProductReservationAndRowDeleteDateStrOrderByRowCreateDateDesc(
                rentableProductReservationInfo,
                "/"
            )

        var notPaidCancel = true
        var paymentNotChecked = true
        for (history in historyList) {
            when (history.historyCode.toInt()) {
                8 -> {
                    // 결제 확인
                    if (paymentNotChecked) {
                        paymentNotChecked = false
                    }
                }

                9 -> {
                    // 결제 확인 취소
                    if (paymentNotChecked) {
                        paymentNotChecked = false
                        notPaidCancel = false
                    }
                }
            }
        }

        if (!notPaidCancel) {
            // 결제 확인 취소 내역 있음 -> return
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "2")
            return null
        }

        if (rentableProductReservationInfo.reservationUnitPrice == BigDecimal(0L)) {
            // 결제 가격이 0원인 상품은 결제 확인을 취소할 수 없습니다.
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "4")
            return null
        }

        // 예약 히스토리에 정보 기입
        val newReservationStateChangeHistory =
            db1RaillyLinkerCompanyRentalProductReservationHistoryRepository.save(
                Db1_RaillyLinkerCompany_RentalProductReservationHistory(
                    rentableProductReservationInfo,
                    9,
                    inputVo.stateChangeDesc
                )
            )

        httpServletResponse.status = HttpStatus.OK.value()
        return RentalReservationAdminController.PostRentableProductReservationInfoPaymentCompleteCancelOutputVo(
            newReservationStateChangeHistory.uid!!
        )
    }


    // ----
    // (개별 상품 반납 확인 <ADMIN>)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun postRentableProductStockReservationInfoReturnCheck(
        httpServletResponse: HttpServletResponse,
        authorization: String,
        rentableProductReservationInfoUid: Long,
        inputVo: RentalReservationAdminController.PostRentableProductStockReservationInfoReturnCheckInputVo
    ): RentalReservationAdminController.PostRentableProductStockReservationInfoReturnCheckOutputVo? {
//        val memberUid = jwtTokenUtil.getMemberUid(
//            authorization.split(" ")[1].trim(),
//            authTokenFilterTotalAuth.authJwtClaimsAes256InitializationVector,
//            authTokenFilterTotalAuth.authJwtClaimsAes256EncryptionKey
//        )
        // rentableProductReservationInfoUid 정보 존재 여부 확인
        val rentableProductReservationInfo =
            db1RaillyLinkerCompanyRentalProductReservationRepository.findByUidAndRowDeleteDateStr(
                rentableProductReservationInfoUid,
                "/"
            )

        if (rentableProductReservationInfo == null) {
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return null
        }

        val reservationHistoryList =
            db1RaillyLinkerCompanyRentalProductReservationHistoryRepository.findAllByRentalProductReservationAndRowDeleteDateStrOrderByRowCreateDateDesc(
                rentableProductReservationInfo,
                "/"
            )

        var notPaid = true
        var paymentNotChecked = true
        var noEarlyReturn = true
        var noEarlyReturnCancel = true
        for (history in reservationHistoryList) {
            when (history.historyCode.toInt()) {
                1 -> {
                    // 관리자 예약 신청 거부
                    httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                    httpServletResponse.setHeader("api-result-code", "2")
                    return null
                }

                8 -> {
                    // 결제 확인
                    if (paymentNotChecked) {
                        notPaid = false
                        paymentNotChecked = false
                    }
                }

                9 -> {
                    // 결제 확인 취소
                    if (paymentNotChecked) {
                        paymentNotChecked = false
                    }
                }

                10 -> {
                    // 사용자 조기 반납 신고
                    if (noEarlyReturnCancel) {
                        noEarlyReturn = false
                    }
                }

                11 -> {
                    // 사용자 조기 반납 신고 취소
                    if (noEarlyReturn) {
                        noEarlyReturnCancel = false
                    }
                }

                12 -> {
                    // 반납 확인
                    httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                    httpServletResponse.setHeader("api-result-code", "4")
                    return null
                }
            }
        }

        if (notPaid) {
            // 결제 확인 완료 아님 = 대여 진행 상태가 아님
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "5")
            return null
        }

        // 상태 확인
        val nowDatetime = LocalDateTime.now()

        if (((noEarlyReturn && noEarlyReturnCancel) || !noEarlyReturnCancel) &&
            nowDatetime.isBefore(rentableProductReservationInfo.rentalEndDatetime)
        ) {
            // 조기 반납 신고 상태가 아니고(내역이 없거나 취소 상태), 상품 반납일도 안됨
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "2")
            return null
        }

        // 개별 상품 반납 확인 내역 추가
        val newReservationStateChangeHistory =
            db1RaillyLinkerCompanyRentalProductReservationHistoryRepository.save(
                Db1_RaillyLinkerCompany_RentalProductReservationHistory(
                    rentableProductReservationInfo,
                    12,
                    inputVo.stateChangeDesc
                )
            )

        httpServletResponse.status = HttpStatus.OK.value()
        return RentalReservationAdminController.PostRentableProductStockReservationInfoReturnCheckOutputVo(
            newReservationStateChangeHistory.uid!!
        )
    }


    // ----
    // (개별 상품 준비 완료 일시 설정 <ADMIN>)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun patchRentableProductStockReservationInfoReady(
        httpServletResponse: HttpServletResponse,
        authorization: String,
        rentableProductReservationInfoUid: Long,
        inputVo: RentalReservationAdminController.PatchRentableProductStockReservationInfoReadyInputVo
    ) {
//        val memberUid = jwtTokenUtil.getMemberUid(
//            authorization.split(" ")[1].trim(),
//            authTokenFilterTotalAuth.authJwtClaimsAes256InitializationVector,
//            authTokenFilterTotalAuth.authJwtClaimsAes256EncryptionKey
//        )
        // rentableProductReservationInfoUid 정보 존재 여부 확인
        val rentableProductReservationInfo =
            db1RaillyLinkerCompanyRentalProductReservationRepository.findByUidAndRowDeleteDateStr(
                rentableProductReservationInfoUid,
                "/"
            )

        if (rentableProductReservationInfo == null) {
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return
        }

        val anchorDatetime =
            if (inputVo.readyDatetime == null) {
                null
            } else {
                ZonedDateTime.parse(
                    inputVo.readyDatetime,
                    DateTimeFormatter.ofPattern("yyyy_MM_dd_'T'_HH_mm_ss_z")
                ).withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()
            }

        // 개별 상품 반납 확인 내역 추가
        rentableProductReservationInfo.productReadyDatetime = anchorDatetime
        db1RaillyLinkerCompanyRentalProductReservationRepository.save(rentableProductReservationInfo)

        httpServletResponse.status = HttpStatus.OK.value()
    }


    // ----
    // (개별 상품 연체 상태 변경 <ADMIN>)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun postRentableProductStockReservationInfoOverdue(
        httpServletResponse: HttpServletResponse,
        authorization: String,
        rentableProductReservationInfoUid: Long,
        inputVo: RentalReservationAdminController.PostRentableProductStockReservationInfoOverdueInputVo
    ): RentalReservationAdminController.PostRentableProductStockReservationInfoOverdueOutputVo? {
//        val memberUid = jwtTokenUtil.getMemberUid(
//            authorization.split(" ")[1].trim(),
//            authTokenFilterTotalAuth.authJwtClaimsAes256InitializationVector,
//            authTokenFilterTotalAuth.authJwtClaimsAes256EncryptionKey
//        )
        val rentableProductReservationInfo =
            db1RaillyLinkerCompanyRentalProductReservationRepository.findByUidAndRowDeleteDateStr(
                rentableProductReservationInfoUid,
                "/"
            )

        if (rentableProductReservationInfo == null) {
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return null
        }

        val reservationHistoryList =
            db1RaillyLinkerCompanyRentalProductReservationHistoryRepository.findAllByRentalProductReservationAndRowDeleteDateStrOrderByRowCreateDateDesc(
                rentableProductReservationInfo,
                "/"
            )
        var notPaid = true
        var paymentNotChecked = true

        var noOverdue = true
        var noOverdueCancel = true
        for (history in reservationHistoryList) {
            when (history.historyCode.toInt()) {
                1 -> {
                    // 관리자 예약 신청 거부
                    httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                    httpServletResponse.setHeader("api-result-code", "2")
                    return null
                }

                8 -> {
                    // 결제 확인
                    if (paymentNotChecked) {
                        notPaid = false
                        paymentNotChecked = false
                    }
                }

                9 -> {
                    // 결제 확인 취소
                    if (paymentNotChecked) {
                        paymentNotChecked = false
                    }
                }

                12 -> {
                    // 반납 확인 상태
                    httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                    httpServletResponse.setHeader("api-result-code", "5")
                    return null
                }

                13 -> {
                    // 연체 상태
                    if (noOverdueCancel) {
                        noOverdue = false
                    }
                }

                14 -> {
                    // 연체 설정 취소
                    if (noOverdue) {
                        noOverdueCancel = false
                    }
                }
            }
        }
        if (notPaid) {
            // 결제 확인 완료 아님 = 대여 진행 상태가 아님
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "6")
            return null
        }

        if (!noOverdue) {
            // 연체 상태입니다.
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "4")
            return null
        }

        // 상태 확인
        val nowDatetime = LocalDateTime.now()

        if (nowDatetime.isBefore(rentableProductReservationInfo.rentalEndDatetime)) {
            // 상품 반납일을 넘지 않음
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "3")
            return null
        }

        // 개별 상품 반납 확인 내역 추가
        val newReservationStateChangeHistory =
            db1RaillyLinkerCompanyRentalProductReservationHistoryRepository.save(
                Db1_RaillyLinkerCompany_RentalProductReservationHistory(
                    rentableProductReservationInfo,
                    13,
                    inputVo.stateChangeDesc
                )
            )

        httpServletResponse.status = HttpStatus.OK.value()
        return RentalReservationAdminController.PostRentableProductStockReservationInfoOverdueOutputVo(
            newReservationStateChangeHistory.uid!!
        )
    }


    // ----
    // (개별 상품 연체 상태 변경 취소 <ADMIN>)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun postRentableProductStockReservationInfoOverdueCancel(
        httpServletResponse: HttpServletResponse,
        authorization: String,
        rentableProductReservationInfoUid: Long,
        inputVo: RentalReservationAdminController.PostRentableProductStockReservationInfoOverdueCancelInputVo
    ): RentalReservationAdminController.PostRentableProductStockReservationInfoOverdueCancelOutputVo? {
//        val memberUid = jwtTokenUtil.getMemberUid(
//            authorization.split(" ")[1].trim(),
//            authTokenFilterTotalAuth.authJwtClaimsAes256InitializationVector,
//            authTokenFilterTotalAuth.authJwtClaimsAes256EncryptionKey
//        )
        val rentableProductReservationInfo =
            db1RaillyLinkerCompanyRentalProductReservationRepository.findByUidAndRowDeleteDateStr(
                rentableProductReservationInfoUid,
                "/"
            )

        if (rentableProductReservationInfo == null) {
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return null
        }

        val reservationHistoryList =
            db1RaillyLinkerCompanyRentalProductReservationHistoryRepository.findAllByRentalProductReservationAndRowDeleteDateStrOrderByRowCreateDateDesc(
                rentableProductReservationInfo,
                "/"
            )

        var notPaid = true
        var paymentNotChecked = true

        var noOverdue = true
        var noOverdueCancel = true
        for (history in reservationHistoryList) {
            when (history.historyCode.toInt()) {
                1 -> {
                    // 관리자 예약 신청 거부
                    httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                    httpServletResponse.setHeader("api-result-code", "2")
                    return null
                }

                8 -> {
                    // 결제 확인
                    if (paymentNotChecked) {
                        notPaid = false
                        paymentNotChecked = false
                    }
                }

                9 -> {
                    // 결제 확인 취소
                    if (paymentNotChecked) {
                        paymentNotChecked = false
                    }
                }

                13 -> {
                    // 연체 상태
                    if (noOverdueCancel) {
                        noOverdue = false
                    }
                }

                14 -> {
                    // 연체 설정 취소
                    if (noOverdue) {
                        noOverdueCancel = false
                    }
                }
            }
        }
        if (notPaid) {
            // 결제 확인 완료 아님 || 예약 신청 거부 = 대여 진행 상태가 아님
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "5")
            return null
        }

        if (noOverdue && noOverdueCancel) {
            // 연체 상태가 아님
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "3")
            return null
        }

        if (!noOverdueCancel) {
            // 연체 상태 변경 취소 상태입니다.
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "4")
            return null
        }

        // 개별 상품 반납 확인 내역 추가
        val newReservationStateChangeHistory =
            db1RaillyLinkerCompanyRentalProductReservationHistoryRepository.save(
                Db1_RaillyLinkerCompany_RentalProductReservationHistory(
                    rentableProductReservationInfo,
                    14,
                    inputVo.stateChangeDesc
                )
            )

        httpServletResponse.status = HttpStatus.OK.value()
        return RentalReservationAdminController.PostRentableProductStockReservationInfoOverdueCancelOutputVo(
            newReservationStateChangeHistory.uid!!
        )
    }


    // ----
    // (대여 가능 상품 예약 상태 테이블의 상세 설명 수정 <ADMIN>)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun patchReservationStateChangeHistoryStateChangeDesc(
        httpServletResponse: HttpServletResponse,
        authorization: String,
        reservationStateChangeHistoryUid: Long,
        inputVo: RentalReservationAdminController.PatchReservationStateChangeHistoryStateChangeDescInputVo
    ) {
//        val memberUid = jwtTokenUtil.getMemberUid(
//            authorization.split(" ")[1].trim(),
//            authTokenFilterTotalAuth.authJwtClaimsAes256InitializationVector,
//            authTokenFilterTotalAuth.authJwtClaimsAes256EncryptionKey
//        )

        val reservationStateChangeHistory =
            db1RaillyLinkerCompanyRentalProductReservationHistoryRepository.findByUidAndRowDeleteDateStr(
                reservationStateChangeHistoryUid,
                "/"
            )

        if (reservationStateChangeHistory == null) {
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return
        }

        reservationStateChangeHistory.historyDesc = inputVo.historyDesc
        db1RaillyLinkerCompanyRentalProductReservationHistoryRepository.save(reservationStateChangeHistory)

        httpServletResponse.status = HttpStatus.OK.value()
    }


    // ----
    // (예약 취소 신청 <>)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun postCancelProductReservation(
        httpServletResponse: HttpServletResponse,
        authorization: String,
        rentalProductReservationUid: Long,
        inputVo: RentalReservationAdminController.PostCancelProductReservationInputVo
    ): RentalReservationAdminController.PostCancelProductReservationOutputVo? {
        val reservationEntity: Db1_RaillyLinkerCompany_RentalProductReservation? =
            db1RaillyLinkerCompanyRentableProductReservationInfoRepository.findByUidAndRowDeleteDateStr(
                rentalProductReservationUid,
                "/"
            )

        if (reservationEntity == null) {
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return null
        }

        val nowDatetime = LocalDateTime.now()

        // 예약 취소 신청 가능 상태 확인
        if (nowDatetime.isAfter(reservationEntity.cancelDeadlineDatetime)) {
            // 예약 취소 가능 기한 초과 -> return
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "2")
            return null
        }

        val historyList =
            db1RaillyLinkerCompanyRentableProductReservationStateChangeHistoryRepository.findAllByRentalProductReservationAndRowDeleteDateStrOrderByRowCreateDateDesc(
                reservationEntity,
                "/"
            )

        var notPaid = true
        var paymentNotChecked = true


        var notRequestCancel = true
//        var notRequestCancelDeny = true
//        var notRequestCancelCancel = true
        var notCancelChecked = true

        var notApproved = true
//        var notApproveCancel = true
        var approveNotChecked = true
        for (history in historyList) {
            when (history.historyCode.toInt()) {
                1 -> {
                    // 예약 신청 거부 내역 있음 -> return
                    httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                    httpServletResponse.setHeader("api-result-code", "4")
                    return null
                }

                2 -> {
                    // 승인
                    if (approveNotChecked) {
                        approveNotChecked = false
                        notApproved = false
                    }
                }

                3 -> {
                    // 승인 취소
                    if (approveNotChecked) {
                        approveNotChecked = false
//                        notApproveCancel = false
                    }
                }

                4 -> {
                    // 예약 취소 신청
                    if (notCancelChecked) {
                        notCancelChecked = false
                        // 예약 취소 거부 내역이 최신인지
                        notRequestCancel = false
                    }
                }

                5 -> {
                    // 예약 취소 신청 취소
                    if (notCancelChecked) {
                        notCancelChecked = false
                        // 예약 취소 거부 내역이 최신인지
//                        notRequestCancelCancel = false
                    }
                }

                6 -> {
                    // 예약 취소 승인 내역 있음 -> return
                    httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                    httpServletResponse.setHeader("api-result-code", "3")
                    return null
                }

                7 -> {
                    // 예약 취소 거부
                    if (notCancelChecked) {
                        notCancelChecked = false
                        // 예약 취소 거부 내역이 최신인지
//                        notRequestCancelDeny = false
                    }
                }

                8 -> {
                    // 결제 확인
                    if (paymentNotChecked) {
                        notPaid = false
                        paymentNotChecked = false
                    }
                }

                9 -> {
                    // 결제 확인 취소
                    if (paymentNotChecked) {
                        paymentNotChecked = false
                    }
                }
            }
        }

        if (notPaid && nowDatetime.isAfter(reservationEntity.paymentCheckDeadlineDatetime)) {
            // 미결제 상태 & 결제 기한 초과 상태(= 취소와 동일) -> return
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "5")
            return null
        }

        if (!notRequestCancel) {
            // 예약 취소 신청 상태 -> return
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "6")
            return null
        }

        // 예약 취소 신청 정보 추가
        val reservationCancelEntity = db1RaillyLinkerCompanyRentableProductReservationStateChangeHistoryRepository.save(
            Db1_RaillyLinkerCompany_RentalProductReservationHistory(
                reservationEntity,
                4,
                inputVo.cancelReason
            )
        )

        // 상태에 따라 예약 취소 자동 승인 정보 추가
        val autoCancelCompleteEntityUid =
            if (!notPaid && (!notApproved || nowDatetime.isAfter(reservationEntity.approvalDeadlineDatetime))) {
                // 결제 확인 상태 && (예약 승인 상태 || 예약 승인 기한 초과)
                null
            } else {
                // 예약 완전 승인 상태가 아니라면 자동 취소 승인 처리
                db1RaillyLinkerCompanyRentableProductReservationStateChangeHistoryRepository.save(
                    Db1_RaillyLinkerCompany_RentalProductReservationHistory(
                        reservationEntity,
                        6,
                        "자동 취소 승인 처리"
                    )
                ).uid
            }

        httpServletResponse.status = HttpStatus.OK.value()
        return RentalReservationAdminController.PostCancelProductReservationOutputVo(
            reservationCancelEntity.uid!!,
            autoCancelCompleteEntityUid
        )
    }


    // ----
    // (예약 취소 신청 취소 <>)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun postCancelProductReservationCancel(
        httpServletResponse: HttpServletResponse,
        authorization: String,
        rentalProductReservationUid: Long
    ): RentalReservationAdminController.PostCancelProductReservationCancelOutputVo? {
        val reservationEntity: Db1_RaillyLinkerCompany_RentalProductReservation? =
            db1RaillyLinkerCompanyRentableProductReservationInfoRepository.findByUidAndRowDeleteDateStr(
                rentalProductReservationUid,
                "/"
            )

        if (reservationEntity == null) {
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return null
        }

        val nowDatetime = LocalDateTime.now()

        val historyList =
            db1RaillyLinkerCompanyRentableProductReservationStateChangeHistoryRepository.findAllByRentalProductReservationAndRowDeleteDateStrOrderByRowCreateDateDesc(
                reservationEntity,
                "/"
            )

        var notPaid = true
        var paymentNotChecked = true

        var notRequestCancel = true
//        var notRequestCancelDeny = true
//        var notRequestCancelCancel = true
        var notCancelChecked = true
        for (history in historyList) {
            when (history.historyCode.toInt()) {
                1 -> {
                    // 예약 신청 거부 내역 있음 -> return
                    httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                    httpServletResponse.setHeader("api-result-code", "3")
                    return null
                }

                4 -> {
                    // 예약 취소 신청
                    if (notCancelChecked) {
                        notCancelChecked = false
                        // 예약 취소 거부 내역이 최신인지
                        notRequestCancel = false
                    }
                }

                5 -> {
                    // 예약 취소 신청 취소
                    if (notCancelChecked) {
                        notCancelChecked = false
                        // 예약 취소 취소 내역이 최신인지
//                        notRequestCancelCancel = false
                    }
                }

                6 -> {
                    // 예약 취소 승인 내역 있음 -> return
                    httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                    httpServletResponse.setHeader("api-result-code", "2")
                    return null
                }

                7 -> {
                    // 예약 취소 거부
                    if (notCancelChecked) {
                        notCancelChecked = false
                        // 예약 취소 거부 내역이 최신인지
//                        notRequestCancelDeny = false
                    }
                }

                8 -> {
                    // 결제 확인
                    if (paymentNotChecked) {
                        notPaid = false
                        paymentNotChecked = false
                    }
                }

                9 -> {
                    // 결제 확인 취소
                    if (paymentNotChecked) {
                        paymentNotChecked = false
                    }
                }
            }
        }

        if (notPaid && nowDatetime.isAfter(reservationEntity.paymentCheckDeadlineDatetime)) {
            // 미결제 상태 & 결제 기한 초과 상태(= 취소와 동일) -> return
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "4")
            return null
        }

        if (notRequestCancel) {
            // 예약 취소 신청 상태가 없습니다. -> return
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "5")
            return null
        }

        // 예약 취소 신청 정보 추가
        val reservationCancelEntity = db1RaillyLinkerCompanyRentableProductReservationStateChangeHistoryRepository.save(
            Db1_RaillyLinkerCompany_RentalProductReservationHistory(
                reservationEntity,
                5,
                "사용자 예약 취소 신청 철회"
            )
        )

        httpServletResponse.status = HttpStatus.OK.value()
        return RentalReservationAdminController.PostCancelProductReservationCancelOutputVo(
            reservationCancelEntity.uid!!
        )
    }


    // ----
    // (개별 상품 조기 반납 신고 <>)
    // 관리자의 상품 반납 확인과 고객의 조기 반납 신고 간의 공유락 처리
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun postRentableProductStockReservationInfoEarlyReturn(
        httpServletResponse: HttpServletResponse,
        authorization: String,
        rentalProductReservationUid: Long,
        inputVo: RentalReservationAdminController.PostRentableProductStockReservationInfoEarlyReturnInputVo
    ): RentalReservationAdminController.PostRentableProductStockReservationInfoEarlyReturnOutputVo? {
        val rentableProductStockReservationInfo =
            db1RaillyLinkerCompanyRentableProductReservationInfoRepository.findByUidAndRowDeleteDateStr(
                rentalProductReservationUid,
                "/"
            )

        if (rentableProductStockReservationInfo == null) {
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return null
        }

        val nowDatetime = LocalDateTime.now()
        if (nowDatetime.isBefore(rentableProductStockReservationInfo.rentalStartDatetime)) {
            // 상품 대여 시작을 넘지 않음
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "3")
            return null
        }

        if (nowDatetime.isAfter(rentableProductStockReservationInfo.rentalEndDatetime)) {
            // 상품 대여 마지막일을 넘음
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "4")
            return null
        }

        val reservationHistoryList =
            db1RaillyLinkerCompanyRentableProductReservationStateChangeHistoryRepository.findAllByRentalProductReservationAndRowDeleteDateStrOrderByRowCreateDateDesc(
                rentableProductStockReservationInfo,
                "/"
            )

        var notPaid = true
        var paymentNotChecked = true
        var noEarlyReturn = true
        var noEarlyReturnCancel = true
        for (history in reservationHistoryList) {
            when (history.historyCode.toInt()) {
                1 -> {
                    // 예약 신청 거부 내역 있음 -> return
                    httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                    httpServletResponse.setHeader("api-result-code", "2")
                    return null
                }

                8 -> {
                    // 결제 확인
                    if (paymentNotChecked) {
                        notPaid = false
                        paymentNotChecked = false
                    }
                }

                9 -> {
                    // 결제 확인 취소
                    if (paymentNotChecked) {
                        paymentNotChecked = false
                    }
                }

                10 -> {
                    // 조기 반납 상태
                    if (noEarlyReturnCancel) {
                        noEarlyReturn = false
                    }
                }

                11 -> {
                    // 조기 반납 취소
                    if (noEarlyReturn) {
                        noEarlyReturnCancel = false
                    }
                }

                12 -> {
                    // 반납 확인
                    httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                    httpServletResponse.setHeader("api-result-code", "5")
                    return null
                }
            }
        }

        if (notPaid && nowDatetime.isAfter(rentableProductStockReservationInfo.paymentCheckDeadlineDatetime)) {
            // 미결제 상태 & 결제 기한 초과 상태(= 취소와 동일) -> return
            // 결제 확인 완료 아님 || 예약 신청 거부 = 대여 진행 상태가 아님
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "2")
            return null
        }

        if (!noEarlyReturn) {
            // 조기 반납 상태입니다.
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "6")
            return null
        }

        // 개별 상품 조기반납 신고 내역 추가
        val newReservationStateChangeHistory =
            db1RaillyLinkerCompanyRentableProductReservationStateChangeHistoryRepository.save(
                Db1_RaillyLinkerCompany_RentalProductReservationHistory(
                    rentableProductStockReservationInfo,
                    10,
                    inputVo.stateChangeDesc
                )
            )

        httpServletResponse.status = HttpStatus.OK.value()
        return RentalReservationAdminController.PostRentableProductStockReservationInfoEarlyReturnOutputVo(
            newReservationStateChangeHistory.uid!!
        )
    }


    // ----
    // (개별 상품 조기 반납 신고 취소 <>)
    // 관리자의 상품 반납 확인과 고객의 조기 반납 신고 간의 공유락 처리
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun postRentableProductStockReservationInfoEarlyReturnCancel(
        httpServletResponse: HttpServletResponse,
        authorization: String,
        rentalProductReservationUid: Long,
        inputVo: RentalReservationAdminController.PostRentableProductStockReservationInfoEarlyReturnCancelInputVo
    ): RentalReservationAdminController.PostRentableProductStockReservationInfoEarlyReturnCancelOutputVo? {
        val rentableProductStockReservationInfo =
            db1RaillyLinkerCompanyRentableProductReservationInfoRepository.findByUidAndRowDeleteDateStr(
                rentalProductReservationUid,
                "/"
            )

        if (rentableProductStockReservationInfo == null) {
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return null
        }

        // 상태 확인
        val historyList =
            db1RaillyLinkerCompanyRentableProductReservationStateChangeHistoryRepository.findAllByRentalProductReservationAndRowDeleteDateStrOrderByRowCreateDateDesc(
                rentableProductStockReservationInfo,
                "/"
            )

        var noEarlyReturn = true
        var noEarlyReturnCancel = true
        for (history in historyList) {
            when (history.historyCode.toInt()) {
                10 -> {
                    // 조기 반납 상태
                    if (noEarlyReturnCancel) {
                        noEarlyReturn = false
                    }
                }

                11 -> {
                    // 조기 반납 취소
                    if (noEarlyReturn) {
                        noEarlyReturnCancel = false
                    }
                }
            }
        }

        if (noEarlyReturn && noEarlyReturnCancel) {
            // 조기 반납 상태 변경 내역이 없습니다.
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "2")
            return null
        }

        if (!noEarlyReturnCancel) {
            // 조기 반납 상태 변경 취소 상태입니다.
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "3")
            return null
        }

        // 개별 상품 조기 반납 취소 내역 추가
        val newReservationStateChangeHistory =
            db1RaillyLinkerCompanyRentableProductReservationStateChangeHistoryRepository.save(
                Db1_RaillyLinkerCompany_RentalProductReservationHistory(
                    rentableProductStockReservationInfo,
                    11,
                    inputVo.stateChangeDesc
                )
            )

        httpServletResponse.status = HttpStatus.OK.value()
        return RentalReservationAdminController.PostRentableProductStockReservationInfoEarlyReturnCancelOutputVo(
            newReservationStateChangeHistory.uid!!
        )
    }


    // ----
    // (예약 연장 신청 <>)
    // 관리자의 상품 반납 확인과 고객의 조기 반납 신고 간의 공유락 처리
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun postRentableProductStockReservationInfoRentalExtend(
        httpServletResponse: HttpServletResponse,
        authorization: String,
        rentalProductReservationUid: Long,
        inputVo: RentalReservationAdminController.PostRentableProductStockReservationInfoRentalExtendInputVo
    ): RentalReservationAdminController.PostRentableProductStockReservationInfoRentalExtendOutputVo? {
        val rentableProductStockReservationInfo =
            db1RaillyLinkerCompanyRentableProductReservationInfoRepository.findByUidAndRowDeleteDateStr(
                rentalProductReservationUid,
                "/"
            )

        if (rentableProductStockReservationInfo == null) {
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return null
        }

        val nowDatetime = LocalDateTime.now()
        if (nowDatetime.isBefore(rentableProductStockReservationInfo.rentalStartDatetime)) {
            // 상품 대여 시작을 넘지 않음
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "3")
            return null
        }

        val inputRentalEndDatetime = ZonedDateTime.parse(
            inputVo.rentalEndDatetime,
            DateTimeFormatter.ofPattern("yyyy_MM_dd_'T'_HH_mm_ss_z")
        ).withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()

        if (rentableProductStockReservationInfo.rentalEndDatetime.isAfter(inputRentalEndDatetime)) {
            // rentalEndDatetime 가 기존 시간보다 작음
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "7")
            return null
        }

        val reservationHistoryList =
            db1RaillyLinkerCompanyRentableProductReservationStateChangeHistoryRepository.findAllByRentalProductReservationAndRowDeleteDateStrOrderByRowCreateDateDesc(
                rentableProductStockReservationInfo,
                "/"
            )

        var notPaid = true
        var paymentNotChecked = true
        var noEarlyReturn = true
        var noEarlyReturnCancel = true

        var notRequestExtend = true
//        var notRequestExtendDeny = true
//        var notRequestExtendCancel = true
        var notCancelChecked = true
        for (history in reservationHistoryList) {
            when (history.historyCode.toInt()) {
                1 -> {
                    // 예약 신청 거부 내역 있음 -> return
                    httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                    httpServletResponse.setHeader("api-result-code", "2")
                    return null
                }

                8 -> {
                    // 결제 확인
                    if (paymentNotChecked) {
                        notPaid = false
                        paymentNotChecked = false
                    }
                }

                9 -> {
                    // 결제 확인 취소
                    if (paymentNotChecked) {
                        paymentNotChecked = false
                    }
                }

                10 -> {
                    // 조기 반납 상태
                    if (noEarlyReturnCancel) {
                        noEarlyReturn = false
                    }
                }

                11 -> {
                    // 조기 반납 취소
                    if (noEarlyReturn) {
                        noEarlyReturnCancel = false
                    }
                }

                12 -> {
                    // 반납 확인
                    httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                    httpServletResponse.setHeader("api-result-code", "4")
                    return null
                }

                15 -> {
                    // 예약 연장 신청
                    if (notCancelChecked) {
                        notCancelChecked = false
                        // 예약 연장 신청 내역이 최신인지
                        notRequestExtend = false
                    }
                }

                16 -> {
                    // 예약 연장 신청 취소
                    if (notCancelChecked) {
                        notCancelChecked = false
                        // 예약 연장 취소 내역이 최신인지
//                        notRequestExtendCancel = false
                    }
                }

                17 -> {
                    // 예약 연장 거부
                    if (notCancelChecked) {
                        notCancelChecked = false
                        // 예약 연장 거부 내역이 최신인지
//                        notRequestExtendDeny = false
                    }
                }
            }
        }

        if (notPaid && nowDatetime.isAfter(rentableProductStockReservationInfo.paymentCheckDeadlineDatetime)) {
            // 미결제 상태 & 결제 기한 초과 상태(= 취소와 동일) -> return
            // 결제 확인 완료 아님 || 예약 신청 거부 = 대여 진행 상태가 아님
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "2")
            return null
        }

        if (!noEarlyReturn) {
            // 조기 반납 상태입니다.
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "5")
            return null
        }

        if (!notRequestExtend) {
            // 예약 연장 신청 상태입니다.
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "6")
            return null
        }

        val newReservationStateChangeHistory =
            db1RaillyLinkerCompanyRentableProductReservationStateChangeHistoryRepository.save(
                Db1_RaillyLinkerCompany_RentalProductReservationHistory(
                    rentableProductStockReservationInfo,
                    15,
                    inputVo.rentalEndDatetime + "/" + inputVo.stateChangeDesc
                )
            )

        httpServletResponse.status = HttpStatus.OK.value()
        return RentalReservationAdminController.PostRentableProductStockReservationInfoRentalExtendOutputVo(
            newReservationStateChangeHistory.uid!!
        )
    }


    // ----
    // (예약 연장 신청 취소 <>)
    @Transactional(transactionManager = Db1MainConfig.TRANSACTION_NAME)
    fun postRentableProductStockReservationInfoRentalExtendCancel(
        httpServletResponse: HttpServletResponse,
        authorization: String,
        rentalProductReservationUid: Long,
        inputVo: RentalReservationAdminController.PostRentableProductStockReservationInfoRentalExtendCancelInputVo
    ): RentalReservationAdminController.PostRentableProductStockReservationInfoRentalExtendCancelOutputVo? {
        val rentableProductStockReservationInfo =
            db1RaillyLinkerCompanyRentableProductReservationInfoRepository.findByUidAndRowDeleteDateStr(
                rentalProductReservationUid,
                "/"
            )

        if (rentableProductStockReservationInfo == null) {
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "1")
            return null
        }

        val nowDatetime = LocalDateTime.now()
        if (nowDatetime.isBefore(rentableProductStockReservationInfo.rentalStartDatetime)) {
            // 상품 대여 시작을 넘지 않음
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "3")
            return null
        }

        val reservationHistoryList =
            db1RaillyLinkerCompanyRentableProductReservationStateChangeHistoryRepository.findAllByRentalProductReservationAndRowDeleteDateStrOrderByRowCreateDateDesc(
                rentableProductStockReservationInfo,
                "/"
            )

        var notPaid = true
        var paymentNotChecked = true

        var notRequestExtend = true
//        var notRequestExtendDeny = true
//        var notRequestExtendCancel = true
        var notCancelChecked = true
        for (history in reservationHistoryList) {
            when (history.historyCode.toInt()) {
                1 -> {
                    // 예약 신청 거부 내역 있음 -> return
                    httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                    httpServletResponse.setHeader("api-result-code", "2")
                    return null
                }

                8 -> {
                    // 결제 확인
                    if (paymentNotChecked) {
                        notPaid = false
                        paymentNotChecked = false
                    }
                }

                9 -> {
                    // 결제 확인 취소
                    if (paymentNotChecked) {
                        paymentNotChecked = false
                    }
                }

                12 -> {
                    // 상품 반납 확인
                    httpServletResponse.status = HttpStatus.NO_CONTENT.value()
                    httpServletResponse.setHeader("api-result-code", "5")
                    return null
                }

                15 -> {
                    // 예약 연장 신청
                    if (notCancelChecked) {
                        notCancelChecked = false
                        // 예약 연장 신청 내역이 최신인지
                        notRequestExtend = false
                    }
                }

                16 -> {
                    // 예약 연장 신청 취소
                    if (notCancelChecked) {
                        notCancelChecked = false
                        // 예약 연장 취소 내역이 최신인지
//                        notRequestExtendCancel = false
                    }
                }

                17 -> {
                    // 예약 연장 거부
                    if (notCancelChecked) {
                        notCancelChecked = false
                        // 예약 연장 거부 내역이 최신인지
//                        notRequestExtendDeny = false
                    }
                }

                18 -> {
                    // 예약 연장 승인
                    if (notCancelChecked) {
                        notCancelChecked = false
                        // 예약 연장 거부 내역이 최신인지
//                        notRequestExtendDeny = false
                    }
                }
            }
        }

        if (notPaid && nowDatetime.isAfter(rentableProductStockReservationInfo.paymentCheckDeadlineDatetime)) {
            // 미결제 상태 & 결제 기한 초과 상태(= 취소와 동일) -> return
            // 결제 확인 완료 아님 || 예약 신청 거부 = 대여 진행 상태가 아님
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "2")
            return null
        }

        if (notRequestExtend) {
            // 예약 연장 신청 상태가 아닙니다.
            httpServletResponse.status = HttpStatus.NO_CONTENT.value()
            httpServletResponse.setHeader("api-result-code", "4")
            return null
        }

        // 개별 상품 조기반납 신고 내역 추가
        val newReservationStateChangeHistory =
            db1RaillyLinkerCompanyRentableProductReservationStateChangeHistoryRepository.save(
                Db1_RaillyLinkerCompany_RentalProductReservationHistory(
                    rentableProductStockReservationInfo,
                    16,
                    inputVo.stateChangeDesc
                )
            )

        httpServletResponse.status = HttpStatus.OK.value()
        return RentalReservationAdminController.PostRentableProductStockReservationInfoRentalExtendCancelOutputVo(
            newReservationStateChangeHistory.uid!!
        )
    }
}