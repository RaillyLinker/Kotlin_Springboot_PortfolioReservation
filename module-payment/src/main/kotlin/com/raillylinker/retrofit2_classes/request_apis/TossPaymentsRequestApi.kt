package com.raillylinker.retrofit2_classes.request_apis

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import io.swagger.v3.oas.annotations.media.Schema
import retrofit2.Call
import retrofit2.http.*

// (한 주소에 대한 API 요청명세)
// 사용법은 아래 기본 사용 샘플을 참고하여 추상함수를 작성하여 사용
interface TossPaymentsRequestApi {
    // [결제 승인 API]
    @POST("/v1/payments/confirm")
    fun postV1PaymentsConfirm(
        // api 호출 인증키 (ex : Basic dGVzdF9za196WExrS0V5cE5BcldtbzUwblgzbG1lYXhZRzVSOg==)
        @Header("Authorization") authorization: String,
        @Body inputVo: PostV1PaymentsConfirmInputVO
    ): Call<PostV1PaymentsConfirmOutputVO?>

    data class PostV1PaymentsConfirmInputVO(
        // 결제의 키값입니다. 최대 길이는 200자입니다. 결제를 식별하는 역할로, 중복되지 않는 고유한 값입니다.
        @SerializedName("paymentKey")
        @Expose
        val paymentKey: String,
        /*
            주문번호입니다. 주문한 결제를 식별합니다.
            충분히 무작위한 값을 생성해서 각 주문마다 고유한 값을 넣어주세요.
            영문 대소문자, 숫자, 특수문자 -, _로 이루어진 6자 이상 64자 이하의 문자열이어야 합니다.
            결제 데이터 관리를 위해 반드시 저장해야 합니다.
         */
        @SerializedName("orderId")
        @Expose
        val orderId: String,
        // 결제할 금액입니다.
        @SerializedName("amount")
        @Expose
        val amount: Long
    )

