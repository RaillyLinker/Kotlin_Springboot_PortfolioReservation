package com.raillylinker.configurations

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import java.util.function.Consumer

// [Swagger API 문서 설정]
@Configuration
class SwaggerConfig(
    // (버전 정보)
    @Value("\${custom-config.swagger.document-version}")
    private var documentVersion: String,

    // (문서 제목)
    @Value("\${custom-config.swagger.document-title}")
    private var documentTitle: String,

    // (문서 설명)
    @Value("\${custom-config.swagger.document-description}")
    private var documentDescription: String
) {
    // <멤버 변수 공간>


    // ---------------------------------------------------------------------------------------------
    // <공개 메소드 공간>
    @Bean
    fun openAPI(): OpenAPI {
        val component =
            Components().addSecuritySchemes(
                "JWT",
                SecurityScheme().apply {
                    this.type = SecurityScheme.Type.HTTP
                    this.scheme = "bearer"
                    this.bearerFormat = "JWT"
                    this.`in` = SecurityScheme.In.HEADER
                    this.name = HttpHeaders.AUTHORIZATION
                })

        val securityRequirement =
            SecurityRequirement().apply {
                this.addList("JWT")
            }

        val documentInfo =
            Info().apply {
                this.title(documentTitle)
                this.version(documentVersion)
                this.description(documentDescription)
            }

        return OpenAPI()
            .components(component)
            .addSecurityItem(securityRequirement)
            .info(documentInfo)
    }

    @Bean
    fun openApiCustomizer(): OpenApiCustomizer {
        val pathItemConsumer =
            Consumer { operation: Operation ->
                operation
                    .responses
                    .addApiResponse(
                        "400",
                        ApiResponse()
                            .description(
                                "클라이언트에서 전달한 Request 변수의 형식이 잘못되었습니다.<br>" +
                                        "입력 데이터를 다시 한번 확인해주세요"
                            )
                    )
                    .addApiResponse(
                        "500",
                        ApiResponse()
                            .description(
                                "서버에서 런타임 에러가 발생하였습니다.<br>" +
                                        "서버 개발자에게 에러 상황, 에러 로그 등의 정보를 알려주세요."
                            )
                    )
            }

        return OpenApiCustomizer { openApi: OpenAPI ->
            openApi
                .paths
                .values
                .forEach(
                    Consumer { pathItem: PathItem ->
                        pathItem
                            .readOperations()
                            .forEach(pathItemConsumer)
                    }
                )
        }
    }
}