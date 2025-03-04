package com.raillylinker.retrofit2_classes.request_apis

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.http.*

// (한 주소에 대한 API 요청명세)
// 사용법은 아래 기본 사용 샘플을 참고하여 추상함수를 작성하여 사용
interface SensApigwNtrussComRequestApi {
    // (Naver SMS 발송)
    // https://api.ncloud-docs.com/docs/ai-application-service-sens-smsv2
    @POST("sms/v2/services/{naverSmsServiceId}/messages")
    @Headers("Content-Type: application/json")
    fun postSmsV2ServicesNaverSmsServiceIdMessages(
        // 프로젝트 등록 시 발급받은 서비스 아이디
        @Path("naverSmsServiceId") naverSmsServiceId: String,
        // 1970년 1월 1일 00:00:00 협정 세계시(UTC)부터의 경과 시간을 밀리초(Millisecond)로 나타냄
        // API Gateway 서버와 시간 차가 5분 이상 나는 경우 유효하지 않은 요청으로 간주
        @Header("x-ncp-apigw-timestamp") xNcpApigwTimestamp: String,
        // 포탈 또는 Sub Account에서 발급받은 Access Key ID
        @Header("x-ncp-iam-access-key") xNcpIamAccessKey: String,
        // 위 예제의 Body를 Access Key Id와 맵핑되는 SecretKey로 암호화한 서명, HMAC 암호화 알고리즘은 HmacSHA256 사용
        @Header("x-ncp-apigw-signature-v2") xNcpApigwSignatureV2: String,
        @Body inputVo: PostSmsV2ServicesNaverSmsServiceIdMessagesInputVO
    ): Call<PostSmsV2ServicesNaverSmsServiceIdMessagesOutputVO?>

    data class PostSmsV2ServicesNaverSmsServiceIdMessagesInputVO(
        // SMS Type, SMS, LMS, MMS (소문자 가능)
        @SerializedName("type")
        @Expose
        var type: String,
        // 메시지 Type, COMM: 일반메시지 default, AD: 광고메시지
        @SerializedName("contentType")
        @Expose
        var contentType: String?,
        // 국가 번호, SENS에서 제공하는 국가로의 발송만 가능, default: 82
        // https://guide.ncloud-docs.com/docs/sens-smspolicy
        @SerializedName("countryCode")
        @Expose
        var countryCode: String?,
        // 발신번호, 사전 등록된 발신번호만 사용 가능
        @SerializedName("from")
        @Expose
        var from: String,
        // 기본 메시지 제목, LMS, MMS에서만 사용 가능(최대 40byte)
        @SerializedName("subject")
        @Expose
        var subject: String?,
        // 기본 메시지 내용, SMS: 최대 90byte, LMS, MMS: 최대 2000byte
        @SerializedName("content")
        @Expose
        var content: String,
        // 메시지 정보, 최대 100개
        @SerializedName("messages")
        @Expose
        var messages: List<MessageVo>,
        // 파일 전송
        @SerializedName("files")
        @Expose
        var files: List<FileVo>?,
        // 메시지 발송 예약 일시 (yyyy-MM-dd HH:mm)
        @SerializedName("reserveTime")
        @Expose
        var reserveTime: String?,
        // 예약 일시 타임존 (기본: Asia/Seoul) https://en.wikipedia.org/wiki/List_of_tz_database_time_zones
        @SerializedName("reserveTimeZone")
        @Expose
        var reserveTimeZone: String?
    ) {
        data class MessageVo(
            // 수신번호, 붙임표 ( - )를 제외한 숫자만 입력 가능
            @SerializedName("to")
            @Expose
            var to: String,
            // 개별 메시지 제목, LMS, MMS에서만 사용 가능(최대 40byte)
            @SerializedName("subject")
            @Expose
            var subject: String?,
            // 개별 메시지 내용, SMS: 최대 90byte, LMS, MMS: 최대 2000byte
            @SerializedName("content")
            @Expose
            var content: String?
        )

        data class FileVo(
            // 파일 아이디, MMS 에서만 사용 가능, 파일 전송 api 사용 후 받아온 fileId 를 입력
            @SerializedName("fileId")
            @Expose
            var fileId: String,
        )
    }

