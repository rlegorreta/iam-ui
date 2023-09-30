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
 *  CustomWebClientService.kt
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.ailegorreta.iamui.backend.data.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.ailegorreta.commons.utils.HasLogger
import com.ailegorreta.iamui.ui.exceptions.RestClientException
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.support.PageableExecutionUtils
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId
import reactor.core.publisher.Mono
import java.io.IOException
import java.time.Duration
import java.util.*
import java.util.function.LongSupplier

/**
 * CustomWebClientService interface. This is to avoid duplicate code between services
 * for common WebClient services like /find/by/id
 *
 * Add circuit break functionality
 *
 * @author rlh
 * @project : iam-ui
 * @date July 2023
 */
interface CustomWebClientService<T> : HasLogger {

    fun controller(): ClientWebController

    @Throws(RestClientException::class)
    fun toObject(mapper: ObjectMapper, jsonValue: String, clazz: Class<T>): T {
        return try {
            mapper.readValue(jsonValue, clazz)
        } catch (e: IOException) {
            throw RestClientException(RestClientException.STATUS_JSON_CONVERSION, e)
        }
    }

    fun toList(res: String): List<T>

    fun toObject(res: String): T

    fun countByNameActive(filter: String, activo: Boolean): Mono<Long> {
        return controller().webclient.get()
                            .uri(controller().uri().path("/${controller().baseUrlMany}/nombre/activo/count")
                                                   .queryParam("nombre",filter.trim { it <= ' ' })
                                                   .queryParam("activo", booleanStr(activo))
                                                   .build().toUri())
                            .attributes(clientRegistrationId(controller().provider()))
                            .retrieve()
                            .bodyToMono(Long::class.java)
    }

    /**
     * Demo to see how can we implement circuit breaker.
     * For more information, see:
     * https://spring.io/guides/gs/cloud-circuit-breaker/
     */
    fun countByNameActive(circuitBreaker: ReactiveCircuitBreaker,
                          filter: String, activo: Boolean): Mono<Long> {
        return circuitBreaker.run(controller().webclient.get()
                                    .uri(controller().uri().path("/${controller().baseUrlMany}/nombre/activo/count")
                                        .queryParam("nombre",filter.trim { it <= ' ' })
                                        .queryParam("activo", booleanStr(activo))
                                        .build().toUri())
                                    .attributes(clientRegistrationId(controller().provider()))
                                    .retrieve()
                                    .bodyToMono(Long::class.java)
        ) { throwable ->
            logger.error("Alguno de los dos micro.servicios del IAM (o ambos se encuentra NO disponibles) se regresa un cero", throwable)
            Mono.just(0)        // return cero that it is an empty list
        }
    }

    fun findByNameActive(filter: String, activo: Boolean, pageable: Pageable): Page<T> {
        val res =  controller().webclient.get()
                            .uri(controller().uri().path("/${controller().baseUrlMany}/nombre/activo")
                                .queryParam("nombre", filter.trim { it <= ' ' })
                                .queryParam("activo", booleanStr(activo))
                                .queryParam("page", pageable.pageNumber)
                                .queryParam("size", pageable.pageSize)
                                .queryParam("sort", pageable.sort)
                                .build().toUri())
                            .attributes(clientRegistrationId(controller().provider()))
                            .retrieve()
                            .bodyToMono(controller().clazzes())
                            .timeout(Duration.ofMillis(10_000))
                            .switchIfEmpty(controller().switchIfEmpty() as Mono<out Nothing>)
                            .doOnNext { s -> logger.debug("findByNameActive: {}", (s as Array<T>).contentToString()) }
                            .map{ elements -> listOf(elements) }
                            as  Mono<List<T>>

        val total = LongSupplier { countByNameActive(filter, activo).block()!! }
        val elements = res.block()!!.first() as Array<T>

        return PageableExecutionUtils.getPage(elements.toMutableList(), pageable, total)
    }

