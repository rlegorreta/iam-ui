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
 *  UsuarioCompaniaService.kt
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.ailegorreta.iamui.backend.data.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.ailegorreta.iamui.backend.data.dto.compania.AsignaAreaDTO
import com.ailegorreta.iamui.backend.data.dto.compania.UsuarioDTO
import com.ailegorreta.iamui.backend.data.dto.compania.SolicitudAsignacionDTO
import com.ailegorreta.client.security.service.CurrentSession
import com.ailegorreta.iamui.config.ServiceConfig
import com.ailegorreta.iamui.ui.exceptions.RestClientException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.io.IOException
import java.time.Duration
import java.time.LocalDateTime
import java.util.*
import kotlin.collections.ArrayList

/**
 * UsuarioService to communicate to IAM-server-repo for Usuarios (Adminstration)
 *
 * Add observability example for Spring Boot 3
 *
 *  @author rlh
 *  @project : iam-ui
 *  @date September 2023
 */
@Service
class UsuarioCompaniaService: ClientWebController, CustomWebClientService<UsuarioDTO> {

    private val securityService: CurrentSession

    constructor (mapper: ObjectMapper,
                 @Qualifier("authorization_code_load_balanced") webclient: WebClient,
                 securityService: CurrentSession,
                 serviceConfig: ServiceConfig) : super(mapper, webclient, serviceConfig) {
        this.securityService = securityService
     }

    override val baseUrlMany = "/iam/compania/usuarios/admin"
    override val baseUrlSingle = "/iam/compania/usuario/admin"
    override fun clazz() = UsuarioDTO::class.java
    override fun clazzes() = Array<UsuarioDTO>::class.java
    override fun switchIfEmpty() = Mono.just(arrayOfNulls<UsuarioDTO>(0))
    override fun controller() = this

    override fun toObject(res: String) = toObject(mapper, res, UsuarioDTO::class.java)

    override fun toList(res: String): List<UsuarioDTO> {
        val usuarios = ArrayList<UsuarioDTO>()

        try {
            val json: JsonNode = mapper.readValue(res, JsonNode::class.java)

            if (json.isArray())
                json.forEach {
                    usuarios.add(toObject(mapper, it.toString(), UsuarioDTO::class.java))
                }
            else
                throw RestClientException(RestClientException.STATUS_JSON_CONVERSION, "Se expera una array");
        } catch (e: IOException) {
            throw RestClientException(RestClientException.STATUS_JSON_CONVERSION, e)
        }

        return usuarios
    }

    fun findByIdUsuario(idUsuario: Long) = webclient.get()
                                                    .uri(controller().uri().path("/${controller().baseUrlSingle}/by/idUsuario")
                                                            .queryParam("idUsuario", idUsuario)
                                                            .build().toUri())
                                                    .attributes(clientRegistrationId(controller().provider()))
                                                    .retrieve()
                                                    .bodyToMono(UsuarioDTO::class.java)
                                                    .doOnNext { s -> logger.debug("Find by id usuario: {}", s.toString()) }
                                                    as Mono<UsuarioDTO>


    fun findByNombreUsuario(nombre: String): Mono<UsuarioDTO> {
        return controller().webclient.get()
            .uri(controller().uri().path("/${controller().baseUrlSingle}/by/nombreUsuario")
                .queryParam("nombre", nombre)
                .build().toUri())
            .attributes(ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId(controller().provider()))
            .retrieve()
            .bodyToMono(controller().clazz())
            .doOnNext { s -> logger.debug("findByNombreUsuario: {}", s.toString()) }
                as Mono<UsuarioDTO>
    }

    fun findUsuarioByIdWithSupervisor(id: Long, depth: Boolean = true) = webclient.get()
                                                    .uri(controller().uri().path("/${controller().baseUrlSingle}/withsupervisor/by/id")
                                                        .queryParam("id", id)
                                                        .queryParam("depth", booleanStr(depth))
                                                        .build().toUri())
                                                    .attributes(clientRegistrationId(controller().provider()))
                                                    .retrieve()
                                                    .bodyToMono(UsuarioDTO::class.java)
                                                    .doOnNext { s -> logger.debug("findUsuarioByIdWithSupervisor: {}", s.toString()) }
                                                    as Mono<UsuarioDTO>

    fun findByInternoAndAdministrador(interno: Boolean, administrador: Boolean): Collection<UsuarioDTO> {
        val res = controller().webclient.get()
            .uri(controller().uri().path("/${controller().baseUrlMany}/by/interno")
                .queryParam("interno", booleanStr(interno))
                .queryParam("administrador", booleanStr(administrador))
                .build().toUri())
            .attributes(clientRegistrationId(controller().provider()))
            .retrieve()
            .bodyToMono(controller().clazzes())
            .timeout(Duration.ofMillis(10_000))
            .switchIfEmpty(controller().switchIfEmpty() as Mono<out Nothing>)
            .doOnNext { s -> logger.debug("Found findByInternoAndAdministrador users: {}", (s as Array<UsuarioDTO>).contentToString()) }
            .map { elements -> listOf(elements) }
            as  Mono<List<UsuarioDTO>>

        val result = res.block()!!.first() as Array<UsuarioDTO>

        return result.toMutableList()
    }

