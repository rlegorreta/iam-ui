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
 *  FacultadService.kt
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.ailegorreta.iamui.backend.data.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.ailegorreta.client.security.service.CurrentSession
import com.ailegorreta.iamui.backend.data.dto.facultad.FacultadDTO
import com.ailegorreta.iamui.config.ServiceConfig
import com.ailegorreta.iamui.ui.dataproviders.FacultadesGridDataProvider.FacultadFilter
import com.ailegorreta.iamui.ui.exceptions.RestClientException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.MediaType
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.io.IOException
import java.time.LocalDateTime
import java.util.*

/**
 * FacultadService to communicate to IAM-server-repo for Permits
 *
 *  For demo purpose add Spring Cloud Circuit Breaker
 *
 *  @author rlh
 *  @project : iam-ui
 *  @date July 2023
 */
@Service
class FacultadService(mapper: ObjectMapper,
                      @Qualifier("authorization_code_load_balanced") webclient: WebClient,
                      private var securityService: CurrentSession,
                      serviceConfig: ServiceConfig,
                      circuitBreakerFactory: ReactiveResilience4JCircuitBreakerFactory) :
    ClientWebController(mapper, webclient, serviceConfig, circuitBreakerFactory),
    CustomWebClientService<FacultadDTO> {

    override val baseUrlMany = "/iam/facultad/facultades"
    override val baseUrlSingle = "/iam/facultad/facultad"

    override fun clazz() = FacultadDTO::class.java
    override fun clazzes() = Array<FacultadDTO>::class.java
    override fun switchIfEmpty() = Mono.just(arrayOfNulls<FacultadDTO>(0))
    override fun controller() = this

    override fun toObject(res: String) = toObject(mapper, res, FacultadDTO::class.java)

    override fun toList(res: String): List<FacultadDTO> {
        val facultades: MutableList<FacultadDTO> = ArrayList()
        try {
            val json = mapper.readValue(res, JsonNode::class.java)

            if (json.isArray)
                json.forEach {
                    facultades.add(toObject(mapper, it.toString(), FacultadDTO::class.java))
                }
            else
                throw RestClientException(RestClientException.STATUS_JSON_CONVERSION, "Se espera una array")
        } catch (e: IOException) {
            throw RestClientException(RestClientException.STATUS_JSON_CONVERSION, e)
        }

        return facultades
    }

    fun findAnyMatchingActive(filter: Optional<FacultadFilter>, pageable: Pageable): Page<FacultadDTO> {
        return if (filter.isPresent && filter.get().filter.isNotEmpty())
            if (filter.get().isShowActive)
                findByNameActive(circuitBreaker!!, filter.get().filter, filter.get().isShowActive, pageable)
            else
                findByName(circuitBreaker!!, filter.get().filter, pageable)
        else if (filter.isPresent && filter.get().isShowActive)
            findActive(circuitBreaker!!, filter.get().isShowActive, pageable)
        else
            findAll(circuitBreaker!!, pageable)
    }

    fun countAnyMatchingActive(filter: Optional<FacultadFilter>): Mono<Long> {
        return if (filter.isPresent && filter.get().filter.isNotEmpty())
            if (filter.get().isShowActive)
                countByNameActive(circuitBreaker!!, filter.get().filter, filter.get().isShowActive)
            else
                countByName(circuitBreaker!!, filter.get().filter)
        else if (filter.isPresent && filter.get().isShowActive)
            countActive(circuitBreaker!!, filter.get().isShowActive)
        else
            count(circuitBreaker!!)
    }

    fun saveFacultad(facultad: FacultadDTO): Mono<FacultadDTO> {
        facultad.fechaModificacion = LocalDateTime.now()
        facultad.usuarioModificacion = securityService.authenticatedUser.get().name

        return circuitBreaker!!.run ( webclient.post()
                                .uri(uri().path("/$baseUrlSingle/add").build().toUri())
                                .accept(MediaType.APPLICATION_JSON)
                                .body(Mono.just(facultad), FacultadDTO::class.java)
                                .attributes(clientRegistrationId(controller().provider()))
                                .retrieve()
                                .bodyToMono(FacultadDTO::class.java)
            ) { throwable ->
                logger.error("Alguno de los servicios el IAM (o ambos se encuentra NO disponible NO se lista ninguna facultad", throwable)
                Mono.empty()
            }
    }

}