    /**
     * Demo to see how can we implement circuit breaker.
     * For more information, see:
     * https://spring.io/guides/gs/cloud-circuit-breaker/
     */
    fun findByNameActive(circuitBreaker: ReactiveCircuitBreaker,
                         filter: String, activo: Boolean, pageable: Pageable): Page<T> {
        val res = circuitBreaker.run(
            controller().webclient.get()
                                .uri(controller().uri().path("/${controller().baseUrlMany}/nombre/activo")
                                    .queryParam("nombre", filter.trim { it <= ' ' })
                                    .queryParam("activo", booleanStr(activo))
                                    .queryParam("page", pageable.pageNumber)
                                    .queryParam("size", pageable.pageSize)
                                    .queryParam("sort", pageable.sort)
                                    .build().toUri())
                                .attributes(clientRegistrationId(controller().provider()))
                                .retrieve()
                                .bodyToMono(controller().clazzes())
                                .timeout(Duration.ofMillis(10_000))
                                .switchIfEmpty(controller().switchIfEmpty() as Mono<out Nothing>)
                                .doOnNext { s -> logger.debug("findByNameActive: {}", (s as Array<T>).contentToString()) }
                                .map{ elements -> listOf(elements) }
                                    as  Mono<List<T>>
        ) { throwable ->
            logger.error("Alguno de los servicios el IAM (o ambos se encuentra NO disponible NO se lista ninguna facultad", throwable)
            Mono.just(ArrayList<T>(0))
        }

        val total = LongSupplier { countByNameActive(filter, activo).block()!! }
        val elements = if (res.block()!!.isEmpty()) res.block()!! else (res.block()!!.first() as Array<T>).toMutableList()

        return PageableExecutionUtils.getPage(elements, pageable, total)
    }

    fun countByName(filter: String): Mono<Long> {
        return controller().webclient.get()
                            .uri(controller().uri().path("/${controller().baseUrlMany}/nombre/count")
                                .queryParam("nombre",filter.trim { it <= ' ' })
                                .build().toUri())
                            .attributes(clientRegistrationId(controller().provider()))
                            .retrieve()
                            .bodyToMono(Long::class.java)
    }

    /**
     * Demo to see how can we implement circuit breaker.
     * For more information, see:
     * https://spring.io/guides/gs/cloud-circuit-breaker/
     */
    fun countByName(circuitBreaker: ReactiveCircuitBreaker,
                    filter: String): Mono<Long> {
        return circuitBreaker.run( controller().webclient.get()
                                    .uri(controller().uri().path("/${controller().baseUrlMany}/nombre/count")
                                        .queryParam("nombre",filter.trim { it <= ' ' })
                                        .build().toUri())
                                    .attributes(clientRegistrationId(controller().provider()))
                                    .retrieve()
                                    .bodyToMono(Long::class.java)
        ) { throwable ->
            logger.error("Alguno de los dos micro.servicios del IAM (o ambos se encuentra NO disponibles) se regresa un cero", throwable)
            Mono.just(0)        // return cero that it is an empty list
        }
    }

    fun findByName(filter: String, pageable: Pageable): Page<T> {
        val res =  controller().webclient.get()
                                .uri(controller().uri().path("/${controller().baseUrlMany}/nombre")
                                    .queryParam("nombre", filter.trim { it <= ' ' })
                                    .queryParam("page", pageable.pageNumber)
                                    .queryParam("size", pageable.pageSize)
                                    .queryParam("sort", pageable.sort)
                                    .build().toUri())
                                .attributes(clientRegistrationId(controller().provider()))
                                .retrieve()
                                .bodyToMono(controller().clazzes())
                                .timeout(Duration.ofMillis(10_000))
                                .switchIfEmpty(controller().switchIfEmpty() as Mono<out Nothing>)
                                .doOnNext { s -> logger.debug("findByName: {}", (s as Array<T>).contentToString()) }
                                .map{ elements -> listOf(elements) }
                                as  Mono<List<T>>

        val total = LongSupplier { countByName(filter).block()!! }
        val elements = res.block()!!.first() as Array<T>

        return PageableExecutionUtils.getPage(elements.toMutableList(), pageable, total)
    }

