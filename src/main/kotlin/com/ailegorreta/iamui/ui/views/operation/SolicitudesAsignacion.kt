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
 *  SolicitudesAsignacion.kt
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.ailegorreta.iamui.ui.views.operation

import com.github.mvysny.karibudsl.v10.*
import com.ailegorreta.iamui.backend.data.Role
import com.ailegorreta.iamui.backend.data.dto.compania.SolicitudAsignacionDTO

import com.ailegorreta.iamui.backend.data.service.UsuarioCompaniaService
import com.ailegorreta.client.components.ui.flexBoxLayout
import com.ailegorreta.client.components.ui.layout.size.*
import com.ailegorreta.client.components.utils.UIUtils
import com.ailegorreta.client.components.utils.css.*
import com.ailegorreta.iamui.ui.MainLayout
import com.ailegorreta.iamui.ui.dataproviders.SolicitudesAprobacionGridDataProvider
import com.ailegorreta.iamui.ui.views.ViewFrame
import com.vaadin.flow.component.AttachEvent
import com.vaadin.flow.component.grid.ColumnTextAlign
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.icon.Icon
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.treegrid.TreeGrid
import com.vaadin.flow.data.renderer.ComponentRenderer
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import jakarta.annotation.security.RolesAllowed

/**
 *  Page that approves assignation to Company's areas to employees.
 *
 * @author rlh
 * @project : iam-ui
 * @date September 2023
 **/
@Component
@Scope("prototype")
@PageTitle("Aprobacion de asignaciones")
@Route(value = "solasignacion", layout = MainLayout::class)
@RolesAllowed(Role.USER_IAM, Role.ADMIN_IAM, Role.ALL)
class SolicitudesAsignacion(usuarioCompaniaService: UsuarioCompaniaService) : ViewFrame() {

    private val service: UsuarioCompaniaService = usuarioCompaniaService
    private val dataProvider: SolicitudesAprobacionGridDataProvider

    init {
        dataProvider = SolicitudesAprobacionGridDataProvider(usuarioCompaniaService.findSolicitudesAsignacion())
        setViewContent(Content(service, dataProvider ))
    }

    override fun onAttach(attachEvent: AttachEvent?) {
        super.onAttach(attachEvent)
    }

    class Content(val service: UsuarioCompaniaService,
                  val dataProvider: SolicitudesAprobacionGridDataProvider
    ) : KComposite() {

        private lateinit var grid: TreeGrid<SolicitudAsignacionDTO>

        val root = ui {
            flexBoxLayout {
                setBoxSizing(BoxSizing.BORDER_BOX)
                setHeightFull()
                setPadding(Horizontal.RESPONSIVE_X, Top.RESPONSIVE_X)
                verticalLayout {
                    isPadding = false; content { align(stretch, top) }
                    label("SOLICITUD DE APROBACION DE ASIGNACIONES:")
                    grid = treeGrid(dataProvider = dataProvider) {
                        setId("usuarios")
                        setSelectionMode(Grid.SelectionMode.SINGLE)
                        addHierarchyColumn(SolicitudAsignacionDTO::nombre).setHeader("Nombre")
                        addColumn(ComponentRenderer<Icon, SolicitudAsignacionDTO>({ agente -> createActive(agente.activo) })).apply {
                            setAutoWidth(true)
                            flexGrow = 0
                            setHeader("Activo")
                            setTextAlign(ColumnTextAlign.END)
                        }
                        addComponentColumn { agente -> createAsignacionCheckButton(agente) }.apply {
                            setAutoWidth(true)
                            flexGrow = 0
                            setHeader("Autorización")
                        }
                    }
                }
            }
        }

        private fun createActive(isActivo: Boolean?): Icon =
                if (isActivo == null)
                    UIUtils.createPrimaryIcon(VaadinIcon.ELLIPSIS_DOTS_H)
                else if (isActivo)
                    UIUtils.createPrimaryIcon(VaadinIcon.CHECK)
                else
                    UIUtils.createDisabledIcon(VaadinIcon.CLOSE)

        private fun createAsignacionCheckButton(agente: SolicitudAsignacionDTO) =
                UIUtils.createCheckbox("Aprobar").apply {
                    isVisible = agente.nombreArea != null
                    onLeftClick {
                        service.approveSolicitudAsignacion(agente.nombreUsuario, agente.idArea!!, it.source.value).block()
                    }
                }

    }
}
