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
 *  UsuarioDTO.kt
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.ailegorreta.iamui.backend.data.dto.facultad

import com.fasterxml.jackson.annotation.*
import java.time.*

/**
 * Data class for UsuarioDTO. Operational.
 *
 * @author rlh
 * @project : iam-ui
 * @date Septemebr 2023
 */
@JsonIgnoreProperties(value = arrayOf("grupos","companias","areas","supervisor"))
data class UsuarioDTO @JvmOverloads constructor (val id : Long? = null,
                                                 val idUsuario: Long,
                                                 val nombre: String,
                                                 val nombreUsuario: String,
                                                 val apellido: String,
                                                 val telefono: String,
                                                 val mail: String,
                                                 @JsonProperty("activo")
                                                 val activo: Boolean,
                                                 @JsonProperty("administrador")
                                                 val administrador: Boolean,
                                                 @JsonProperty("interno")
                                                 val interno: Boolean,
                                                 val fechaIngreso: LocalDate,
                                                 var usuarioModificacion: String,
                                                 val zonaHoraria: String?,
                                                 val nombreCompania: String? = "",
                                                 var fechaModificacion: LocalDateTime = LocalDateTime.now(),
                                                 var perfil: PerfilDTO? = null,
                                                 var sinFacultades: Collection<FacultadDTO> = ArrayList(),
                                                 var extraFacultades: Collection<FacultadDTO> = ArrayList()) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UsuarioDTO) return false

        return idUsuario == other.idUsuario
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0
}
