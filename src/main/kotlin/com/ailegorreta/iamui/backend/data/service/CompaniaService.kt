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
 *  CompaniaService.kt
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.ailegorreta.iamui.backend.data.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.ailegorreta.iamui.backend.data.dto.compania.CompaniaDTO
import com.ailegorreta.iamui.backend.data.dto.compania.Negocio
import com.ailegorreta.iamui.config.ServiceConfig
import com.ailegorreta.iamui.ui.dataproviders.CompaniaFilter
import com.ailegorreta.iamui.ui.exceptions.RestClientException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.support.PageableExecutionUtils
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.io.IOException
import java.time.Duration
import java.util.*
import java.util.function.LongSupplier

/**
 * CompaniaService to communicate to IAM-server-repo for Companies
 *
 *  @author rlh
 *  @project : iam-ui
 *  @date July 2023
 */
@Service
class CompaniaService: ClientWebController, CustomWebClientService<CompaniaDTO> {

    constructor (mapper: ObjectMapper,
                 @Qualifier("authorization_code_load_balanced") webclient: WebClient,
                 serviceConfig: ServiceConfig
    ) : super(mapper, webclient, serviceConfig)

    override val baseUrlMany = "/iam/compania/companias"
    override val baseUrlSingle = "/iam/compania/compania"
    override fun clazz() = CompaniaDTO::class.java
    override fun clazzes() = Array<CompaniaDTO>::class.java
    override fun switchIfEmpty() = Mono.just(arrayOfNulls<CompaniaDTO>(0))
    override fun controller() = this

    override fun toObject(res: String) = toObject(mapper, res, CompaniaDTO::class.java)

    override fun toList(res: String): List<CompaniaDTO> {
        val companias = ArrayList<CompaniaDTO>()

        try {
            val json: JsonNode = mapper.readValue(res, JsonNode::class.java)

            if (json.isArray)
                json.forEach {
                    companias.add(toObject(mapper, it.toString(), CompaniaDTO::class.java))
                }
            else
                throw RestClientException(RestClientException.STATUS_JSON_CONVERSION, "Se espera una array")
        } catch (e: IOException) {
            throw RestClientException(RestClientException.STATUS_JSON_CONVERSION, e)
        }

        return companias
    }

    override fun count(): Mono<Long> {
        return controller().webclient.get()
            .uri(controller().uri().path("/${controller().baseUrlMany}/clientes/count")
                .build().toUri())
            .attributes(clientRegistrationId(controller().provider()))
            .retrieve()
            .bodyToMono(Long::class.java)
    }

    override fun findAll(pageable: Pageable): Page<CompaniaDTO> {
        val res =  controller().webclient.get()
            .uri(controller().uri().path("/${controller().baseUrlMany}/clientes")
                .queryParam("page", pageable.pageNumber)
                .queryParam("size", pageable.pageSize)
                .queryParam("sort", pageable.sort)
                .build().toUri())
            .attributes(clientRegistrationId(controller().provider()))
            .retrieve()
            .bodyToMono(controller().clazzes())
            .timeout(Duration.ofMillis(10_000))
            .switchIfEmpty(controller().switchIfEmpty() as Mono<out Nothing>)
            .doOnNext { s -> logger.debug("Found companias: {}", (s as Array<CompaniaDTO>).contentToString()) }
            .map{ elements -> listOf(elements) }
                as  Mono<List<CompaniaDTO>>

        val total = LongSupplier { count().block()!! }
        val elements = res.block()!!.first() as Array<CompaniaDTO>

        return PageableExecutionUtils.getPage(elements.toMutableList(), pageable, total)
    }

    fun countByNombreNegocio(filter: String, negocio: String): Mono<Long> {
        return controller().webclient.get()
            .uri(controller().uri().path("/${controller().baseUrlMany}/nombre/negocio/count")
                .queryParam("nombre", filter.trim { it <= ' ' })
                .queryParam("negocio", negocio.trim { it <= ' ' })
                .build().toUri())
            .attributes(clientRegistrationId(controller().provider()))
            .retrieve()
            .bodyToMono(Long::class.java)
            .cast(Long::class.java)
    }