    fun saveUsuario(usuario: UsuarioDTO): Mono<UsuarioDTO> {
        usuario.fechaModificacion = LocalDateTime.now()
        usuario.usuarioModificacion = securityService.authenticatedUser.get().name
        logger.error("Se actualizó el usuario: ${usuario.nombre} ${usuario.apellido}")
        return webclient.post()
                        .uri(uri().path("/$baseUrlSingle/add").build().toUri())
                        .accept(MediaType.APPLICATION_JSON)
                        .body(Mono.just(usuario), UsuarioDTO::class.java)
                        .attributes(clientRegistrationId(controller().provider()))
                        .retrieve()
                        .bodyToMono(UsuarioDTO::class.java)
    }

    fun findGrafoAdministrador(nombre: String) = controller().webclient.get()
                                                            .uri(controller().uri().path("/${controller().baseUrlSingle}/grafo/empleados")
                                                                .queryParam("nombreAdministrador", nombre)
                                                                .build().toUri())
                                                            .attributes(clientRegistrationId(controller().provider()))
                                                            .retrieve()
                                                            .bodyToMono(String::class.java)
                                                            .cast(String::class.java)

    fun findGrafoAdministradorMaestro(nombre: String) = controller().webclient.get()
        .uri(controller().uri().path("/${controller().baseUrlSingle}/maestro/grafo/empleados")
            .queryParam("nombreAdministrador", nombre)
            .build().toUri())
        .attributes(clientRegistrationId(controller().provider()))
        .retrieve()
        .bodyToMono(String::class.java)
        .cast(String::class.java)

    fun assignArea(asignaArea: AsignaAreaDTO) = webclient.post()
                        .uri(uri().path("/$baseUrlSingle/add/area").build().toUri())
                        .accept(MediaType.APPLICATION_JSON)
                        .body(Mono.just(asignaArea), AsignaAreaDTO::class.java)
                        .attributes(clientRegistrationId(controller().provider()))
                        .retrieve()
                        .bodyToMono(AsignaAreaDTO::class.java)

    fun unAssignArea(unAsignaArea: AsignaAreaDTO) = webclient.post()
                        .uri(uri().path("/$baseUrlSingle/delete/area").build().toUri())
                        .accept(MediaType.APPLICATION_JSON)
                        .body(Mono.just(unAsignaArea), AsignaAreaDTO::class.java)
                        .attributes(clientRegistrationId(controller().provider()))
                        .retrieve()
                        .bodyToMono(AsignaAreaDTO::class.java)

    fun findEmpleadosAssigned(idArea: Long): Collection<UsuarioDTO> {
        val res =  controller().webclient.get()
            .uri(controller().uri().path("/${controller().baseUrlMany}/asignados/area")
                .queryParam("idArea", idArea)
                .build().toUri())
            .attributes(clientRegistrationId(controller().provider()))
            .retrieve()
            .bodyToMono(controller().clazzes())
            .timeout(Duration.ofMillis(10_000))
            .switchIfEmpty(controller().switchIfEmpty() as Mono<out Nothing>)
            .doOnNext { s -> logger.debug("Found users: {}", (s as Array<UsuarioDTO>).contentToString()) }
            .map{ elements -> listOf(elements) }
            as  Mono<Collection<UsuarioDTO>>

        val result = res.block()!!.first() as Array<UsuarioDTO>

        return result.toMutableList()
    }

    /*
     * Hierarchical data set for solicitudes. Usuarios 1:m Areas
     */
    fun findSolicitudesAsignacion(): Collection<SolicitudAsignacionDTO> {
        val res = controller().webclient.get()
                                    .uri(controller().uri().path("/${controller().baseUrlMany}/solicitudes/asignacion")
                                        .build().toUri())
                                    .attributes(clientRegistrationId(controller().provider()))
                                    .retrieve()
                                    .bodyToMono(controller().clazzes())
                                    .timeout(Duration.ofMillis(10_000))
                                    .switchIfEmpty(controller().switchIfEmpty() as Mono<out Nothing>)
                                    .doOnNext { s -> logger.debug("Found findSolicitudesAsignacion usuarios: {}", (s as Array<UsuarioDTO>).contentToString()) }
                                    .map{ elements -> listOf(elements) }
                                    as  Mono<List<UsuarioDTO>>
        val solicitudes: ArrayList<SolicitudAsignacionDTO> = ArrayList()
        val result = res.block()!!.first() as Array<UsuarioDTO>
        val employees = result.toMutableList()

        employees.forEach {
            val employee = SolicitudAsignacionDTO(nombre = it.nombreCompleto(), nombreUsuario = it.nombreUsuario!!,
                                                  activo = it.activo, asignado = null)

            solicitudes.add(employee)
            for (asignado in it.areas)
                if (!asignado.activo)
                    employee.areas.add(
                        SolicitudAsignacionDTO(nombre = asignado.area.nombre,
                                                              nombreUsuario = employee.nombreUsuario,
                                                              idArea = asignado.area.idArea,
                                                              nombreArea = asignado.area.nombre,
                                                              activo = null, asignado = asignado.activo)
                    )
        }

        return solicitudes
    }

    /*
     * Approval for an Employee to be assigned to an Area
     */
    fun approveSolicitudAsignacion(nombreUsuario: String, idArea: Long, approve: Boolean) = webclient.post()
                                            .uri(uri().path("/$baseUrlSingle/solicitud/aprobar").build().toUri())
                                            .accept(MediaType.APPLICATION_JSON)
                                            .body(Mono.just(AproveAsingacionDTO(nombreUsuario, idArea, approve)), AproveAsingacionDTO::class.java)
                                            .attributes(clientRegistrationId(controller().provider()))
                                            .retrieve()
                                            .bodyToMono(AproveAsingacionDTO::class.java)

    data class AproveAsingacionDTO constructor (val nombreUsuario: String, val idArea: Long, val approve: Boolean)
}

