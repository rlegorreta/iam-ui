/* Copyright (c) 2022, LMASS Desarrolladores, S.C.
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
 *  SolicitudesAprobacionGridDataProvider.kt
 *
 *  Developed 2022 by LMASS Desarrolladores, S.C. www.lmass.com.mx
 */
package com.ailegorreta.iamui.ui.dataproviders

import com.ailegorreta.iamui.backend.data.dto.compania.SolicitudAsignacionDTO
import com.vaadin.flow.data.provider.hierarchy.AbstractHierarchicalDataProvider
import com.vaadin.flow.data.provider.hierarchy.HierarchicalQuery
import java.io.Serializable
import java.util.stream.Stream

/**
 * A hierarchical Data provider in memory for Solicitued Asigacion
 *
 *  @author rlh
 *  @project : iam-ui
 *  @date July 2023
 */
class SolicitudesAprobacionGridDataProvider constructor (val items: Collection<SolicitudAsignacionDTO>):
                                AbstractHierarchicalDataProvider<SolicitudAsignacionDTO, SolicitudAsignacionFilter>() {

    override fun isInMemory() = true

    override fun hasChildren(solicitudAsignacionDTO: SolicitudAsignacionDTO): Boolean {
        return solicitudAsignacionDTO.areas.size > 0
    }

    override fun getChildCount(query: HierarchicalQuery<SolicitudAsignacionDTO, SolicitudAsignacionFilter>): Int {
        if (query.parent == null)
            return items.size
        else
            return query.parent.areas.size
    }

    override fun fetchChildren(query: HierarchicalQuery<SolicitudAsignacionDTO, SolicitudAsignacionFilter>): Stream<SolicitudAsignacionDTO> {
        if (query.parent == null)
            return items.stream()
        else
            return query.parent.areas.stream()
    }
}

data class SolicitudAsignacionFilter constructor (val filter: String = ""): Serializable {
    companion object {
        val  emptyFilter = SolicitudAsignacionFilter()
    }
}
