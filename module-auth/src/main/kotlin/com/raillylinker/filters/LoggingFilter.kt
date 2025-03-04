package com.raillylinker.filters

import jakarta.servlet.AsyncEvent
import jakarta.servlet.AsyncListener
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.ContentCachingRequestWrapper
import org.springframework.web.util.ContentCachingResponseWrapper
import java.io.UnsupportedEncodingException
import java.time.Duration
import java.time.LocalDateTime

// [API 별 Request / Response 로깅 필터]
// API 호출시마다 Request 와 Response 를 로깅하도록 처리했습니다.
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class LoggingFilter : OncePerRequestFilter() {
    // <멤버 변수 공간>
    private val classLogger = LoggerFactory.getLogger(this::class.java)

    // 로깅 body 에 표시할 데이터 타입
    // 여기에 포함된 데이터 타입만 로그에 표시됩니다.
    private val visibleTypeList = listOf(
        MediaType.valueOf("text/*"),
        MediaType.APPLICATION_JSON,
        MediaType.APPLICATION_XML,
        MediaType.valueOf("application/*+json"),
        MediaType.valueOf("application/*+xml")
    )


    // ---------------------------------------------------------------------------------------------
    // <상속 메소드 공간>
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val requestTime = LocalDateTime.now()

        // 요청자 Ip (ex : 127.0.0.1)
        val clientAddressIp = request.remoteAddr

        if (isAsyncDispatch(request)) {
            filterChain.doFilter(request, response)
            return
        }

        val httpServletRequest = request as? ContentCachingRequestWrapper ?: ContentCachingRequestWrapper(request)
        val httpServletResponse =
            response as? ContentCachingResponseWrapper ?: ContentCachingResponseWrapper(response)

        var isError = false
        try {
            if (httpServletRequest.getHeader("accept") == "text/event-stream") {
                // httpServletResponse 를 넣어야 response Body 출력이 제대로 되지만 text/event-stream 연결에 에러가 발생함
                filterChain.doFilter(httpServletRequest, response)
            } else {
                filterChain.doFilter(httpServletRequest, httpServletResponse)
            }
        } catch (e: Exception) {
            if (!(e is ServletException && e.rootCause is AccessDeniedException)) {
                isError = true
            }
            throw e
        } finally {
            val queryString = httpServletRequest.queryString?.let { "?$it" } ?: ""
            val endpoint = "${httpServletRequest.method} ${httpServletRequest.requestURI}$queryString"

            val requestHeaders = httpServletRequest.headerNames.asSequence().associateWith { headerName ->
                httpServletRequest.getHeader(headerName)
            }

            val requestContentByteArray = httpServletRequest.contentAsByteArray
            val requestBody = if (requestContentByteArray.isNotEmpty()) {
                getContentByte(requestContentByteArray, httpServletRequest.contentType)
            } else ""

            val responseStatus = httpServletResponse.status
            val responseStatusPhrase = try {
                HttpStatus.valueOf(responseStatus).reasonPhrase
            } catch (_: Exception) {
                ""
            }
            val responseHeaders = httpServletResponse.headerNames.asSequence().associateWith { headerName ->
                httpServletResponse.getHeader(headerName)
            }

            val responseContentByteArray = httpServletResponse.contentAsByteArray
            val responseBody = if (responseContentByteArray.isNotEmpty()) {
                if (httpServletResponse.contentType.startsWith("text/html")) {
                    // HTML 로깅 스킵
                    "HTML Content"
                } else if (
                    httpServletRequest.requestURI.startsWith("/v3/api-docs") ||
                    httpServletRequest.requestURI == "/swagger-ui/swagger-initializer.js"
                ) {
                    // 요청 스킵
                    "Skip"
                } else {
                    getContentByte(httpServletResponse.contentAsByteArray, httpServletResponse.contentType)
                }
            } else ""

            // 로깅 처리
            val logMessage =
                """
                        >>ApiFilterLog>>
                        {
                            "request_info" : {
                                "request_time" : "$requestTime",
                                "end_point" : "$endpoint",
                                "client_ip" : "$clientAddressIp",
                                "request_headers" : "$requestHeaders",
                                "request_body" : "$requestBody"
                            },
                            "response_info" :{
                                "response_status" : "$responseStatus $responseStatusPhrase",
                                "processing_duration_ms" : "${Duration.between(requestTime, LocalDateTime.now()).toMillis()}",
                                "response_headers" : "$responseHeaders",
                                "response_body" : "$responseBody"
                            }
                        }
                    """.trimIndent()

            if (isError) {
                classLogger.error(logMessage)
            } else {
                classLogger.info(logMessage)
            }

            // response 복사
            if (httpServletRequest.isAsyncStarted) { // DeferredResult 처리
                httpServletRequest.asyncContext.addListener(object : AsyncListener {
                    override fun onComplete(event: AsyncEvent?) {
                        httpServletResponse.copyBodyToResponse()
                    }

                    override fun onTimeout(event: AsyncEvent?) {
                    }

                    override fun onError(event: AsyncEvent?) {
                    }

                    override fun onStartAsync(event: AsyncEvent?) {
                    }
                })
            } else {
                httpServletResponse.copyBodyToResponse()
            }
        }
    }


    // ---------------------------------------------------------------------------------------------
    // <공개 메소드 공간>


    // ---------------------------------------------------------------------------------------------
    // <비공개 메소드 공간>
    private fun getContentByte(content: ByteArray, contentType: String): String {
        val mediaType = MediaType.valueOf(contentType)
        val visible = visibleTypeList.stream().anyMatch { visibleType -> visibleType.includes(mediaType) }

        return if (visible) {
            var contentStr = ""
            contentStr += try {
                String(content, charset("UTF-8"))
            } catch (e: UnsupportedEncodingException) {
                val contentSize = content.size
                "$contentSize bytes content"
            }
            contentStr
        } else {
            val contentSize = content.size
            "$contentSize bytes content"
        }
    }


    // ---------------------------------------------------------------------------------------------
    // <중첩 클래스 공간>
}