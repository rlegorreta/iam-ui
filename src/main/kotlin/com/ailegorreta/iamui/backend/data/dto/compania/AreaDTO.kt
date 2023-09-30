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
*  AreasDTO.kt
*
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
*/
package com.ailegorreta.iamui.backend.data.dto.compania

import java.time.LocalDateTime

/**
 * Data class for Areas in CompanyService.
 *
 * @author rlh
 * @project : iam-ui
 * @date September 2023
 */
data class AreaDTO @JvmOverloads constructor(var id : Long? = null,
                                             var nombre: String = "",
                                             var usuarioModificacion: String = "",
                                             var fechaModificacion: LocalDateTime = LocalDateTime.now(),
                                             var activo: Boolean = false,
                                             var idArea: Long = -1,
                                             var idPersona: Long = -1) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AreaDTO) return false

        return nombre == other.nombre
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0
}
