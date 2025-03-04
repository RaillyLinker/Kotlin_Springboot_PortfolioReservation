package com.raillylinker.configurations.kafka_configs

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate

// [Kafka Consumer 설정]
// kafka_consumers 폴더 안의 Listeners 클래스 파일과 연계하여 사용하세요.
@EnableKafka
@Configuration
class Kafka1MainConfig {
    companion object {
        // !!!application.yml 의 kafka-cluster 안에 작성된 이름 할당하기!!!
        const val KAFKA_CONFIG_NAME: String = "kafka1-main"

        const val CONSUMER_BEAN_NAME: String =
            "${KAFKA_CONFIG_NAME}_ConsumerFactory"

        const val PRODUCER_BEAN_NAME: String =
            "${KAFKA_CONFIG_NAME}_ProducerFactory"
    }

    @Value("\${kafka-cluster.$KAFKA_CONFIG_NAME.uri}")
    private lateinit var uri: String

    @Value("\${kafka-cluster.$KAFKA_CONFIG_NAME.consumer.username}")
    private lateinit var userName: String

    @Value("\${kafka-cluster.$KAFKA_CONFIG_NAME.consumer.password}")
    private lateinit var password: String

    @Bean(CONSUMER_BEAN_NAME)
    fun kafkaConsumer(): ConcurrentKafkaListenerContainerFactory<String, Any> {
        val config: MutableMap<String, Any> = HashMap()
        // Kafka 브로커에 연결하기 위한 주소를 설정합니다. 여러 개의 브로커가 있을 경우, 콤마로 구분하여 나열합니다.
        config[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = uri
        config[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        config[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java

        // SASL/SCRAM 인증 설정 추가
        config["security.protocol"] = "SASL_PLAINTEXT"
        config["sasl.mechanism"] = "PLAIN"
        config["sasl.jaas.config"] =
            "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"$userName\" password=\"$password\";"

        val factory = ConcurrentKafkaListenerContainerFactory<String, Any>()
        factory.consumerFactory = DefaultKafkaConsumerFactory(config)

        return factory
    }

    @Bean(PRODUCER_BEAN_NAME)
    fun kafkaProducer(): KafkaTemplate<String, Any> {
        val config: MutableMap<String, Any> = HashMap()
        // Kafka 브로커에 연결하기 위한 주소를 설정합니다. 여러 개의 브로커가 있을 경우, 콤마로 구분하여 나열합니다.
        config[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = uri
        config[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        config[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java

        // SASL/SCRAM 인증 설정 추가
        config["security.protocol"] = "SASL_PLAINTEXT"
        config["sasl.mechanism"] = "PLAIN"
        config["sasl.jaas.config"] =
            "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"$userName\" password=\"$password\";"

        return KafkaTemplate(DefaultKafkaProducerFactory(config))
    }
}