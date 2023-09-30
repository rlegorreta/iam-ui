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
 *  UsuarioService.kt
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.ailegorreta.iamui.backend.data.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.ailegorreta.iamui.backend.data.dto.facultad.AssignToUserDTO
import com.ailegorreta.iamui.backend.data.dto.facultad.FacultadDTO
import com.ailegorreta.iamui.backend.data.dto.facultad.UsuarioDTO
import com.ailegorreta.iamui.config.ServiceConfig
import com.ailegorreta.iamui.ui.exceptions.RestClientException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.io.IOException
import java.time.Duration
import java.util.*

/**
 * UsuarioService to communicate to IAM-server-repo for Usuarios (Operational)
 *
 *  @author rlh
 *  @project : iam-ui
 *  @date July 2023
 */
@Service
class UsuarioService(mapper: ObjectMapper,
                     @Qualifier("authorization_code_load_balanced") webclient: WebClient,
                     serviceConfig: ServiceConfig):
                     ClientWebController(mapper, webclient, serviceConfig), CustomWebClientService<UsuarioDTO> {

    override val baseUrlMany = "iam/facultad/usuarios"
    override val baseUrlSingle = "iam/facultad/usuario"

    override fun clazz() = UsuarioDTO::class.java
    override fun clazzes() = Array<UsuarioDTO>::class.java
    override fun switchIfEmpty() = Mono.just(arrayOfNulls<UsuarioDTO>(0))
    override fun controller() = this

    override fun toObject(res: String) = toObject(mapper, res, UsuarioDTO::class.java)

    override fun toList(res: String) : List<UsuarioDTO> {
        val usuarios = ArrayList<UsuarioDTO>()

        try {
            val json:JsonNode = mapper.readValue(res, JsonNode::class.java)

            if (json.isArray())
                json.forEach {
                    usuarios.add(toObject(mapper, it.toString(), UsuarioDTO::class.java))
                }
            else
                throw RestClientException(RestClientException.STATUS_JSON_CONVERSION, "Se espera una array")
        } catch (e: IOException) {
            throw RestClientException(RestClientException.STATUS_JSON_CONVERSION, e)
        }

        return usuarios
    }

    fun findFacultades(nombre: String): List<String> {
        val res = controller().webclient.get()
            .uri(controller().uri().path("/${controller().baseUrlSingle}/facultades")
                .queryParam("nombre", nombre)
                .build().toUri())
            .attributes(ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId(controller().provider()))
            .retrieve()
            .bodyToMono(Array<FacultadDTO>::class.java)
            .timeout(Duration.ofMillis(10_000))
            .switchIfEmpty(controller().switchIfEmpty() as Mono<out Nothing>)
            .doOnNext { s -> logger.debug("Found facultades: {}", (s as Array<FacultadDTO>).contentToString()) }
            .map{ elements -> listOf(elements) }
            as  Mono<List<FacultadDTO>>

        val facultades = ArrayList<String>()

        res.block()!!.forEach{ facultades.add(it.nombre)}

        return facultades
    }

    fun findFacultadesDetail(nombre: String): Collection<FacultadDTO> {
        var res = controller().webclient.get()
            .uri(
                controller().uri().path("/${controller().baseUrlSingle}/facultades")
                    .queryParam("nombre", nombre)
                    .build().toUri()
            )
            .attributes(ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId(controller().provider()))
            .retrieve()
            .bodyToMono(Array<FacultadDTO>::class.java)
            .timeout(Duration.ofMillis(10_000))
            .switchIfEmpty(controller().switchIfEmpty() as Mono<out Nothing>)
            .doOnNext { s -> logger.debug("Found facultades: {}", (s as Array<FacultadDTO>).contentToString()) }
            .map { elements -> listOf(elements) }

        val result = res.block()!!.first() as Array<FacultadDTO>

        return result.toMutableList()
    }

    fun findGrafoFacultades(name: String) = controller().webclient.get()
                                                       .uri(controller().uri().path("/${controller().baseUrlSingle}/grafo/facultades")
                                                            .queryParam("nombre", name)
                                                            .build().toUri())
                                                        .attributes(ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId(controller().provider()))
                                                        .retrieve()
                                                        .bodyToMono(String::class.java)
                                                        .cast(String::class.java)

    fun findByCompany(nombre: String): Collection<UsuarioDTO> {
        val res = controller().webclient.get()
            .uri(controller().uri().path("/${controller().baseUrlMany}/by/compania")
                .queryParam("nombreCompania", nombre)
                .build().toUri())
            .attributes(ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId(controller().provider()))
            .retrieve()
            .bodyToMono(controller().clazzes())
            .timeout(Duration.ofMillis(10_000))
            .switchIfEmpty(controller().switchIfEmpty() as Mono<out Nothing>)
            .doOnNext { s -> logger.debug("Found usuarios by Company: {}", (s as Array<UsuarioDTO>).contentToString()) }
            .map{ elements -> listOf(elements) }

        val result = res.block()!!.first() as Array<UsuarioDTO>

        return result.toMutableList()
    }

    fun findByNameUsuario(nombreUsuario: String): Mono<UsuarioDTO> {
        return controller().webclient.get()
            .uri(controller().uri().path("/${controller().baseUrlSingle}/by/nombreusuario")
                .queryParam("nombreUsuario", nombreUsuario)
                .build().toUri())
            .attributes(ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId(controller().provider()))
            .retrieve()
            .bodyToMono(controller().clazz())
            .doOnNext { s -> logger.debug("findByNameUser: {}", s.toString()) }
    }

    fun assignPerfil(assignPerfil: AssignToUserDTO) = webclient.post()
        .uri(uri().path("/$baseUrlSingle/asigna/perfil").build().toUri())
        .accept(MediaType.APPLICATION_JSON)
        .body(Mono.just(assignPerfil), AssignToUserDTO::class.java)
        .attributes(ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId(controller().provider()))
        .retrieve()
        .bodyToMono(AssignToUserDTO::class.java)

    fun assignFacultadExtra(assignFacultad: AssignToUserDTO) = webclient.post()
        .uri(uri().path("/$baseUrlSingle/asigna/facultad").build().toUri())
        .accept(MediaType.APPLICATION_JSON)
        .body(Mono.just(assignFacultad), AssignToUserDTO::class.java)
        .attributes(ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId(controller().provider()))
        .retrieve()
        .bodyToMono(AssignToUserDTO::class.java)

    fun unAssignFacultadExtra(desAssignFacultad: AssignToUserDTO) = webclient.post()
        .uri(uri().path("/$baseUrlSingle/desasigna/facultad").build().toUri())
        .accept(MediaType.APPLICATION_JSON)
        .body(Mono.just(desAssignFacultad), AssignToUserDTO::class.java)
        .attributes(ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId(controller().provider()))
        .retrieve()
        .bodyToMono(AssignToUserDTO::class.java)

    fun forbidFacultad(forbidFacultad: AssignToUserDTO) = webclient.post()
        .uri(uri().path("/$baseUrlSingle/prohibe/facultad").build().toUri())
        .accept(MediaType.APPLICATION_JSON)
        .body(Mono.just(forbidFacultad), AssignToUserDTO::class.java)
        .attributes(ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId(controller().provider()))
        .retrieve()
        .bodyToMono(AssignToUserDTO::class.java)

    fun unForbidFacultad(desForbidFacultad: AssignToUserDTO) = webclient.post()
        .uri(uri().path("/$baseUrlSingle/desprohibe/facultad").build().toUri())
        .accept(MediaType.APPLICATION_JSON)
        .body(Mono.just(desForbidFacultad), AssignToUserDTO::class.java)
        .attributes(ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId(controller().provider()))
        .retrieve()
        .bodyToMono(AssignToUserDTO::class.java)
}