    // (성공 VO)
    data class PostV1PaymentsConfirmOutputVO(
        // 상점아이디(MID)입니다. 토스페이먼츠에서 발급합니다. 최대 길이는 14자입니다.
        @SerializedName("mid")
        @Expose
        val mid: String,
        // 결제수단입니다. 카드, 가상계좌, 간편결제, 휴대폰, 계좌이체, 문화상품권, 도서문화상품권, 게임문화상품권 중 하나입니다.
        @SerializedName("method")
        @Expose
        val method: String,
        /*
            마지막 거래의 키값입니다.
            한 결제 건의 승인 거래와 취소 거래를 구분하는 데 사용됩니다.
            예를 들어 결제 승인 후 부분 취소를 두 번 했다면 마지막 부분 취소 거래의 키값이 할당됩니다.
            최대 길이는 64자입니다.
         */
        @SerializedName("lastTransactionKey")
        @Expose
        val lastTransactionKey: String,
        /*
            결제의 키값입니다.
            최대 길이는 200자입니다.
            결제를 식별하는 역할로, 중복되지 않는 고유한 값입니다.
            결제 데이터 관리를 위해 반드시 저장해야 합니다.
            결제 상태가 변해도 값이 유지됩니다.
            결제 승인, 결제 조회, 결제 취소 API에 사용합니다.
         */
        @SerializedName("paymentKey")
        @Expose
        val paymentKey: String,
        /*
            주문번호입니다.
            결제 요청에서 내 상점이 직접 생성한 영문 대소문자, 숫자, 특수문자 -, _로 이루어진 6자 이상 64자 이하의 문자열입니다.
            각 주문을 식별하는 역할로, 결제 데이터 관리를 위해 반드시 저장해야 합니다.
            결제 상태가 변해도 orderId는 유지됩니다.
         */
        @SerializedName("orderId")
        @Expose
        val orderId: String,
        // 구매상품입니다. 예를 들면 생수 외 1건 같은 형식입니다. 최대 길이는 100자입니다.
        @SerializedName("orderName")
        @Expose
        val orderName: String,
        /*
            과세를 제외한 결제 금액(컵 보증금 등)입니다.
            이 값은 결제 취소 및 부분 취소가 되면 과세 제외 금액도 일부 취소되어 값이 바뀝니다.
            * 과세 제외 금액이 있는 카드 결제는 부분 취소가 안 됩니다.
         */
        @SerializedName("taxExemptionAmount")
        @Expose
        val taxExemptionAmount: Long,
        /*
            결제 처리 상태입니다. 아래와 같은 상태 값을 가질 수 있습니다. 상태 변화 흐름이 궁금하다면 흐름도를 살펴보세요.
            - READY: 결제를 생성하면 가지게 되는 초기 상태입니다. 인증 전까지는 READY 상태를 유지합니다.
            - IN_PROGRESS: 결제수단 정보와 해당 결제수단의 소유자가 맞는지 인증을 마친 상태입니다. 결제 승인 API를 호출하면 결제가 완료됩니다.
            - WAITING_FOR_DEPOSIT: 가상계좌 결제 흐름에만 있는 상태입니다. 발급된 가상계좌에 구매자가 아직 입금하지 않은 상태입니다.
            - DONE: 인증된 결제수단으로 요청한 결제가 승인된 상태입니다.
            - CANCELED: 승인된 결제가 취소된 상태입니다.
            - PARTIAL_CANCELED: 승인된 결제가 부분 취소된 상태입니다.
            - ABORTED: 결제 승인이 실패한 상태입니다.
            - EXPIRED: 결제 유효 시간 30분이 지나 거래가 취소된 상태입니다. IN_PROGRESS 상태에서 결제 승인 API를 호출하지 않으면 EXPIRED가 됩니다.
         */
        @SerializedName("status")
        @Expose
        val status: String,
        /*
            결제가 일어난 날짜와 시간 정보입니다.
            yyyy-MM-dd'T'HH:mm:ss±hh:mm ISO 8601 형식입니다.
            (e.g. 2022-01-01T00:00:00+09:00)
         */
        @SerializedName("requestedAt")
        @Expose
        val requestedAt: String,
        /*
            결제 승인이 일어난 날짜와 시간 정보입니다.
            yyyy-MM-dd'T'HH:mm:ss±hh:mm ISO 8601 형식입니다.
            (e.g. 2022-01-01T00:00:00+09:00)
         */
        @SerializedName("approvedAt")
        @Expose
        val approvedAt: String,
        // 에스크로 사용 여부입니다.
        @SerializedName("useEscrow")
        @Expose
        val useEscrow: Boolean,
        /*
            문화비(도서, 공연 티켓, 박물관·미술관 입장권 등) 지출 여부입니다.
            계좌이체, 가상계좌 결제에만 적용됩니다.
            * 카드 결제는 항상 false로 돌아옵니다.
            카드 결제 문화비는 카드사에 문화비 소득공제 전용 가맹점번호로 등록하면 자동으로 처리됩니다.
         */
        @SerializedName("cultureExpense")
        @Expose
        val cultureExpense: Boolean
    )

    // (실패 VO)
    data class PostV1PaymentsConfirmErrorOutputVO(
        // 에러 코드입니다.
        @SerializedName("code")
        @Expose
        val code: String,
        // 에러 메시지입니다.
        @SerializedName("message")
        @Expose
        val message: String
    )


    // ----
    // [결제 취소 API]
    @POST("/v1/payments/{paymentKey}/cancel")
    fun postV1PaymentsCancel(
        // api 호출 인증키 (ex : Basic dGVzdF9za196WExrS0V5cE5BcldtbzUwblgzbG1lYXhZRzVSOg==)
        @Header("Authorization") authorization: String,
        // 결제의 키값입니다. 최대 길이는 200자입니다. 결제를 식별하는 역할로, 중복되지 않는 고유한 값입니다.
        @Path("paymentKey") paymentKey: String,
        @Body inputVo: PostV1PaymentsCancelInputVO
    ): Call<PostV1PaymentsCancelOutputVO?>

