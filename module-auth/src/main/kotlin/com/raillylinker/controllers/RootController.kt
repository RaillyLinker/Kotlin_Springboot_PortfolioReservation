package com.raillylinker.controllers

import com.raillylinker.services.RootService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.ModelAndView

@Tag(name = "root APIs", description = "Root 경로에 대한 API 컨트롤러")
@Controller
class RootController(
    private val service: RootService
) {
    // <멤버 변수 공간>


    // ---------------------------------------------------------------------------------------------
    // <매핑 함수 공간>
    @Operation(
        summary = "루트 홈페이지",
        description = "루트 홈페이지를 반환합니다."
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
        path = ["", "/"],
        consumes = [MediaType.ALL_VALUE],
        produces = [MediaType.TEXT_HTML_VALUE]
    )
    fun getRootHomePage(
        @Parameter(hidden = true)
        httpServletResponse: HttpServletResponse
    ): ModelAndView? {
        return service.getRootHomePage(httpServletResponse)
    }
}