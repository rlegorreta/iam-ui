/* Copyright (c) 2023, LegoSoft Soluciones, S.C.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are not permitted.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 *  ClientWebController.kt
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.ailegorreta.iamui.backend.data.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.ailegorreta.iamui.config.ServiceConfig
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono

/**
 * ClientWebController abstract class that generates all Web client REST services
 * for the IAM-server-repo
 *
 * @author rlh
 * @project : iam-ui
 * @date July 2023
 */
abstract class ClientWebController(val mapper: ObjectMapper,
                                   val webclient: WebClient,
                                   val serviceConfig: ServiceConfig,
                                   circuitBreakerFactory: ReactiveResilience4JCircuitBreakerFactory? = null) {

    protected val circuitBreaker = circuitBreakerFactory?.create("webController")

    fun uri() = UriComponentsBuilder.fromUriString(serviceConfig.securityIAMProvider)

    fun provider() = serviceConfig.securityClientId + "-oidc"
    // ^ use the authentication_code not the credentials_code

    abstract fun controller(): ClientWebController

    abstract val baseUrlSingle: String

    abstract val baseUrlMany: String

    abstract fun clazz(): Class<*>

    abstract fun clazzes(): Class<*>

    abstract fun switchIfEmpty(): Mono<*>

}
