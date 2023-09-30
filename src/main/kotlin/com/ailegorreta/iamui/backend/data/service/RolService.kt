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
 *  RolService.kt
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.ailegorreta.iamui.backend.data.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.ailegorreta.iamui.backend.data.dto.facultad.AssignFacultadDTO
import com.ailegorreta.iamui.backend.data.dto.facultad.RolDTO
import com.ailegorreta.client.security.service.CurrentSession
import com.ailegorreta.iamui.config.ServiceConfig
import com.ailegorreta.iamui.ui.dataproviders.RolFilter
import com.ailegorreta.iamui.ui.exceptions.RestClientException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.MediaType
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.io.IOException
import java.time.LocalDateTime
import java.util.*

/**
 * RolService to communicate to IAM-server-repo for Roles
 *
 * @author rlh
 * @project : iam-ui
 * @date July 2023
 */
@Service
class RolService(mapper: ObjectMapper,
                 @Qualifier("authorization_code_load_balanced") webclient: WebClient,
                 private var securityService: CurrentSession,
                 serviceConfig: ServiceConfig,
                 circuitBreakerFactory: ReactiveResilience4JCircuitBreakerFactory) :
            ClientWebController(mapper, webclient, serviceConfig, circuitBreakerFactory),
    CustomWebClientService<RolDTO> {

    override val baseUrlMany = "/iam/facultad/roles"
    override val baseUrlSingle = "/iam/facultad/rol"

    override fun clazz() = RolDTO::class.java
    override fun clazzes() = Array<RolDTO>::class.java
    override fun switchIfEmpty() = Mono.just(arrayOfNulls<RolDTO>(0))
    override fun controller() =  this

    override fun toObject(res: String) = toObject(mapper, res, RolDTO::class.java)

    override fun toList(res: String): List<RolDTO> {
        val roles: MutableList<RolDTO> = ArrayList()

        try {
            val json = mapper.readValue(res, JsonNode::class.java)

            if (json.isArray)
                json.forEach {
                    roles.add(toObject(mapper, it.toString(), RolDTO::class.java))
                }
        } catch (e: IOException) {
            throw RestClientException(RestClientException.STATUS_JSON_CONVERSION, e)
        }
        return roles
    }

    fun findAnyMatchingActive(filter: Optional<RolFilter>, pageable: Pageable): Page<RolDTO> {
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

    fun countAnyMatchingActive(filter: Optional<RolFilter>): Mono<Long> {
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

    fun findByIdRol(idRol: Long): Mono<RolDTO> = circuitBreaker!!.run( webclient.get()
                                            .uri(controller().uri().path("/$baseUrlSingle/by/idRol")
                                                .queryParam("idRol", idRol)
                                            .build().toUri())
                                            .attributes(clientRegistrationId(controller().provider()))
                                            .retrieve()
                                            .bodyToMono(RolDTO::class.java)
                                            .doOnNext { s -> logger.debug("Found id rol: {}", s.toString()) }
                                            .cast(RolDTO::class.java)
        ) { throwable ->
            logger.error("Alguno de los servicios el IAM (o ambos se encuentra NO disponible NO se lista ninguna facultad", throwable)
            Mono.empty()
        }

    fun saveRol(rol: RolDTO): Mono<RolDTO> {
        rol.fechaModificacion = LocalDateTime.now()
        rol.usuarioModificacion = securityService.authenticatedUser.get().name

        return circuitBreaker!!.run( webclient.post()
                                    .uri(uri().path("/$baseUrlSingle/add").build().toUri())
                                    .accept(MediaType.APPLICATION_JSON)
                                    .body(Mono.just(rol), RolDTO::class.java)
                                    .attributes(ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId(controller().provider()))
                                    .retrieve()
                                    .bodyToMono(RolDTO::class.java)
        ) { throwable ->
            logger.error("Alguno de los servicios el IAM (o ambos se encuentra NO disponible NO se lista ninguna facultad", throwable)
            Mono.empty()
        }
    }

    fun assignPermit(assignFacultad: AssignFacultadDTO): Mono<AssignFacultadDTO> = circuitBreaker!!.run(  webclient.post()
                                        .uri(uri().path("/$baseUrlSingle/add/facultad").build().toUri())
                                        .accept(MediaType.APPLICATION_JSON)
                                        .body(Mono.just(assignFacultad), AssignFacultadDTO::class.java)
                                        .attributes(ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId(controller().provider()))
                                        .retrieve()
                                        .bodyToMono(AssignFacultadDTO::class.java)
        ) { throwable ->
            logger.error("Alguno de los servicios el IAM (o ambos se encuentra NO disponible NO se lista ninguna facultad", throwable)
            Mono.empty()
        }

    fun unAssignPermit(assignFacultad: AssignFacultadDTO): Mono<AssignFacultadDTO> = circuitBreaker!!.run( webclient.post()
                                        .uri(uri().path("/$baseUrlSingle/delete/facultad").build().toUri())
                                        .accept(MediaType.APPLICATION_JSON)
                                        .body(Mono.just(assignFacultad), AssignFacultadDTO::class.java)
                                        .attributes(ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId(controller().provider()))
                                        .retrieve()
                                        .bodyToMono(AssignFacultadDTO::class.java)
        ) { throwable ->
            logger.error("Alguno de los servicios el IAM (o ambos se encuentra NO disponible NO se lista ninguna facultad", throwable)
            Mono.empty()
        }

}
