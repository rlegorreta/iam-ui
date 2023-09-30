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
 *  GrupoService.kt
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.ailegorreta.iamui.backend.data.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.ailegorreta.iamui.backend.data.dto.compania.GrupoDTO
import com.ailegorreta.iamui.backend.data.dto.compania.NewGrupoDTO
import com.ailegorreta.iamui.config.ServiceConfig
import com.ailegorreta.iamui.ui.exceptions.RestClientException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.io.IOException
import java.util.*

/**
 * GrupoService to communicate to IAM-server-repo for Groups
 *
 *  @author rlh
 *  @project : iam-ui
 *  @date July 2023
 */
@Service
class GrupoService(mapper: ObjectMapper,
                   @Qualifier("authorization_code_load_balanced") webclient: WebClient,
                   serviceConfig: ServiceConfig
): ClientWebController(mapper, webclient, serviceConfig),
    CustomWebClientService<GrupoDTO> {

    override val baseUrlMany = "/iam/compania/grupos"
    override val baseUrlSingle = "/iam/compania/grupo"
    override fun clazz() = GrupoDTO::class.java
    override fun clazzes() = Array<GrupoDTO>::class.java
    override fun switchIfEmpty() = Mono.just(arrayOfNulls<GrupoDTO>(0))
    override fun controller() = this

    override fun toObject(res: String) = toObject(mapper, res, GrupoDTO::class.java)

    override fun toList(res: String) : List<GrupoDTO> {
        val grupos = ArrayList<GrupoDTO>()

        try {
            var json:JsonNode = mapper.readValue(res, JsonNode::class.java)

            if (json.isArray())
                json.forEach {
                    grupos.add(toObject(mapper, it.toString(), GrupoDTO::class.java))
                }
            else
                throw RestClientException(RestClientException.STATUS_JSON_CONVERSION, "Se expera una array");
        } catch (e: IOException) {
            throw RestClientException(RestClientException.STATUS_JSON_CONVERSION, e)
        }

        return grupos
    }

    fun updateGrupo(grupo: GrupoDTO) = webclient.post()
        .uri(uri().path("/$baseUrlSingle/update").build().toUri())
        .accept(MediaType.APPLICATION_JSON)
        .body(Mono.just(grupo), GrupoDTO::class.java)
        .attributes(clientRegistrationId(controller().provider()))
        .retrieve()
        .bodyToMono(GrupoDTO::class.java)

    fun newGrupo(grupo: GrupoDTO, nombre: String) = webclient.post()
        .uri(uri().path("/$baseUrlSingle/add").build().toUri())
        .accept(MediaType.APPLICATION_JSON)
        .body(Mono.just(NewGrupoDTO(grupoDTO = grupo, nombre = nombre)), NewGrupoDTO::class.java)
        .attributes(clientRegistrationId(controller().provider()))
        .retrieve()
        .bodyToMono(GrupoDTO::class.java)

    fun countMiembros(id: Long): Mono<Long> {
        return controller().webclient.get()
            .uri(controller().uri().path("/${controller().baseUrlSingle}/miembros/count")
                .queryParam("id",id)
                .build().toUri())
            .attributes(clientRegistrationId(controller().provider()))
            .retrieve()
            .bodyToMono(Long::class.java)
            .cast(Long::class.java)
    }
}