    /**
     * Demo to see how can we implement circuit breaker.
     * For more information, see:
     * https://spring.io/guides/gs/cloud-circuit-breaker/
     */
    fun findByName(circuitBreaker: ReactiveCircuitBreaker,
                   filter: String, pageable: Pageable): Page<T> {
        val res = circuitBreaker.run(
                            controller().webclient.get()
                            .uri(controller().uri().path("/${controller().baseUrlMany}/nombre")
                                .queryParam("nombre", filter.trim { it <= ' ' })
                                .queryParam("page", pageable.pageNumber)
                                .queryParam("size", pageable.pageSize)
                                .queryParam("sort", pageable.sort)
                                .build().toUri())
                            .attributes(clientRegistrationId(controller().provider()))
                            .retrieve()
                            .bodyToMono(controller().clazzes())
                            .timeout(Duration.ofMillis(10_000))
                            .switchIfEmpty(controller().switchIfEmpty() as Mono<out Nothing>)
                            .doOnNext { s -> logger.debug("findByName: {}", (s as Array<T>).contentToString()) }
                            .map{ elements -> listOf(elements) }
                                as  Mono<List<T>>
        ) { throwable ->
            logger.error("Alguno de los servicios el IAM (o ambos se encuentra NO disponible NO se lista ninguna facultad", throwable)
            Mono.just(ArrayList<T>(0))
        }
        val total = LongSupplier { countByName(filter).block()!! }
        val elements = if (res.block()!!.isEmpty()) res.block()!! else (res.block()!!.first() as Array<T>).toMutableList()

        return PageableExecutionUtils.getPage(elements, pageable, total)
    }

    fun countActive(activo: Boolean): Mono<Long> {
        return controller().webclient.get()
            .uri(controller().uri().path("/${controller().baseUrlMany}/activo/count")
                .queryParam("activo", booleanStr(activo))
                .build().toUri())
            .attributes(clientRegistrationId(controller().provider()))
            .retrieve()
            .bodyToMono(Long::class.java)
    }

    /**
     * Demo to see how can we implement circuit breaker.
     * For more information, see:
     * https://spring.io/guides/gs/cloud-circuit-breaker/
     */
    fun countActive(circuitBreaker: ReactiveCircuitBreaker, activo: Boolean): Mono<Long> {
        return circuitBreaker.run( controller().webclient.get()
                                    .uri(controller().uri().path("/${controller().baseUrlMany}/activo/count")
                                        .queryParam("activo", booleanStr(activo))
                                        .build().toUri())
                                    .attributes(clientRegistrationId(controller().provider()))
                                    .retrieve()
                                    .bodyToMono(Long::class.java)
        ) { throwable ->
            logger.error("Alguno de los dos micro.servicios del IAM (o ambos se encuentra NO disponibles) se regresa un cero", throwable)
            Mono.just(0)        // return cero that it is an empty list
        }
    }

    fun findActive(activo: Boolean, pageable: Pageable): Page<T> {
        val res =  controller().webclient.get()
                                .uri(controller().uri().path("/${controller().baseUrlMany}/activo")
                                    .queryParam("activo", booleanStr(activo))
                                    .queryParam("page", pageable.pageNumber)
                                    .queryParam("size", pageable.pageSize)
                                    .queryParam("sort", pageable.sort)
                                    .build().toUri())
                                .attributes(clientRegistrationId(controller().provider()))
                                .retrieve()
                                .bodyToMono(controller().clazzes())
                                .timeout(Duration.ofMillis(10_000))
                                .switchIfEmpty(controller().switchIfEmpty() as Mono<out Nothing>)
                                .doOnNext { s -> logger.debug("findActive: {}", (s as Array<T>).contentToString()) }
                                .map{ elements -> listOf(elements) }
                                    as  Mono<List<T>>

        val total = LongSupplier { countActive(activo).block()!! }
        val elements = res.block()!!.first() as Array<T>

        return PageableExecutionUtils.getPage(elements.toMutableList(), pageable, total)
    }