    data class PostSmsV2ServicesNaverSmsServiceIdMessagesOutputVO(
        // 요청 아이디
        @SerializedName("requestId")
        @Expose
        var requestId: String,
        // 요청 시간 yyyy-MM-dd'T'HH:mm:ss.SSS
        @SerializedName("requestTime")
        @Expose
        var requestTime: String,
        // 요청 상태 코드, 202: 성공, 그 외: 실패, HTTP Status 규격을 따름
        @SerializedName("statusCode")
        @Expose
        var statusCode: String,
        // 요청 상태명, success: 성공, fail: 실패
        @SerializedName("statusName")
        @Expose
        var statusName: String
    )


    // (Naver SMS 파일 발송)
    // postSmsV2ServicesNaverSmsServiceIdMessages API 에서 파일을 전송하기 전에,
    // 이것을 실행하여 fileId 를 발급받아서 전송하면 됩니다.
    // https://api.ncloud-docs.com/docs/ai-application-service-sens-smsv2
    @POST("sms/v2/services/{naverSmsServiceId}/files")
    @Headers("Content-Type: application/json")
    fun postSmsV2ServicesNaverSmsServiceIdFiles(
        // 프로젝트 등록 시 발급받은 서비스 아이디
        @Path("naverSmsServiceId") naverSmsServiceId: String,
        // 1970년 1월 1일 00:00:00 협정 세계시(UTC)부터의 경과 시간을 밀리초(Millisecond)로 나타냄
        // API Gateway 서버와 시간 차가 5분 이상 나는 경우 유효하지 않은 요청으로 간주
        @Header("x-ncp-apigw-timestamp") xNcpApigwTimestamp: String,
        // 포탈 또는 Sub Account에서 발급받은 Access Key ID
        @Header("x-ncp-iam-access-key") xNcpIamAccessKey: String,
        // 위 예제의 Body를 Access Key Id와 맵핑되는 SecretKey로 암호화한 서명, HMAC 암호화 알고리즘은 HmacSHA256 사용
        @Header("x-ncp-apigw-signature-v2") xNcpApigwSignatureV2: String,
        @Body inputVo: PostSmsV2ServicesNaverSmsServiceIdFilesInputVO
    ): Call<PostSmsV2ServicesNaverSmsServiceIdFilesOutputVO?>

    data class PostSmsV2ServicesNaverSmsServiceIdFilesInputVO(
        // 파일 이름, .jpg, .jpeg 확장자를 가진 파일 이름, 최대 40자
        @SerializedName("fileName")
        @Expose
        var fileName: String,
        // 파일 바디, .jpg, .jpeg 이미지를 Base64로 인코딩한 값, 원 파일 기준 최대 300Kbyte, 해상도 최대 1500 * 1440
        @SerializedName("fileBody")
        @Expose
        var fileBody: String
    )

    data class PostSmsV2ServicesNaverSmsServiceIdFilesOutputVO(
        // 파일 아이디, MMS 메시지 발송 시 활용
        @SerializedName("fileId")
        @Expose
        var fileId: String,
        // 파일 업로드 시간 yyyy-MM-dd'T'HH:mm:ss.SSS
        @SerializedName("createTime")
        @Expose
        var createTime: String,
        // 파일 만료 시간 yyyy-MM-dd'T'HH:mm:ss.SSS
        @SerializedName("expireTime")
        @Expose
        var expireTime: String
    )


    // (Naver Alimtalk 발송)
    // https://api.ncloud-docs.com/docs/ai-application-service-sens-alimtalkv2#%EB%A9%94%EC%8B%9C%EC%A7%80-%EB%B0%9C%EC%86%A1
    @POST("alimtalk/v2/services/{naverSmsServiceId}/messages")
    @Headers("Content-Type: application/json")
    fun postAlimtalkV2ServicesNaverSmsServiceIdMessages(
        // 프로젝트 등록 시 발급받은 서비스 아이디
        @Path("naverSmsServiceId") naverSmsServiceId: String,
        // 1970년 1월 1일 00:00:00 협정 세계시(UTC)부터의 경과 시간을 밀리초(Millisecond)로 나타냄
        // API Gateway 서버와 시간 차가 5분 이상 나는 경우 유효하지 않은 요청으로 간주
        @Header("x-ncp-apigw-timestamp") xNcpApigwTimestamp: String,
        // 포탈 또는 Sub Account에서 발급받은 Access Key ID
        @Header("x-ncp-iam-access-key") xNcpIamAccessKey: String,
        // 위 예제의 Body를 Access Key Id와 맵핑되는 SecretKey로 암호화한 서명, HMAC 암호화 알고리즘은 HmacSHA256 사용
        @Header("x-ncp-apigw-signature-v2") xNcpApigwSignatureV2: String,
        @Body inputVo: PostAlimtalkV2ServicesNaverSmsServiceIdMessagesInputVO
    ): Call<PostAlimtalkV2ServicesNaverSmsServiceIdMessagesOutputVO?>

