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
 *  MainLayout.kt
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */

package com.ailegorreta.iamui.backend.data.dto.eventlogger

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode

/**
 * Data class for Events. This DTO is for send events to the
 * Event Logger.
 *
 * @author rlh
 * @project : iam-ui
 * @date July 2023
 */
data class EventDTO constructor(@JsonProperty("correlationId") var correlationId:String,
                                @JsonProperty("eventType") var eventType: EventType,
                                @JsonProperty("username") var username:String,
                                @JsonProperty("eventName") var eventName:String,
                                @JsonProperty("applicationName") var applicationName:String,
                                @JsonProperty("coreName") var coreName: String,
                                @JsonProperty("company") var company: String,
                                @JsonProperty("eventBody") var eventBody: JsonNode){

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EventDTO) return false

        return correlationId == other.correlationId
    }

    override fun hashCode(): Int = correlationId.hashCode()

    override fun toString() = "username = $username " +
            "correlationId = $correlationId " +
            "eventType = $eventType " +
            "eventName = $eventName " +
            "applicationName = $applicationName " +
            "coreName = $coreName " +
            "company = $company " +
            "eventBody = " + eventBody.toString()
}

enum class EventType {
    /**
     * Only store into a data base
     */
    DB_STORE,
    /**
     * Only store into a TXT file
     */
    FILE_STORE,
    /**
     * Store into a data base and a TXT file
     */
    FULL_STORE,
    /**
     * No store the event
     */
    NON_STORE
}