    data class PostV1PaymentsCancelInputVO(
        // 결제를 취소하는 이유입니다. 최대 길이는 200자입니다.
        @SerializedName("cancelReason")
        @Expose
        val cancelReason: String,
        // 취소할 금액입니다. 값이 없으면 전액 취소됩니다.
        @SerializedName("cancelAmount")
        @Expose
        val cancelAmount: Long?,
        /*
            결제 취소 후 금액이 환불될 계좌의 정보입니다.
            가상계좌 결제에만 필수입니다.
            다른 결제수단으로 이루어진 결제를 취소할 때는 사용하지 않습니다.
            보낸 계좌 정보는 유효성 검사가 이뤄집니다.
            유효한 계좌로 결제 취소 요청이 되었다면 환불은 취소 요청 후 2일 후에 진행됩니다.
            구매자가 가상계좌에 입금을 아직 안 했다면, 결제를 취소해도 환불해야 되는 금액이 없기 때문에 이 파라미터를 추가할 필요가 없습니다.
            입금 전에는 부분 취소를 할 수 없고 전체 금액 취소만 할 수 있습니다.
         */
        @SerializedName("refundReceiveAccount")
        @Expose
        val refundReceiveAccount: RefundReceiveAccount?
    ) {
        data class RefundReceiveAccount(
            // 취소 금액을 환불받을 계좌의 은행 코드입니다.
            // 은행 코드 : https://docs.tosspayments.com/codes/org-codes#%EC%9D%80%ED%96%89-%EC%BD%94%EB%93%9C
            // 증권사 코드 : https://docs.tosspayments.com/codes/org-codes#%EC%A6%9D%EA%B6%8C%EC%82%AC-%EC%BD%94%EB%93%9C
            @SerializedName("bank")
            @Expose
            val bank: String,
            // 취소 금액을 환불받을 계좌의 계좌번호입니다. - 없이 숫자만 넣어야 합니다. 최대 길이는 20자입니다.
            @SerializedName("accountNumber")
            @Expose
            val accountNumber: String,
            // 취소 금액을 환불받을 계좌의 예금주입니다. 최대 길이는 60자입니다.
            @SerializedName("holderName")
            @Expose
            val holderName: String
        )
    }

