package com.example.demo

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.ContentCachingRequestWrapper
import org.springframework.web.util.ContentCachingResponseWrapper

@Component
class TestFilter : OncePerRequestFilter() {
    private val log = LoggerFactory.getLogger(javaClass)
    private val om = ObjectMapper()

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val wrappedRequest =
            if (request is ContentCachingRequestWrapper) {
                request
            } else {
                ContentCachingRequestWrapper(request)
            }
        val wrappedResponse =
            if (response is ContentCachingResponseWrapper) {
                response
            } else {
                ContentCachingResponseWrapper(response)
            }

        try {
            // 필터 체인을 통해 다음 처리를 진행합니다.
            filterChain.doFilter(wrappedRequest, wrappedResponse)
        } finally {
            // 요청 URL 기록
            val logMap = mutableMapOf<String, Any?>()
            logMap["url"] = request.requestURI

            // 요청 헤더를 Map으로 기록
            val requestHeaders = mutableMapOf<String, String>()
            val headerNames = wrappedRequest.headerNames
            while (headerNames.hasMoreElements()) {
                val headerName = headerNames.nextElement()
                requestHeaders[headerName] = wrappedRequest.getHeader(headerName)
            }
            logMap["requestHeaders"] = requestHeaders

            // 요청 바디를 문자열로 변환 (UTF-8 인코딩 사용)
            val requestBody = String(wrappedRequest.contentAsByteArray, charset("UTF-8"))
            logMap["requestBody"] = requestBody

            // 응답 헤더를 Map으로 기록
            val responseHeaders = mutableMapOf<String, String>()
            for (headerName in wrappedResponse.headerNames) {
                responseHeaders[headerName] = requireNotNull(wrappedResponse.getHeader(headerName))
            }
            logMap["responseHeaders"] = responseHeaders

            // 응답 바디를 문자열로 변환 (UTF-8 인코딩 사용)
            val responseBody = String(wrappedResponse.contentAsByteArray, charset("UTF-8"))
            logMap["responseBody"] = responseBody

            // 사용자 커스텀 필드 (필요 시 request attribute 등에서 동적으로 값을 가져올 수 있음)
            logMap["customField"] = "CustomValue" // 여기에 사용자 정의 값을 설정합니다.

            // Map 데이터를 JSON 문자열로 변환하여 로그에 출력
            val jsonLog = om.writeValueAsString(logMap)
            log.info(jsonLog)

            // 응답 바디를 클라이언트로 복사 (반드시 호출해야 응답이 정상 전송됩니다.)
            wrappedResponse.copyBodyToResponse()
        }
    }
}
