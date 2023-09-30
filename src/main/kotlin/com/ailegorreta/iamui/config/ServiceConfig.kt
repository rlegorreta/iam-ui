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
 *  ServiceConfig
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.ailegorreta.iamui.config

import com.ailegorreta.client.security.config.SecurityServiceConfig
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.timelimiter.TimeLimiterConfig
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder
import org.springframework.cloud.client.circuitbreaker.Customizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration


/**
 * Configuration class to read all application properties.
 *
 * @project iam-ui
 * @author rlh
 * @date: September 2023
 */
@Configuration
class ServiceConfig: SecurityServiceConfig {

    @Value("\${spring.application.name}")
    val appName: String = "Nombre de la aplicación no definido"

    @Value("\${spring.application.version}")
    val appVersion: String = "Versión de la aplicación no definida"

    @Value("\${vapid.public.key}")
    val publicKey: String = "Public key not defined in application properties"

    @Value("\${vapid.private.key}")
    val privateKey: String = "Private key not defines in application properties"

    @Value("\${microservice.cache.provider-uri}")
    private val cacheProviderUri: String = "Issuer uri not defined"
    fun getCacheProvider() =  cacheProviderUri

    @Value("\${microservice.audit.provider-uri}")
    private val auditProviderUri: String = "Issuer uri not defined"
    fun getAuditProvider() =  auditProviderUri

    @Value("\${microservice.audit.subscription.host}")
    private val subscriptionHost: String = "Issuer uri not defined"
    fun getSubscriptionHost() =  subscriptionHost

    @Value("\${microservice.audit.subscription.port}")
    private val subscriptionPort = -1
    fun getSubscriptionPort() = subscriptionPort

    @Value("\${security.clientId}")
    private val securityClientId: String = "ClientID not defined"
    override fun getSecurityClientId() = securityClientId

    @Value("\${spring.security.oauth2.client.provider.spring.issuer-uri}")
    private val issuerUri: String = "Issuer uri not defined"
    override fun getIssuerUri() = issuerUri

    @Value("\${server.port}")
    private val serverPort: Int = 0
    override fun getServerPort() = serverPort

    @Value("\${microservice.iam.clientId}")
    private val securityIamClientId: String = "Issuer uri not defined"
    override fun getSecurityIAMClientId() = securityIamClientId

    @Value("\${microservice.iam.provider-uri}")
    private val securityIamProvider: String = "Issuer uri not defined"
    override fun getSecurityIAMProvider() = securityIamProvider

    @Value("\${security.iam.load-balanced}")
    private val useLoadBalanced: Boolean = false
    override fun useLoadBalanced(): Boolean = useLoadBalanced

    /**
     * Circuit breaker global configuration. For more information see:
     * https://docs.spring.io/spring-cloud-circuitbreaker/docs/current/reference/html/
     */
    val circuitBreakerConfig = CircuitBreakerConfig.custom()
                                                    .failureRateThreshold(50f)
                                                    .waitDurationInOpenState(Duration.ofMillis(1000))
                                                    .slidingWindowSize(5)
                                                    .build()
    val timeLimiterConfig: TimeLimiterConfig = TimeLimiterConfig.custom()
                                                                .timeoutDuration(Duration.ofSeconds(4))
                                                                .build()

    // the circuitBreakerConfig and timeLimiterConfig objects
    @Bean
    fun globalCustomConfiguration() = Customizer { factory: ReactiveResilience4JCircuitBreakerFactory ->
        factory.configureDefault { id -> Resilience4JConfigBuilder(id)
                                            .timeLimiterConfig(timeLimiterConfig)
                                            .circuitBreakerConfig(circuitBreakerConfig)
                                            .build()
        }
    }

}