    fun findByNombreNegocio(filter: String, negocio:String, pageable: Pageable): Page<CompaniaDTO> {
        val res =  controller().webclient.get()
            .uri(controller().uri().path("/${controller().baseUrlMany}/nombre/negocio")
                .queryParam("nombre", filter.trim { it <= ' ' })
                .queryParam("negocio", negocio.trim { it <= ' ' })
                .queryParam("page", pageable.pageNumber)
                .queryParam("size", pageable.pageSize)
                .queryParam("sort", pageable.sort)
                .build().toUri())
            .attributes(clientRegistrationId(controller().provider()))
            .retrieve()
            .bodyToMono(controller().clazzes())
            .timeout(Duration.ofMillis(10_000))
            .switchIfEmpty(controller().switchIfEmpty() as Mono<out Nothing>)
            .doOnNext { s -> logger.debug("Found companias: {}", (s as Array<CompaniaDTO>).contentToString()) }
            .map{ elements -> listOf(elements) }
                as  Mono<List<CompaniaDTO>>

        val total = LongSupplier { count().block()!! }

        return PageableExecutionUtils.getPage(res.block()!!.toMutableList(), pageable, total)
    }

    fun countByNegocio(negocio: String): Mono<Long> {
        return controller().webclient.get()
            .uri(controller().uri().path("/${controller().baseUrlMany}/negocio/count")
                .queryParam("negocio", negocio.trim { it <= ' ' })
                .build().toUri())
            .attributes(clientRegistrationId(controller().provider()))
            .retrieve()
            .bodyToMono(Long::class.java)
            .cast(Long::class.java)
    }

    fun findByNegocio(negocio: String, pageable: Pageable): Page<CompaniaDTO> {
        val res =  controller().webclient.get()
            .uri(controller().uri().path("/${controller().baseUrlMany}/negocio")
                .queryParam("negocio", negocio.trim { it <= ' ' })
                .queryParam("page", pageable.pageNumber)
                .queryParam("size", pageable.pageSize)
                .queryParam("sort", pageable.sort)
                .build().toUri())
            .attributes(clientRegistrationId(controller().provider()))
            .retrieve()
            .bodyToMono(controller().clazzes())
            .timeout(Duration.ofMillis(10_000))
            .switchIfEmpty(controller().switchIfEmpty() as Mono<out Nothing>)
            .doOnNext { s -> logger.debug("Found companias: {}", (s as Array<CompaniaDTO>).contentToString()) }
            .map{ elements -> listOf(elements) }
                as  Mono<List<CompaniaDTO>>

        val total = LongSupplier { count().block()!! }

        return PageableExecutionUtils.getPage(res.block()!!.toMutableList(), pageable, total)
    }

    fun findAnyMatchingNegocio(filter: Optional<CompaniaFilter>, pageable: Pageable): Page<CompaniaDTO> {
        if (filter.isPresent && filter.get().filter.isNotEmpty())
            if (filter.get().negocio.equals(Negocio.TODOS))
                return findByName(filter.get().filter, pageable)
            else
                return findByNombreNegocio(filter.get().filter, filter.get().negocio.toString(), pageable)
        else
            if (filter.isPresent && !filter.get().negocio.equals(Negocio.TODOS))
                return findByNegocio(filter.get().negocio.toString(), pageable)
            else
                return findAll(pageable)
    }

    fun countAnyMatchingNegocio(filter: Optional<CompaniaFilter>): Mono<Long> {
        if (filter.isPresent && filter.get().filter.isNotEmpty())
            if (filter.get().negocio.equals(Negocio.TODOS))
                return countByName(filter.get().filter)
            else
                return countByNombreNegocio(filter.get().filter, filter.get().negocio.toString())
        else
            if (filter.isPresent && !filter.get().negocio.equals(Negocio.TODOS))
                return countByNegocio(filter.get().negocio.toString())
            else
                return count()
    }

    fun graphCompaniasByAdministrador(nombre: String) = controller().webclient.get()
        .uri(controller().uri().path("/${controller().baseUrlSingle}/admin/grafo/companias")
            .queryParam("nombreAdministrador", nombre)
            .build().toUri())
        .attributes(clientRegistrationId(controller().provider()))
        .retrieve()
        .bodyToMono(String::class.java)
        .cast(String::class.java)

}