    data class PostAlimtalkV2ServicesNaverSmsServiceIdMessagesInputVO(
        // 카카오톡 채널명 ((구)플러스친구 아이디)
        @SerializedName("plusFriendId")
        @Expose
        var plusFriendId: String,
        // 템플릿 코드
        @SerializedName("templateCode")
        @Expose
        var templateCode: String,
        // 메시지 정보, 최대 100개
        @SerializedName("messages")
        @Expose
        var messages: List<MessageVo>,
        // 메시지 발송 예약 일시 (yyyy-MM-dd HH:mm)
        @SerializedName("reserveTime")
        @Expose
        var reserveTime: String?,
        // 예약 일시 타임존 (기본: Asia/Seoul) https://en.wikipedia.org/wiki/List_of_tz_database_time_zones
        @SerializedName("reserveTimeZone")
        @Expose
        var reserveTimeZone: String?
    ) {
        data class MessageVo(
            // 수신자 국가번호, default: 82
            @SerializedName("countryCode")
            @Expose
            var countryCode: String?,
            // 수신자번호
            @SerializedName("to")
            @Expose
            var to: String,
            // 알림톡 강조표시 내용, 강조 표기 유형의 템플릿에서만 사용 가능
            @SerializedName("title")
            @Expose
            var title: String?,
            // 알림톡 메시지 내용
            @SerializedName("content")
            @Expose
            var content: String,
            // 알림톡 헤더 내용, 아이템 리스트 유형의 템플릿에서만 사용 가능, 16 bytes 미만 까지 입력 가능
            @SerializedName("headerContent")
            @Expose
            var headerContent: String?,
            // 아이템 하이라이트, 아이템 리스트 유형의 템플릿에서만 사용 가능
            @SerializedName("itemHighlight")
            @Expose
            var itemHighlight: ItemHighlightVo?,
            // 아이템 리스트, 아이템리스트 유형의 템플릿에서만 사용 가능
            @SerializedName("item")
            @Expose
            var item: ItemVo?,
            // 알림톡 메시지 버튼
            @SerializedName("buttons")
            @Expose
            var buttons: List<ButtonVo>?,
            // SMS Failover 사용 여부, Failover가 설정된 카카오톡 채널에서만 사용 가능, 기본: 카카오톡 채널의 Failover 설정 여부를 따름
            @SerializedName("useSmsFailover")
            @Expose
            var useSmsFailover: Boolean?,
            // Failover 설정
            @SerializedName("failoverConfig")
            @Expose
            var failoverConfig: FailOverConfigVo?
        ) {
            data class ItemHighlightVo(
                // 아이템 하이라이트 제목, 아이템 리스트 유형의 템플릿에서만 사용 가능
                // 이미지가 없는 경우 : 최대 30자까지 입력 가능 (2줄), 1줄은 15자까지 입력 가능
                // 이미지가 있는 경우 : 최대 21자까지 입력 가능 (2줄), 1줄은 10자까지 입력 가능, 2줄 초과 시 말줄임 처리
                @SerializedName("title")
                @Expose
                var title: String,
                // 아이템 하이라이트 설명, 아이템 리스트 유형의 템플릿에서만 사용 가능
                // 이미지가 없는 경우 : 최대 19자까지 입력 가능 (1줄)
                // 이미지가 있는 경우 : 최대 13자까지 입력 가능 (1줄), 1줄 초과 시 말줄임 처리
                @SerializedName("description")
                @Expose
                var description: String
            )

            data class ItemVo(
                // 아이템 리스트, 아이템리스트 유형의 템플릿에서만 사용 가능, 최소 2개 이상, 최대 10개
                @SerializedName("list")
                @Expose
                var list: List<ListItemVo>,
                // 아이템 요약 정보, 아이템리스트 유형의 템플릿에서만 사용 가능
                @SerializedName("summary")
                @Expose
                var summary: SummaryVo?
            ) {
                data class ListItemVo(
                    // 아이템 리스트 제목, 아이템리스트 유형의 템플릿에서만 사용 가능, 최대 6자까지 입력 가능
                    @SerializedName("title")
                    @Expose
                    var title: String,
                    // 아이템 리스트 설명, 아이템리스트 유형의 템플릿에서만 사용 가능, 최대 23자까지 입력 가능
                    @SerializedName("description")
                    @Expose
                    var description: String
                )

                data class SummaryVo(
                    // 아이템 요약 제목, 아이템리스트 유형의 템플릿에서만 사용 가능, 최대 6자까지 입력 가능
                    @SerializedName("title")
                    @Expose
                    var title: String,
                    // 아이템 요약 설명, 아이템리스트 유형의 템플릿에서만 사용 가능,
                    // 허용되는 문자: 통화기호(유니코드 통화기호, 元, 円, 원), 통화코드 (ISO 4217), 숫자, 콤마, 소수점, 공백
                    // 소수점 2자리까지 허용, 최대 23자까지 입력 가능
                    @SerializedName("description")
                    @Expose
                    var description: String
                )
            }

            data class ButtonVo(
                /*
                    type        name        필수 항목
                    DS          배송 조회
                    WL          웹 링크      linkMobile, linkPc (http:// 또는 https://로 시작하는 URL)
                    AL          앱 링크      schemeIos, schemeAndroid
                    BK          봇 키워드
                    MD          메시지 전달
                    AC          채널 추가    버튼 명은 채널 추가 로 고정
                 */
                // 버튼 Type
                @SerializedName("type")
                @Expose
                var type: String,
                // 버튼명
                @SerializedName("name")
                @Expose
                var name: String,
                @SerializedName("linkMobile")
                @Expose
                var linkMobile: String?,
                @SerializedName("linkPc")
                @Expose
                var linkPc: String?,
                @SerializedName("schemeIos")
                @Expose
                var schemeIos: String?,
                @SerializedName("schemeAndroid")
                @Expose
                var schemeAndroid: String?
            )

            data class FailOverConfigVo(
                // Failover SMS 메시지 Type, SMS 또는 LMS, 기본: content 길이에 따라 자동 적용(90 bytes 이하 SMS, 초과 LMS)
                @SerializedName("type")
                @Expose
                var type: String?,
                // Failover SMS 발신번호, 기본: Failover 설정 시 선택한 발신번호, 승인되지 않은 발신번호 사용시 Failover 동작 안함
                @SerializedName("from")
                @Expose
                var from: String?,
                // Failover SMS 제목, LMS type으로 동작할 때 사용, 기본: 카카오톡 채널명
                @SerializedName("subject")
                @Expose
                var subject: String?,
                // Failover SMS 내용, 기본: 알림톡 메시지 내용 (버튼 제외)
                @SerializedName("content")
                @Expose
                var content: String?
            )
        }
    }

