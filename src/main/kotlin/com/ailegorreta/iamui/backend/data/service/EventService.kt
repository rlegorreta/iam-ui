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
*  EventService.kt
*
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
*/
package com.ailegorreta.iamui.backend.data.service

import com.ailegorreta.iamui.backend.data.dto.GraphqlRequestBody
import com.ailegorreta.iamui.backend.data.dto.Notification
import com.ailegorreta.commons.utils.HasLogger
import com.ailegorreta.iamui.config.ServiceConfig
import com.ailegorreta.iamui.util.GraphqlSchemaReaderUtil
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono

/**
 * EventService this class does NOT send events to the kafka machine. It just read persistence notifications.
 *
 * The kafka notifications are sent by iam server repo microservice
 *
 *  @author rlh
 *  @project : iam-ui
 *  @date September 2023
 */
@Service
class EventService(@Qualifier("authorization_code") val webClient: WebClient,
                   private val serviceConfig: ServiceConfig): HasLogger {

    fun uri(): UriComponentsBuilder = UriComponentsBuilder.fromUriString(serviceConfig.getAuditProvider())

    /**
     * Calls the Audit server repo microservice to get all persistent events from this day.
     *
     * This method is a support for the user that did not read the on-line event
     */
    fun notifications(): List<Notification> {
        val graphQLRequestBody = GraphqlRequestBody( GraphqlSchemaReaderUtil.getSchemaFromFileName("notifications"),
                            mutableMapOf("username" to  SecurityContextHolder.getContext().authentication!!.name))

        val res = webClient.post()
                            .uri(uri().path("/audit/graphql").build().toUri())
                            .accept(MediaType.APPLICATION_JSON)
                            .body(Mono.just(graphQLRequestBody), GraphqlRequestBody::class.java)
                            .attributes(clientRegistrationId(serviceConfig.securityClientId + "-oidc"))
                            .retrieve()
                            .bodyToMono(GraphqlResponseNotifications::class.java)
                            .block()

        if ((res == null) || (res.errors != null)) {
            logger.error("Error al leer las notificaciones:" + res?.errors)
            return emptyList()
        }

        return res.data!!.notifications
    }

    data class GraphqlResponseNotifications(val data: Data? = null,
                                            val errors: Collection<Map<String, Any>>? = null) {
        data class Data (val notifications: List<Notification> = emptyList())
    }

}
