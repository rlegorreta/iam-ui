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
 *  StatService.kt
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.ailegorreta.iamui.backend.data.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.ailegorreta.iamui.config.ServiceConfig
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction
import reactor.core.publisher.Mono
import java.util.*

/**
 * StatService to communicate to IAM-server-repo for Statistics REST services
 * For now the view is disabled for this version.
 *
 *  @author rlh
 *  @project : iam-ui
 *  @date July 2023
 */
@Service
class StatService (mapper: ObjectMapper,
                   @Qualifier("authorization_code") webclient: WebClient,
                   serviceConfig: ServiceConfig
): ClientWebController(mapper, webclient, serviceConfig),
    CustomWebClientService<Int> {

    override val baseUrlMany = "/iam/estadistica/stat"
    override val baseUrlSingle = "/iam/estadistica/stat"
    override fun clazz() = Int::class.java
    override fun clazzes() = Array<Int>::class.java
    override fun switchIfEmpty() = Mono.just(arrayOfNulls<Int>(0))
    override fun controller() = this

    override fun toObject(res: String) = toObject(mapper, res, Int::class.java)

    override fun toList(res: String) : List<Int> { return ArrayList<Int>() }

    fun porcFacultadesInactivas(): Mono<Int> {
        return controller().webclient.get()
            .uri(controller().uri().path("/${controller().baseUrlSingle}/facultades/inactivas/porcentaje")
                .build().toUri())
            .attributes(ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId(controller().provider()))
            .retrieve()
            .bodyToMono(Int::class.java)
            .cast(Int::class.java)
    }

    fun porcRolesInactivos(): Mono<Int> {
        return controller().webclient.get()
            .uri(controller().uri().path("/${controller().baseUrlSingle}/roles/inactivos/porcentaje")
                .build().toUri())
            .attributes(ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId(controller().provider()))
            .retrieve()
            .bodyToMono(Int::class.java)
            .cast(Int::class.java)
    }

    fun porcPerfilesInactivos(): Mono<Int> {
        return controller().webclient.get()
            .uri(controller().uri().path("/${controller().baseUrlSingle}/perfiles/inactivos/porcentaje")
                .build().toUri())
            .attributes(ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId(controller().provider()))
            .retrieve()
            .bodyToMono(Int::class.java)
            .cast(Int::class.java)
    }

    fun porcUsuariosInactivos(): Mono<Int> {
        return controller().webclient.get()
            .uri(controller().uri().path("/${controller().baseUrlSingle}/usuarios/inactivos/porcentaje")
                .build().toUri())
            .attributes(ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId(controller().provider()))
            .retrieve()
            .bodyToMono(Int::class.java)
            .cast(Int::class.java)
    }
}