    data class PostAlimtalkV2ServicesNaverSmsServiceIdMessagesOutputVO(
        // 발송 요청 아이디
        @SerializedName("requestId")
        @Expose
        var requestId: String,
        // 발송 요청 시간, yyyy-MM-dd'T'HH:mm:ss.SSS
        @SerializedName("requestTime")
        @Expose
        var requestTime: String,
        // 요청 상태 코드, 성공: 202, 실패: 그 외, HTTP Status 규격을 따름
        @SerializedName("statusCode")
        @Expose
        var statusCode: String,
        // 요청 상태명, 성공: success, 처리 중: processing, 예약 중: reserved, 실패: fail
        @SerializedName("statusName")
        @Expose
        var statusName: String,
        // 메시지
        @SerializedName("messages")
        @Expose
        var messages: List<MessageVo>
    ) {
        data class MessageVo(
            // 메시지 아이디
            @SerializedName("messageId")
            @Expose
            var messageId: String,
            // 수신자 국가번호, default: 82
            @SerializedName("countryCode")
            @Expose
            var countryCode: String?,
            // 수신자 번호
            @SerializedName("to")
            @Expose
            var to: String,
            // 알림톡 메시지 내용
            @SerializedName("content")
            @Expose
            var content: String,
            // 발송요청 상태 코드, 성공: A000, 실패: 그 외 코드(Desc 항목에 실패 사유가 명시)
            @SerializedName("requestStatusCode")
            @Expose
            var requestStatusCode: String,
            // 발송 요청 상태명, 성공: success, 실패: fail
            @SerializedName("requestStatusName")
            @Expose
            var requestStatusName: String,
            // 발송 요청 상태 내용
            @SerializedName("requestStatusDesc")
            @Expose
            var requestStatusDesc: String,
            // SMS Failover 사용 여부
            @SerializedName("useSmsFailover")
            @Expose
            var useSmsFailover: Boolean
        )
    }
}