    fun findActive(circuitBreaker: ReactiveCircuitBreaker, activo: Boolean, pageable: Pageable): Page<T> {
        val res = circuitBreaker.run( controller().webclient.get()
                                    .uri(controller().uri().path("/${controller().baseUrlMany}/activo")
                                        .queryParam("activo", booleanStr(activo))
                                        .queryParam("page", pageable.pageNumber)
                                        .queryParam("size", pageable.pageSize)
                                        .queryParam("sort", pageable.sort)
                                        .build().toUri())
                                    .attributes(clientRegistrationId(controller().provider()))
                                    .retrieve()
                                    .bodyToMono(controller().clazzes())
                                    .timeout(Duration.ofMillis(10_000))
                                    .switchIfEmpty(controller().switchIfEmpty() as Mono<out Nothing>)
                                    .doOnNext { s -> logger.debug("findActive: {}", (s as Array<T>).contentToString()) }
                                    .map{ elements -> listOf(elements) }
                                        as  Mono<List<T>>
        ) { throwable ->
            logger.error("Alguno de los servicios el IAM (o ambos se encuentra NO disponible NO se lista ninguna facultad", throwable)
            Mono.just(ArrayList<T>(0))
        }

        val total = LongSupplier { countActive(activo).block()!! }
        val elements = if (res.block()!!.isEmpty()) res.block()!! else (res.block()!!.first() as Array<T>).toMutableList()

        return PageableExecutionUtils.getPage(elements, pageable, total)
    }

    fun count(): Mono<Long> {
        return controller().webclient.get()
                            .uri(controller().uri().path("/${controller().baseUrlMany}/count")
                                .build().toUri())
                            .attributes(clientRegistrationId(controller().provider()))
                            .retrieve()
                            .bodyToMono(Long::class.java)
    }

    /**
     * Demo to see how can we implement circuit breaker.
     * For more information, see:
     * https://spring.io/guides/gs/cloud-circuit-breaker/
     */
    fun count(circuitBreaker: ReactiveCircuitBreaker): Mono<Long> {
        return circuitBreaker.run(
            controller().webclient.get()
                .uri(controller().uri().path("/${controller().baseUrlMany}/count")
                    .build().toUri())
                .attributes(clientRegistrationId(controller().provider()))
                .retrieve()
                .bodyToMono(Long::class.java)
        ) { throwable ->
            logger.error("Alguno de los dos micro.servicios del IAM (o ambos se encuentra NO disponibles) se regresa un cero", throwable)
            Mono.just(0)        // return cero that it is an empty list
        }
    }

    fun findAll(pageable: Pageable): Page<T> {
        val res =  controller().webclient.get()
                                    .uri(controller().uri().path("/${controller().baseUrlMany}")
                                        .queryParam("page", pageable.pageNumber)
                                        .queryParam("size", pageable.pageSize)
                                        .queryParam("sort", pageable.sort)
                                        .build().toUri())
                                    .attributes(clientRegistrationId(controller().provider()))
                                    .retrieve()
                                    .bodyToMono(controller().clazzes())
                                    .timeout(Duration.ofMillis(10_000))
                                    .switchIfEmpty(controller().switchIfEmpty() as Mono<out Nothing>)
                                    .doOnNext { s -> logger.debug("find all: {}", (s as Array<T>).contentToString()) }
                                    .map{ elements -> listOf(elements) }
                                    as  Mono<List<T>>

        val total = LongSupplier { count().block()!! }
        val elements = res.block()!!.first() as Array<T>

        return PageableExecutionUtils.getPage(elements.toMutableList(), pageable, total)
    }

