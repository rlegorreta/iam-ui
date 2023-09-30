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
package com.ailegorreta.iamui.backend.data.dto.compania

import com.fasterxml.jackson.annotation.*
import java.time.*
import jakarta.validation.constraints.*

/**
 * Data class for UsuarioDTO for Compania
 *
 * @author rlh
 * @project : iam-ui
 * @date September 2023
 */
@JsonIgnoreProperties(value = arrayOf("sinFacultades", "extraFacultades", "perfil","activoStr"))
data class UsuarioDTO @JvmOverloads constructor (var id : Long? = null,
                                                 @field:NotNull
                                                 @field:Min(1)
                                                 var idUsuario: Long = 0L,
                                                 @field:Size(min = 4, max = 12, message="el nombre largo debe ser de 4 a 12 caracteres")
                                                 var nombreUsuario: String? = "",
                                                 @field:NotNull(message = "campo requerido")
                                                 @field:Size(min = 3, max = 30, message="el nombre debe ser de 3 a 30 caracteres")
                                                 var nombre: String = "",
                                                 @field:NotNull(message = "campo requerido")
                                                 @field:Size(min = 3, max = 30, message="el apellido debe ser de 3 a 30 caracteres")
                                                 var apellido: String = "",
                                                 @field:NotNull(message = "campo requerido")
                                                 @Pattern(regexp = "^\\(?(\\d{3})\\)?[- ]?(\\d{3})[- ]?(\\d{4})$", message=" Teclear un número telefónico válido")
                                                 var telefono: String,
                                                 @field:NotNull(message = "campo requerido")
                                                 @Email(message = "Teclear un email válido")
                                                 var mail: String,
                                                 @JsonProperty("interno")
                                                 val interno: Boolean,
                                                 @JsonProperty("activo")
                                                 var activo: Boolean = true,
                                                 @JsonProperty("administrador")
                                                 var administrador: Boolean = false,
                                                 var fechaIngreso: LocalDate = LocalDate.now(),
                                                 @field:NotNull(message = "campo requerido")
                                                 var zonaHoraria: String,
                                                 var usuarioModificacion: String,
                                                 var fechaModificacion: LocalDateTime = LocalDateTime.now(),
                                                 var supervisor: UsuarioDTO? = null,
                                                 var grupos: ArrayList<GrupoDTO> = ArrayList(),
                                                 var companias: ArrayList<CompaniaDTO> = ArrayList(),
                                                 var areas: Collection<AsignadoDTO> = ArrayList()) {

    fun getActivoStr() = if (activo) "Activo" else "Suspendido"

    fun setActivoStr(activoStr: String) {
        activo = activoStr.equals("Activo")
    }

    fun nombreCompleto() = "$nombre $apellido"

    fun areasDTOs(): Collection<AreaDTO> {
        val areasDTOs = ArrayList<AreaDTO>()

        areas.forEach { areasDTOs.add(it.area) }

        return areasDTOs
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UsuarioDTO) return false

        return idUsuario == other.idUsuario
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0
}

/**
 * Relationship ASIGNADO.
 *
 * @author rlh
 * @project : IAM UI
 * @date March 2022
 */
data class AsignadoDTO constructor (@JsonProperty("activo")
                                    val activo: Boolean,
                                    val area: AreaDTO) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AsignadoDTO) return false

        return area.id == other.area.id
    }

    override fun hashCode(): Int = area.id?.hashCode() ?: 0
}

