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
*  CompaniaDTO.kt
*
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
*/
package com.ailegorreta.iamui.backend.data.dto.compania

import java.util.*
import com.fasterxml.jackson.annotation.*
import com.ailegorreta.commons.dtomappers.EntityDTOMapper
import com.vaadin.flow.shared.util.SharedUtil
import java.time.LocalDateTime

/**
 * Data class for Company in CompanyService.
 *
 * @author rlh
 * @project : iam-ui
 * @date September 2023
 */
data class CompaniaDTO @JvmOverloads constructor(val id : Long? = null,
                                                 val nombre: String,
                                                 @JsonProperty("padre")
                                                 val padre: Boolean,
                                                 var negocio: Negocio,
                                                 var usuarioModificacion: String,
                                                 var fechaModificacion: LocalDateTime = LocalDateTime.now(),
                                                 var activo: Boolean,
                                                 var idPersona: Long,
                                                 var areas: Collection<AreaDTO> = ArrayList(),
                                                 var subsidiarias: Collection<CompaniaDTO> = ArrayList()) {

    /*
     * Do the mapping from AreaDTO to CompaniaDTO just in order to have a TreeGrid
     */
    companion object : EntityDTOMapper<AreaDTO, CompaniaDTO> {
        override var dtos = HashMap<Int, Any>()

        override fun fromEntityRecursive(entity: AreaDTO): CompaniaDTO {
            val a = dtos.get(entity.hashCode())

            if (a != null)
                return a as CompaniaDTO

            val companiaDTO =  CompaniaDTO(id = entity.id,
                                           nombre = entity.nombre,
                                           padre = false,
                                           negocio = Negocio.NA,
                                           usuarioModificacion = entity.usuarioModificacion,
                                           fechaModificacion = entity.fechaModificacion,
                                           activo = entity.activo,
                                           idPersona = entity.idArea)

            dtos.put(entity.hashCode(), companiaDTO)

            return companiaDTO
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CompaniaDTO) return false

        return id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0
}

enum class Negocio(val initials: String) {
    NA("NA"),
    GOBIERNO("GO"),
    FINANCIERA("FI"),
    INDUSTRIAL("IN"),
    PARTICULAR("PA"),
    TODOS("  ");

    fun  getDisplayName() = SharedUtil.capitalize(name.lowercase(Locale.ENGLISH))
}
