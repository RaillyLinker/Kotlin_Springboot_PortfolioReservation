package com.raillylinker.util_components

import org.springframework.stereotype.Component
import org.springframework.util.StringUtils
import org.springframework.web.multipart.MultipartFile
import org.thymeleaf.context.Context
import org.thymeleaf.spring6.SpringTemplateEngine
import org.thymeleaf.templatemode.TemplateMode
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// [커스텀 유틸 함수 모음]
@Component
class CustomUtil {
    // (ThymeLeaf 엔진으로 랜더링 한 HTML String 을 반환)
    fun parseHtmlFileToHtmlString(
        justHtmlFileNameWithOutSuffix: String,
        variableDataMap: Map<String, Any?>
    ): String {
        // 타임리프 resolver 설정
        val templateResolver = ClassLoaderTemplateResolver()
        templateResolver.prefix = "templates/" // static/templates 경로 아래에 있는 파일을 읽는다
        templateResolver.suffix = ".html" // .html로 끝나는 파일을 읽는다
        templateResolver.templateMode = TemplateMode.HTML // 템플릿은 html 형식

        // 스프링 template 엔진을 thymeleafResolver 를 사용하도록 설정
        val templateEngine = SpringTemplateEngine()
        templateEngine.setTemplateResolver(templateResolver)

        // 템플릿 엔진에서 사용될 변수 입력
        val context = Context()
        context.setVariables(variableDataMap)

        // 지정한 html 파일과 context 를 읽어 String 으로 반환
        return templateEngine.process(justHtmlFileNameWithOutSuffix, context)
    }

    // (byteArray 를 Hex String 으로 반환)
    fun bytesToHex(bytes: ByteArray): String {
        val builder = StringBuilder()
        for (b in bytes) {
            builder.append(String.format("%02x", b))
        }
        return builder.toString()
    }


    // ----
    // (파일명, 경로, 확장자 분리 함수)
    // sample.jpg -> sample, jpg
    fun splitFilePath(filePath: String): FilePathParts {
        val fileName = filePath.substringBeforeLast(".", filePath) // 확장자가 없다면 전체 파일 이름이 그대로 fileName
        val extension = if (fileName != filePath) filePath.substringAfterLast(".", "") else null

        return FilePathParts(
            fileName = fileName,
            extension = extension
        )
    }

    data class FilePathParts(
        val fileName: String,
        val extension: String?
    )


    // ----
    // (Multipart File 을 로컬에 저장)
    // 반환값 : 저장된 파일명
    fun multipartFileLocalSave(
        // 파일을 저장할 로컬 위치 Path
        saveDirectoryPath: Path,
        // 저장할 파일명(파일명 뒤에 (현재 일시 yyyy_MM_dd_'T'_HH_mm_ss_SSS_z) 가 붙습니다.)
        // null 이라면 multipartFile 의 originalFilename 을 사용합니다.
        fileName: String?,
        // 저장할 MultipartFile
        multipartFile: MultipartFile
    ): String {
        // 파일 저장 기본 디렉토리 생성
        Files.createDirectories(saveDirectoryPath)

        // 원본 파일명(with suffix)
        val multiPartFileNameString = StringUtils.cleanPath(multipartFile.originalFilename!!)

        val fileNameSplit = splitFilePath(multiPartFileNameString)

        // 확장자가 없는 파일명
        val fileNameWithOutExtension: String = fileName ?: fileNameSplit.fileName
        // 확장자
        val fileExtension: String =
            if (fileNameSplit.extension == null) {
                ""
            } else {
                ".${fileNameSplit.extension}"
            }

        val savedFileName =
            "${fileNameWithOutExtension}(${
                LocalDateTime.now().atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("yyyy_MM_dd_'T'_HH_mm_ss_SSS_z"))
            })$fileExtension"

        // multipartFile 을 targetPath 에 저장
        multipartFile.transferTo(
            // 파일 저장 경로와 파일명(with index) 을 합친 path 객체
            saveDirectoryPath.resolve(
                savedFileName
            ).normalize()
        )

        return savedFileName
    }


    // ---------------------------------------------------------------------------------------------
    // <중첩 클래스 공간>

}