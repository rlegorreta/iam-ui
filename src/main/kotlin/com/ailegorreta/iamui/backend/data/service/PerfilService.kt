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
 *  PerfilService.kt
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.ailegorreta.iamui.backend.data.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.ailegorreta.iamui.backend.data.dto.facultad.AssignRolDTO
import com.ailegorreta.iamui.backend.data.dto.facultad.PerfilDTO
import com.ailegorreta.client.security.service.CurrentSession
import com.ailegorreta.iamui.config.ServiceConfig
import com.ailegorreta.iamui.ui.dataproviders.PerfilFilter
import com.ailegorreta.iamui.ui.exceptions.RestClientException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.io.IOException
import java.util.*
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId
import java.time.LocalDateTime

/**
 * PerfilService to communicate to IAM-server-repo for Profiles
 *
 *  @author rlh
 *  @project : IAM-UI
 *  @date March 2023
 */
@Service
class PerfilService: ClientWebController, CustomWebClientService<PerfilDTO> {

	private var securityService : CurrentSession

	constructor (mapper: ObjectMapper,
				 @Qualifier("authorization_code_load_balanced") webclient: WebClient,
				 securityService: CurrentSession,
				 serviceConfig: ServiceConfig
	) : super(mapper, webclient, serviceConfig) {
					 this.securityService = securityService
 				 }

	override val baseUrlMany = "/iam/facultad/perfiles"
	override val baseUrlSingle = "/iam/facultad/perfil"

	override fun clazz() = PerfilDTO::class.java
	override fun clazzes() = Array<PerfilDTO>::class.java
	override fun switchIfEmpty() = Mono.just(arrayOfNulls<PerfilDTO>(0))
	override fun controller() = this

	override fun toObject(res: String) = toObject(mapper, res, PerfilDTO::class.java)
	
	override fun toList(res: String) : List<PerfilDTO> {
		val perfiles = ArrayList<PerfilDTO>()
		
		try {
			var json:JsonNode = mapper.readValue(res, JsonNode::class.java)
			
			if (json.isArray())
				json.forEach {
					perfiles.add(toObject(mapper, it.toString(), PerfilDTO::class.java))
				}
			else
				throw RestClientException(RestClientException.STATUS_JSON_CONVERSION, "Se expera una array");			
		} catch (e: IOException) {
			throw RestClientException(RestClientException.STATUS_JSON_CONVERSION, e)
		}
		
		return perfiles
	}
	
	fun findAnyMatchingActive(filter: Optional<PerfilFilter>, pageable: Pageable): Page<PerfilDTO> {
		return if (filter.isPresent() && filter.get().filter.isNotEmpty())
			if (filter.get().showActive)
				findByNameActive(filter.get().filter, filter.get().showActive, pageable)
			else
				findByName(filter.get().filter, pageable)
		else
			if (filter.isPresent() && filter.get().showActive)
				findActive(filter.get().showActive, pageable)
			else
				findAll(pageable)
	}
	
	fun countAnyMatchingActive(filter: Optional<PerfilFilter>): Mono<Long> {
		return if (filter.isPresent() && !filter.get().filter.isEmpty())
			if (filter.get().showActive)
				countByNameActive(filter.get().filter, filter.get().showActive)
			else
				countByName(filter.get().filter)
		else
			if (filter.isPresent() && filter.get().showActive)
				countActive(filter.get().showActive)
			else
				count()
	}

	fun savePerfil(perfil: PerfilDTO): Mono<PerfilDTO> {
		perfil.fechaModificacion = LocalDateTime.now()
		perfil.usuarioModificacion = securityService.authenticatedUser.get().name

		return webclient.post()
						.uri(uri().path("/$baseUrlSingle/add").build().toUri())
						.accept(MediaType.APPLICATION_JSON)
						.body(Mono.just(perfil), PerfilDTO::class.java)
						.attributes(clientRegistrationId(controller().provider()))
						.retrieve()
						.bodyToMono(PerfilDTO::class.java)
	}

	fun assignRole(assignRol: AssignRolDTO) = webclient.post()
													.uri(uri().path("/$baseUrlSingle/add/rol").build().toUri())
													.accept(MediaType.APPLICATION_JSON)
													.body(Mono.just(assignRol), AssignRolDTO::class.java)
													.attributes(clientRegistrationId(controller().provider()))
													.retrieve()
													.bodyToMono(AssignRolDTO::class.java)
	
	fun unAssignRole(assignRol: AssignRolDTO) = webclient.post()
														.uri(uri().path("/$baseUrlSingle/delete/rol").build().toUri())
														.accept(MediaType.APPLICATION_JSON)
														.body(Mono.just(assignRol), AssignRolDTO::class.java)
														.attributes(clientRegistrationId(controller().provider()))
														.retrieve()
														.bodyToMono(AssignRolDTO::class.java)

	fun findByNameDetail(nombre: String) = webclient.get()
													.uri(controller().uri().path("/by/nombre/detail")
														.queryParam("nombre", nombre)
														.build().toUri())
													.accept(MediaType.APPLICATION_JSON)
													.attributes(clientRegistrationId(controller().provider()))
													.retrieve()
													.bodyToMono(PerfilDTO::class.java)
													.switchIfEmpty(switchIfEmpty() as Mono<out Nothing>)
													.doOnNext { s -> logger.debug("Found perfil: {}", s.toString()) }
													.cast(PerfilDTO::class.java)
}
