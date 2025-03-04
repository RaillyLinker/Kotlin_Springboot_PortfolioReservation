package com.raillylinker.util_components

import jakarta.mail.internet.InternetAddress
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ClassPathResource
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.io.File

// [Spring Email 유틸]
@Component
class EmailSender(
    private val customUtil: CustomUtil,
    private val javaMailSender: JavaMailSender,
    @Value("\${custom-config.smtp.sender-name}")
    var mailSenderName: String
) {
    fun sendMessageMail(
        senderName: String, // 이메일에 표시될 발송자 이름 (발송 이메일 주소는 application.yml 에 저장)
        receiverEmailAddressArray: Array<String>, // 수신자 이메일 배열
        carbonCopyEmailAddressArray: Array<String>?, // 참조자 이메일 배열
        subject: String, // 이메일 제목
        message: String, // 이메일 내용
        sendFileList: List<File>?, // 첨부파일 리스트
        sendMultipartFileList: List<MultipartFile>? // 첨부파일 리스트 (멀티파트)
    ) {
        val mimeMessage = javaMailSender.createMimeMessage()
        val isMultipart = !sendFileList.isNullOrEmpty() || !sendMultipartFileList.isNullOrEmpty() // multipart 전송 여부
        val mimeMessageHelper = MimeMessageHelper(mimeMessage, isMultipart, "UTF-8")

        mimeMessageHelper.setFrom(InternetAddress(mailSenderName, senderName)) // 발송자명 적용
        mimeMessageHelper.setTo(receiverEmailAddressArray) // 수신자 이메일 적용
        if (carbonCopyEmailAddressArray != null) {
            mimeMessageHelper.setCc(carbonCopyEmailAddressArray) // 참조자 이메일 적용
        }
        mimeMessageHelper.setSubject(subject) // 메일 제목 적용
        mimeMessageHelper.setText(message, false) // 메일 본문 내용 적용

        // 첨부파일 적용
        if (sendFileList != null) {
            for (sendFile in sendFileList) {
                mimeMessageHelper.addAttachment(sendFile.name, sendFile)
            }
        }
        if (sendMultipartFileList != null) {
            for (sendFile in sendMultipartFileList) {
                mimeMessageHelper.addAttachment(sendFile.originalFilename!!, sendFile)
            }
        }

        javaMailSender.send(mimeMessage)
    }

    // (ThymeLeaf 로 랜더링 한 HTML 이메일 발송)
    fun sendThymeLeafHtmlMail(
        senderName: String, // 이메일에 표시될 발송자 이름 (발송 이메일 주소는 application.yml 에 저장)
        receiverEmailAddressArray: Array<String>, // 수신자 이메일 배열
        carbonCopyEmailAddressArray: Array<String>?, // 참조자 이메일 배열
        subject: String, // 이메일 제목
        thymeLeafTemplateName: String, // thymeLeaf Html 이름 (ex : resources/templates/test.html -> "test")
        thymeLeafDataVariables: Map<String, Any>?, // thymeLeaf 템플릿에 제공할 정보 맵
        // thymeLeaf 내에 사용할 cid 파일 리스트
        /*
             Map 타입 변수는 (변수명, 파일 경로)의 순서이며,
             thymeLeafCidFileMap 은 ("image1", File("d://document/images/image-1.jpeg")) 이렇게,
             thymeLeafCidFileClassPathResourceMap 은 ("image2", ClassPathResource("static/images/image-2.jpeg")) 이렇게 입력하고,
             img 테그의 src 에는 'cid:image1' 혹은 'cid:image2' 이렇게 표시
         */
        thymeLeafCidFileMap: Map<String, File>?,
        thymeLeafCidFileClassPathResourceMap: Map<String, ClassPathResource>?,
        sendFileList: List<File>?, // 첨부파일 리스트
        sendMultipartFileList: List<MultipartFile>? // 첨부파일 리스트 (멀티파트)
    ) {
        val mimeMessage = javaMailSender.createMimeMessage()
        val mimeMessageHelper = MimeMessageHelper(mimeMessage, true, "UTF-8")

        mimeMessageHelper.setFrom(InternetAddress(mailSenderName, senderName)) // 발송자명 적용
        mimeMessageHelper.setTo(receiverEmailAddressArray) // 수신자 이메일 설정
        if (carbonCopyEmailAddressArray != null) {
            mimeMessageHelper.setCc(carbonCopyEmailAddressArray) // 참조자 이메일 설정
        }
        mimeMessageHelper.setSubject(subject)

        // 타임리프 HTML 랜더링
        val htmlString: String =
            customUtil.parseHtmlFileToHtmlString(
                thymeLeafTemplateName,
                thymeLeafDataVariables ?: mapOf()
            )
        mimeMessageHelper.setText(htmlString, true)

        // cid 파일 적용
        if (thymeLeafCidFileMap != null) {
            for (thymeLeafCidFileData in thymeLeafCidFileMap) {
                mimeMessageHelper.addInline(thymeLeafCidFileData.key, thymeLeafCidFileData.value)
            }
        }
        if (thymeLeafCidFileClassPathResourceMap != null) {
            for (thymeLeafCidFileData in thymeLeafCidFileClassPathResourceMap) {
                mimeMessageHelper.addInline(thymeLeafCidFileData.key, thymeLeafCidFileData.value)
            }
        }

        // 첨부파일 적용
        if (sendFileList != null) {
            for (sendFile in sendFileList) {
                mimeMessageHelper.addAttachment(sendFile.name, sendFile)
            }
        }
        if (sendMultipartFileList != null) {
            for (sendFile in sendMultipartFileList) {
                mimeMessageHelper.addAttachment(sendFile.originalFilename!!, sendFile)
            }
        }

        javaMailSender.send(mimeMessage)
    }
}