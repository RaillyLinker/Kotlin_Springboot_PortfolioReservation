package com.raillylinker.configurations

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.mail.javamail.JavaMailSenderImpl
import java.util.*

// [JavaMail 설정]
@Configuration
class MailConfig(
    @Value("\${custom-config.smtp.host}")
    var host: String,
    @Value("\${custom-config.smtp.port}")
    var port: Int,
    @Value("\${custom-config.smtp.sender-name}")
    var senderName: String,
    @Value("\${custom-config.smtp.sender-password}")
    var senderPassword: String,
    @Value("\${custom-config.smtp.time-out-millis}")
    var timeOutMillis: String
) {
    @Bean
    fun javaMailSender(): JavaMailSenderImpl {
        val mailSender = JavaMailSenderImpl()
        mailSender.host = host
        mailSender.port = port
        mailSender.username = senderName
        mailSender.password = senderPassword

        val props: Properties = mailSender.javaMailProperties
        props["mail.smtp.connectiontimeout"] = timeOutMillis
        props["mail.smtp.timeout"] = timeOutMillis
        props["mail.smtp.writetimeout"] = timeOutMillis

        // SMTP 종류별 설정
        // 보안 설정이 필요없는 상황이라면 아래 코드를 주석처리 하면 됩니다.
        if (port == 587) {
            // port 587 일 경우
            props["mail.transport.protocol"] = "smtp"
            props["mail.smtp.auth"] = "true"
            props["mail.smtp.starttls.enable"] = "true"
            props["mail.debug"] = "true"
        } else if (port == 465) {
            // port 465 일 경우
            props["mail.smtp.ssl.enable"] = "true"  // SSL 활성화
            props["mail.smtp.auth"] = "true"  // SMTP 인증 활성화
            props["mail.smtp.connectiontimeout"] = "10000"
            props["mail.smtp.timeout"] = "10000"
            props["mail.smtp.writetimeout"] = "10000"
            props["mail.smtp.ssl.checkserveridentity"] = "false"
            props["mail.smtp.ssl.trust"] = "*"
        }

        return mailSender
    }
}