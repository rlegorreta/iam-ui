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
 *  CacheService.kt
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.ailegorreta.iamui.backend.data.service

import com.ailegorreta.commons.utils.HasLogger
import com.ailegorreta.iamui.config.ServiceConfig
import org.asynchttpclient.Dsl.request
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction.oauth2AuthorizedClient
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriComponentsBuilder
import java.time.LocalDate


/**
 * CacheService to communicate to the cache server microservice
 *
 * @author rlh
 * @project iam-ui
 * @date July 2023
 */
@Service
class CacheService(@Qualifier("authorization_code") val webClient: WebClient,
                   private val serviceConfig: ServiceConfig) : HasLogger {

    fun uri(): UriComponentsBuilder = UriComponentsBuilder.fromUriString(serviceConfig.getCacheProvider())

    fun getDay(days: Int): LocalDate? = webClient.get()
                                                .uri(uri().path("/cache/day")
                                                    .queryParam("days", days)
                                                    .build().toUri())
                                                 .attributes(clientRegistrationId(serviceConfig.securityClientId + "-oidc"))
                                //.attributes(oauth2AuthorizedClient(client))
                                // .attributes(ServerOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId(serviceConfig.securityClientId + "-client-credentials"))
                                                .retrieve()
                                                .bodyToMono(LocalDate::class.java)
                                                .block()
}