    /**
     * Demo to see how can we implement circuit breaker.
     * Of course these three methods must be in the super class
     *
     * For more information, see:
     * https://spring.io/guides/gs/cloud-circuit-breaker/
     */
    fun findAll(circuitBreaker: ReactiveCircuitBreaker, pageable: Pageable): Page<T> {
        val res = circuitBreaker.run(
            controller().webclient.get()
                .uri(controller().uri().path("/${controller().baseUrlMany}")
                    .queryParam("page", pageable.pageNumber)
                    .queryParam("size", pageable.pageSize)
                    .queryParam("sort", pageable.sort)
                    .build().toUri())
                .attributes(clientRegistrationId(controller().provider()))
                .retrieve()
                .bodyToMono(controller().clazzes())
                .timeout(Duration.ofMillis(10_000))
                .switchIfEmpty(controller().switchIfEmpty() as Mono<out Nothing>)
                .doOnNext { s -> logger.debug("find all: {}", (s as Array<T>).contentToString()) }
                .map{ elements -> listOf(elements) }
                    as  Mono<List<T>>
        ) { throwable ->
            logger.error("Alguno de los servicios el IAM (o ambos se encuentra NO disponible NO se lista ninguna facultad", throwable)
            Mono.just(ArrayList<T>(0))
        }

        val total = LongSupplier { count().block()!! }
        val elements = if (res.block()!!.isEmpty()) res.block()!! else (res.block()!!.first() as Array<T>).toMutableList()

        return PageableExecutionUtils.getPage(elements, pageable, total)
    }

    fun findById(id: Long): Mono<T> {
        return controller().webclient.get()
                .uri(controller().uri().path("/${controller().baseUrlSingle}/by/id")
                    .queryParam("id", id)
                    .build().toUri())
                .attributes(clientRegistrationId(controller().provider()))
                .retrieve()
                .bodyToMono(controller().clazz())
                .doOnNext { s -> logger.debug("findById: {}", s.toString()) }
                as Mono<T>
    }

    /**
     * Demo to see how can we implement circuit breaker.
     * Of course these three methods must be in the super class
     *
     * For more information, see:
     * https://spring.io/guides/gs/cloud-circuit-breaker/
     */
    fun findById(circuitBreaker: ReactiveCircuitBreaker, id: Long): Mono<T> {
        return circuitBreaker.run( controller().webclient.get()
                                        .uri(controller().uri().path("/${controller().baseUrlSingle}/by/id")
                                            .queryParam("id", id)
                                            .build().toUri())
                                        .attributes(clientRegistrationId(controller().provider()))
                                        .retrieve()
                                        .bodyToMono(controller().clazz())
                                        .doOnNext { s -> logger.debug("findById: {}", s.toString()) }
                                            as Mono<T>
        ) { throwable ->
            logger.error("Alguno de los servicios el IAM (o ambos se encuentra NO disponible NO se lista ninguna facultad", throwable)
            Mono.empty()
        }
    }

    fun findByName(nombre: String): Mono<T> {
        return controller().webclient.get()
            .uri(controller().uri().path("/${controller().baseUrlSingle}/by/nombre")
                .queryParam("nombre", nombre)
                .build().toUri())
            .attributes(clientRegistrationId(controller().provider()))
            .retrieve()
            .bodyToMono(controller().clazz())
            .doOnNext { s -> logger.debug("findByName: {}", s.toString()) }
            as Mono<T>
    }

    /**
     * Demo to see how can we implement circuit breaker.
     * Of course these three methods must be in the super class
     *
     * For more information, see:
     * https://spring.io/guides/gs/cloud-circuit-breaker/
     */
    fun findByName(circuitBreaker: ReactiveCircuitBreaker, nombre: String): Mono<T> {
        return circuitBreaker.run( controller().webclient.get()
                                        .uri(controller().uri().path("/${controller().baseUrlSingle}/by/nombre")
                                            .queryParam("nombre", nombre)
                                            .build().toUri())
                                        .attributes(clientRegistrationId(controller().provider()))
                                        .retrieve()
                                        .bodyToMono(controller().clazz())
                                        .doOnNext { s -> logger.debug("findByName: {}", s.toString()) }
                                            as Mono<T>
        ) { throwable ->
            logger.error("Alguno de los servicios el IAM (o ambos se encuentra NO disponible NO se lista ninguna facultad", throwable)
            Mono.empty()
        }
    }

    fun booleanStr(activo: Boolean) = if (activo) "1" else "0"

}
