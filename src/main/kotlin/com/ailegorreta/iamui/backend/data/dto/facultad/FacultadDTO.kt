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
*  FacultadDTO.kt
*
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
*/
package com.ailegorreta.iamui.backend.data.dto.facultad

import java.util.Locale

import com.fasterxml.jackson.annotation.*
import com.ailegorreta.iamui.backend.data.dto.DnDDTO

import com.vaadin.flow.shared.util.SharedUtil
import java.time.LocalDateTime
import jakarta.validation.constraints.*

/**
 * Data class for FacultadDTO. 
 *
 * @author rlh
 * @project : iam-ui
 * @date July 2023
 */
@JsonIgnoreProperties(value = ["activoStr"])
data class FacultadDTO @JvmOverloads constructor(override var id : Long? = null,
												 @field:NotNull(message = "campo requerido")
												 @field:Size(min = 4, max = 8, message = "Nombre de la facultad de 4 a 8 caracteres")
											     var nombre: String,
												 @field:Size(min = 0, max = 40, message = "El nombre de la facultad no puede ser de mas de 40 caracteres")
												 var descripcion: String?,
												 var tipo: FacultadTipo = FacultadTipo.SIMPLE,
												 var usuarioModificacion: String,
												 var fechaModificacion: LocalDateTime = LocalDateTime.now(),
												 @JsonProperty("activo")
												 var activo: Boolean = true): DnDDTO {

	fun getActivoStr() = if (activo) "Activa" else "Suspendida"
	
	fun setActivoStr(activoStr: String):Unit {
		activo = activoStr.equals("Activa")
	}
			
	override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FacultadDTO) return false

		return nombre == other.nombre
	}

	override fun hashCode(): Int = id?.hashCode() ?: 0

}

enum class FacultadTipo(val initials: String) {
	SIMPLE("FS"),
	HORARIO("FH"),
	FISICA("FF"),
	SISTEMA("FS");

	fun  getDisplayName() = SharedUtil.capitalize(name.lowercase(Locale.ENGLISH))
}