    // (성공 VO)
    data class PostV1PaymentsCancelOutputVO(
        // 상점아이디(MID)입니다. 토스페이먼츠에서 발급합니다. 최대 길이는 14자입니다.
        @SerializedName("mid")
        @Expose
        val mid: String,
        // 결제수단입니다. 카드, 가상계좌, 간편결제, 휴대폰, 계좌이체, 문화상품권, 도서문화상품권, 게임문화상품권 중 하나입니다.
        @SerializedName("method")
        @Expose
        val method: String,
        /*
            마지막 거래의 키값입니다.
            한 결제 건의 승인 거래와 취소 거래를 구분하는 데 사용됩니다.
            예를 들어 결제 승인 후 부분 취소를 두 번 했다면 마지막 부분 취소 거래의 키값이 할당됩니다.
            최대 길이는 64자입니다.
         */
        @SerializedName("lastTransactionKey")
        @Expose
        val lastTransactionKey: String,
        /*
            결제의 키값입니다.
            최대 길이는 200자입니다.
            결제를 식별하는 역할로, 중복되지 않는 고유한 값입니다.
            결제 데이터 관리를 위해 반드시 저장해야 합니다.
            결제 상태가 변해도 값이 유지됩니다.
            결제 승인, 결제 조회, 결제 취소 API에 사용합니다.
         */
        @SerializedName("paymentKey")
        @Expose
        val paymentKey: String,
        /*
            주문번호입니다.
            결제 요청에서 내 상점이 직접 생성한 영문 대소문자, 숫자, 특수문자 -, _로 이루어진 6자 이상 64자 이하의 문자열입니다.
            각 주문을 식별하는 역할로, 결제 데이터 관리를 위해 반드시 저장해야 합니다.
            결제 상태가 변해도 orderId는 유지됩니다.
         */
        @SerializedName("orderId")
        @Expose
        val orderId: String,
        // 구매상품입니다. 예를 들면 생수 외 1건 같은 형식입니다. 최대 길이는 100자입니다.
        @SerializedName("orderName")
        @Expose
        val orderName: String,
        /*
            과세를 제외한 결제 금액(컵 보증금 등)입니다.
            이 값은 결제 취소 및 부분 취소가 되면 과세 제외 금액도 일부 취소되어 값이 바뀝니다.
            * 과세 제외 금액이 있는 카드 결제는 부분 취소가 안 됩니다.
         */
        @SerializedName("taxExemptionAmount")
        @Expose
        val taxExemptionAmount: Long,
        /*
            결제 처리 상태입니다. 아래와 같은 상태 값을 가질 수 있습니다. 상태 변화 흐름이 궁금하다면 흐름도를 살펴보세요.
            - READY: 결제를 생성하면 가지게 되는 초기 상태입니다. 인증 전까지는 READY 상태를 유지합니다.
            - IN_PROGRESS: 결제수단 정보와 해당 결제수단의 소유자가 맞는지 인증을 마친 상태입니다. 결제 승인 API를 호출하면 결제가 완료됩니다.
            - WAITING_FOR_DEPOSIT: 가상계좌 결제 흐름에만 있는 상태입니다. 발급된 가상계좌에 구매자가 아직 입금하지 않은 상태입니다.
            - DONE: 인증된 결제수단으로 요청한 결제가 승인된 상태입니다.
            - CANCELED: 승인된 결제가 취소된 상태입니다.
            - PARTIAL_CANCELED: 승인된 결제가 부분 취소된 상태입니다.
            - ABORTED: 결제 승인이 실패한 상태입니다.
            - EXPIRED: 결제 유효 시간 30분이 지나 거래가 취소된 상태입니다. IN_PROGRESS 상태에서 결제 승인 API를 호출하지 않으면 EXPIRED가 됩니다.
         */
        @SerializedName("status")
        @Expose
        val status: String,
        /*
            결제가 일어난 날짜와 시간 정보입니다.
            yyyy-MM-dd'T'HH:mm:ss±hh:mm ISO 8601 형식입니다.
            (e.g. 2022-01-01T00:00:00+09:00)
         */
        @SerializedName("requestedAt")
        @Expose
        val requestedAt: String,
        /*
            결제 승인이 일어난 날짜와 시간 정보입니다.
            yyyy-MM-dd'T'HH:mm:ss±hh:mm ISO 8601 형식입니다.
            (e.g. 2022-01-01T00:00:00+09:00)
         */
        @SerializedName("approvedAt")
        @Expose
        val approvedAt: String,
        // 에스크로 사용 여부입니다.
        @SerializedName("useEscrow")
        @Expose
        val useEscrow: Boolean,
        /*
            문화비(도서, 공연 티켓, 박물관·미술관 입장권 등) 지출 여부입니다.
            계좌이체, 가상계좌 결제에만 적용됩니다.
            * 카드 결제는 항상 false로 돌아옵니다.
            카드 결제 문화비는 카드사에 문화비 소득공제 전용 가맹점번호로 등록하면 자동으로 처리됩니다.
         */
        @SerializedName("cultureExpense")
        @Expose
        val cultureExpense: Boolean
    )

    // (실패 VO)
    data class PostV1PaymentsCancelErrorOutputVO(
        // 에러 코드입니다.
        @SerializedName("code")
        @Expose
        val code: String,
        // 에러 메시지입니다.
        @SerializedName("message")
        @Expose
        val message: String
    )
}