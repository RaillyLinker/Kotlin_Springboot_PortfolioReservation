package com.raillylinker.kafka_components.producers

import com.google.gson.Gson
import com.raillylinker.configurations.kafka_configs.Kafka1MainConfig
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class Kafka1MainProducer(
    @Qualifier(Kafka1MainConfig.PRODUCER_BEAN_NAME) private val kafka1MainProducerTemplate: KafkaTemplate<String, Any>,
) {
    // <멤버 변수 공간>
    private val classLogger: Logger = LoggerFactory.getLogger(this::class.java)

    // ---------------------------------------------------------------------------------------------
    // <공개 메소드 공간>
    // (결제 실패)
    fun sendMessageFromPaymentPaymentFailed(message: SendMessageFromPaymentPaymentFailedInputVo) {
        // kafkaProducer1 에 토픽 메세지 발행
        kafka1MainProducerTemplate.send("from_payment_payment_failed", Gson().toJson(message))
    }

    data class SendMessageFromPaymentPaymentFailedInputVo(
        val paymentRequestUid: Long
    )


    // ----
    // (결제 성공)
    fun sendMessageFromPaymentPaymentCompleted(message: SendMessageFromPaymentPaymentCompletedInputVo) {
        // kafkaProducer1 에 토픽 메세지 발행
        kafka1MainProducerTemplate.send("from_payment_payment_completed", Gson().toJson(message))
    }

    data class SendMessageFromPaymentPaymentCompletedInputVo(
        val paymentRequestUid: Long
    )


    // ----
    // (환불 실패)
    fun sendMessageFromPaymentPaymentRefundFailed(message: SendMessageFromPaymentPaymentRefundFailedInputVo) {
        // kafkaProducer1 에 토픽 메세지 발행
        kafka1MainProducerTemplate.send("from_payment_payment_refund_failed", Gson().toJson(message))
    }

    data class SendMessageFromPaymentPaymentRefundFailedInputVo(
        val paymentRefundRequestUid: Long
    )


    // ----
    // (환불 성공)
    fun sendMessageFromPaymentPaymentRefundCompleted(message: SendMessageFromPaymentPaymentRefundCompletedInputVo) {
        // kafkaProducer1 에 토픽 메세지 발행
        kafka1MainProducerTemplate.send("from_payment_payment_refund_completed", Gson().toJson(message))
    }

    data class SendMessageFromPaymentPaymentRefundCompletedInputVo(
        val paymentRefundRequestUid: Long
    )
}