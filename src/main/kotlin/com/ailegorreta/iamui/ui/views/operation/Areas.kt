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
 *  Areas.kt
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */

package com.ailegorreta.iamui.ui.views.operation

import com.ailegorreta.client.components.ui.Initials
import com.ailegorreta.client.components.ui.ListItem
import com.ailegorreta.client.components.ui.layout.size.Right
import com.ailegorreta.client.components.ui.layout.size.Vertical
import com.ailegorreta.client.components.utils.SimpleDialog
import com.ailegorreta.client.components.utils.UIUtils
import com.ailegorreta.iamui.backend.data.dto.compania.*
import com.github.mvysny.karibudsl.v10.*
import com.github.mvysny.kaributools.refresh
import com.ailegorreta.iamui.backend.data.Role
import com.ailegorreta.client.components.ui.flexBoxLayout
import com.ailegorreta.client.components.ui.searchBarCombo
import com.ailegorreta.client.components.ui.layout.size.Horizontal
import com.ailegorreta.client.components.ui.layout.size.Top
import com.ailegorreta.client.components.utils.css.BoxSizing
import com.ailegorreta.iamui.backend.data.service.CompaniaService
import com.ailegorreta.iamui.backend.data.service.UsuarioCompaniaService
import com.ailegorreta.iamui.ui.MainLayout
import com.ailegorreta.iamui.ui.components.navigation.bar.AppBar
import com.ailegorreta.iamui.ui.dataproviders.CompaniaFilter
import com.ailegorreta.iamui.ui.dataproviders.CompaniasGridDataProvider
import com.ailegorreta.iamui.ui.views.ViewFrame
import com.vaadin.flow.component.AttachEvent
import com.vaadin.flow.component.ClickEvent
import com.vaadin.flow.component.DetachEvent
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.grid.ColumnTextAlign
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.grid.dnd.GridDropMode
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.icon.Icon
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.data.provider.DataProvider
import com.vaadin.flow.data.renderer.ComponentRenderer
import com.vaadin.flow.router.BeforeEvent
import com.vaadin.flow.router.HasUrlParameter
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.shared.Registration
import jakarta.annotation.security.RolesAllowed
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

/**
 *  Pages that assigns and unassigned areas to employees.
 *
 * @author rlh
 * @project : iam-ui
 * @date September 2023
 **/
@Component
@Scope("prototype")
@PageTitle("Asignacion areas compañia")
@Route(value = "areas", layout = MainLayout::class)
@RolesAllowed(Role.USER_IAM, Role.ADMIN_IAM, Role.ALL)
class Areas : ViewFrame {
    private val usuarioCompaniaService: UsuarioCompaniaService
    private val companiaService: CompaniaService
    private val companiasDataProvider: CompaniasGridDataProvider

    constructor(usuarioCompaniaService: UsuarioCompaniaService, companiaService: CompaniaService,
                companiasDataProvider: CompaniasGridDataProvider
    ) {
        this.companiaService = companiaService
        this.usuarioCompaniaService = usuarioCompaniaService
        this.companiasDataProvider = companiasDataProvider
        setViewContent(Content(usuarioCompaniaService, companiasDataProvider))
    }

    override fun onAttach(attachEvent: AttachEvent?) {
        super.onAttach(attachEvent)
    }

    class Content(val usuarioCompaniaService: UsuarioCompaniaService,
                  dataProvider: CompaniasGridDataProvider
    ) : KComposite() {
        private var draggedItems: List<UsuarioDTO> = ArrayList()
        private var selectedOperadora: CompaniaDTO? = null

        val root = ui {
            flexBoxLayout {
                setBoxSizing(BoxSizing.BORDER_BOX)
                setHeightFull()
                setPadding(Horizontal.RESPONSIVE_X, Top.RESPONSIVE_X)

                verticalLayout {
                    isPadding = false; content { align(stretch, top) }

                    searchBarCombo {
                        setPlaceHolder("Búsqueda")
                        this.comboBox().setItems(
                            Negocio.FINANCIERA, Negocio.GOBIERNO,
                                                 Negocio.INDUSTRIAL, Negocio.PARTICULAR, Negocio.TODOS)
                        addFilterChangeListener {
                            if (!this@searchBarCombo.filter.isEmpty())
                                if (this@searchBarCombo.comboboxValue() != null)
                                    dataProvider.setFilter(
                                        CompaniaFilter(this@searchBarCombo.getFilter(),
                                                                          this@searchBarCombo.comboboxValue() as Negocio
                                    )
                                    )
                                else
                                    dataProvider.setFilter(CompaniaFilter(this@searchBarCombo.getFilter()))
                            else
                                if (this@searchBarCombo.comboboxValue() != null)
                                    dataProvider.setFilter(CompaniaFilter(negocio = this@searchBarCombo.comboboxValue() as Negocio))
                                else
                                    dataProvider.setFilter(CompaniaFilter.emptyFilter)
                        }
                        getActionButton().getElement().setAttribute("new-button", true)
                        // Do nothing for this screen in the action button
                        /*
                        addActionClickListener{ }
                         */
                    }
                    treeGrid(dataProvider = dataProvider) {
                        setId("treeGridCompanias")
                        dropMode = GridDropMode.ON_GRID
                        isRowsDraggable = true
                        addSelectionListener{ it.getFirstSelectedItem()
                                                .ifPresent{selectedOperadora = it}
                        }
                        addDropListener{
                            if (selectedOperadora != null) {
                                if (draggedItems.isEmpty())
                                    UIUtils.showNotification("Se requiere seleccionar a una área")
                                else {
                                    val usuarioDTO = draggedItems.first()
                                    val companiaDTO = selectedOperadora

                                    if (companiaDTO!!.negocio.equals(Negocio.NA)) {
                                        // it is one assignment
                                        assignOneArea(usuarioDTO, companiaDTO.idPersona)
                                        this.refresh()
                                    } else {
                                        // multiple assignments
                                        SimpleDialog("Se assignan a TODAS la áreas pertenecientes a ${companiaDTO.nombre}",
                                                            MultipleAssigment(usuarioDTO, companiaDTO, this@Content)
                                        ).open()
                                        this.refresh()
                                    }
                                }
                            } else
                                UIUtils.showNotification("Se requiere seleccionar a una Compañía")
                        }

                        addHierarchyColumn(CompaniaDTO::nombre).setHeader("Nombre")
                        addComponentColumn { fondo -> createAsignacionesFondoButton(fondo) }.apply {
                            isAutoWidth = true
                            flexGrow = 0
                            setHeader("Asignaciones")
                        }
                        columnFor(CompaniaDTO::usuarioModificacion) {
                            isAutoWidth = true
                            flexGrow = 0
                            setHeader("Usuario modificó")
                            setTextAlign(ColumnTextAlign.END)
                        }
                        addColumn(ComponentRenderer<Span, CompaniaDTO>({ compania -> createDate(compania) })).apply {
                            isAutoWidth = true
                            flexGrow = 0
                            setHeader("Fecha modificación")
                            setTextAlign(ColumnTextAlign.END)
                        }
                    }
                    div { }
                    label("EMPLEADOS:")
                    grid(dataProvider = DataProvider.ofCollection(usuarioCompaniaService.findByInternoAndAdministrador(true, false))) {
                        setId("usuarios")
                        isRowsDraggable = true
                        addDragStartListener { draggedItems = it.draggedItems }
                        setSelectionMode(Grid.SelectionMode.NONE)
                        addDragEndListener {
                            if (!draggedItems.isEmpty())  draggedItems = ArrayList()
                        }
                        columnFor(UsuarioDTO::idUsuario)   {
                            isAutoWidth = true
                            flexGrow = 0
                            isFrozen = true
                            setHeader("Id Usuario")
                        }
                        addColumn(ComponentRenderer { usuario -> createUserInfo(usuario) }).apply {
                            setHeader("Nombre")
                            setSortProperty("nombreUsuario")
                        }
                        addComponentColumn { usuario -> createAsignacionesUsuarioButton(usuario) }.apply {
                            isAutoWidth = true
                            flexGrow = 0
                            setHeader("Asignaciones")
                        }
                        addColumn(ComponentRenderer { usuario -> createActive(usuario.activo) }).apply {
                            isAutoWidth = true
                            flexGrow = 0
                            setHeader("Activo")
                            setTextAlign(ColumnTextAlign.END)
                        }
                        addColumn(ComponentRenderer { usuario -> createDateIngreso(usuario) }).apply {
                            isAutoWidth = true
                            flexGrow = 0
                            setHeader("Fecha ingreso")
                            setTextAlign(ColumnTextAlign.END)
                        }
                    }

                }

            }
        }

        private fun createDate(compania: CompaniaDTO): Span = Span(UIUtils.formatLocalDateTime(compania.fechaModificacion))

        private fun createDateIngreso(usuario: UsuarioDTO): Span = Span(UIUtils.formatLocalDate(usuario.fechaIngreso))

        private fun createAsignacionesFondoButton(area: CompaniaDTO) =
                UIUtils.createSmallButton("Asignación", VaadinIcon.EYE).apply {
                    isVisible = area.negocio.equals(Negocio.NA)
                    onLeftClick {
                        UI.getCurrent().navigate(AreaEmpleadosDetalle::class.java, "${area.idPersona}:${area.nombre}")
                    }
                }

        private fun createActive(isActivo: Boolean): Icon =
                if (isActivo)
                    UIUtils.createPrimaryIcon(VaadinIcon.CHECK)
                else
                    UIUtils.createDisabledIcon(VaadinIcon.CLOSE)

        private fun createUserInfo(usuario: UsuarioDTO) =
                ListItem( Initials(if (usuario.interno) (if (usuario.administrador) "EA" else "E_") else (if (usuario.administrador) "CA" else "C_")),
                        usuario.nombreUsuario,
                        usuario.mail).apply {
                    setPadding(Vertical.XS)
                    setSpacing(Right.M)
                }

        private fun createAsignacionesUsuarioButton(usuario: UsuarioDTO) =
                UIUtils.createSmallButton("Asignación", VaadinIcon.EYE).apply {
                    isVisible = usuario.areas.isNotEmpty()
                    onLeftClick {
                        UI.getCurrent().navigate(EmpleadoAreasDetalle::class.java, usuario.id!!.toString())
                    }
                }
        fun assignOneArea(usuarioDTO: UsuarioDTO, idArea: Long) {
            // check that is has not been assigned before
            usuarioDTO.areas.forEach {
                if (it.area.idArea.equals(idArea)) {
                    UIUtils.showNotification("El usuario:${usuarioDTO.nombre} ya tiene asignado al área $idArea")

                    return
                }
            }
           val result = usuarioCompaniaService.assignArea(AsignaAreaDTO(idArea, usuarioDTO)).block()!!

           UIUtils.showNotification("Se asignó al usuario:${usuarioDTO.nombre} a $idArea")
           usuarioDTO.areas = result.usuarioDTO.areas  // update Grid
        }
    }

    class MultipleAssigment(val usuarioDTO: UsuarioDTO, val companiaDTO: CompaniaDTO,
                            val caller: Content) : SimpleDialog.Caller {
        override fun dialogResponseCancel(dialog: SimpleDialog?) {
            UIUtils.showNotification("Se canceló la asignación")
        }

        override fun dialogResponseOk(dialog: SimpleDialog?) {
            companiaDTO.areas.forEach {
                caller.assignOneArea(usuarioDTO, it.idArea)
            }
        }
    }
}

/**
 *  Clase que desasigna las áreas de la Compañía a los vendedores(Empleados) desde
 *  el grid de Empleados
 *
 *  - Se pueden consultar y borrar las áreas  asignadas al empleado.
 *  - Se tiene un solo grid sobre la relación dentro del DTO de usuarios de sus áreas
 *
 *  Utiliza un grid de las áreas 'ya' asignadas al empleado.
 *
 */
@PageTitle("Empleado Areas Detalle")
@Route(value = "empleado-areas-detalle", layout = MainLayout::class)
@RolesAllowed(Role.USER_IAM, Role.ADMIN_IAM, Role.ALL)
class EmpleadoAreasDetalle: ViewFrame, HasUrlParameter<String> {

    private val service: UsuarioCompaniaService
    private var title = " "
    private var appBarListener: Registration? = null

    constructor(service: UsuarioCompaniaService) {
        this.service = service
    }

    override fun onAttach(attachEvent: AttachEvent?) {
        super.onAttach(attachEvent)
        initAppBar()
    }

    override fun onDetach(dettachEvent: DetachEvent?) {
        super.onDetach(dettachEvent)
        appBarListener!!.remove()
    }

    override fun setParameter(beforeEvent: BeforeEvent?, navHistory: String?) {
        val id = navHistory!!

        val usuario = service.findById(id.toLong()).block()

        if (usuario != null) {
            title = usuario.nombre
            UI.getCurrent().page.setTitle(usuario.nombre)
            setViewContent(Content(usuario, service))
        } else
            UIUtils.showNotification("El usuario no se encontró en la base de datos")
    }

    private fun initAppBar(): AppBar? {
        val appBar = MainLayout.get().appBar

        appBar.setNaviMode(AppBar.NaviMode.CONTEXTUAL)
        appBarListener = appBar.contextIcon.addClickListener { _: ClickEvent<Button?>? -> UI.getCurrent().navigate(Areas::class.java) }
        appBar.title = "Areas asignadas al empleado:" +  title

        return appBar
    }

    class Content(val usuario: UsuarioDTO,
                  val service: UsuarioCompaniaService
    ): KComposite() {


        val root = ui {
            verticalLayout {
                isPadding = false; content { align(stretch, top) }
                label("AREAS ASIGNADAS:")
                grid(dataProvider = DataProvider.ofCollection(usuario.areasDTOs())) {
                    addThemeName("mobile")
                    setId("usuarios-fondos")
                    setSelectionMode(Grid.SelectionMode.SINGLE)
                    columnFor(AreaDTO::nombre) {
                        isAutoWidth = true
                        flexGrow = 0
                        isFrozen = true
                        setHeader("Nombre")
                    }
                    addComponentColumn { fondo -> createUnAssign(this@grid, fondo) }.apply {
                        isAutoWidth = true
                        flexGrow = 0
                    }
                }
            }
        }

        private fun createUnAssign(grid: Grid<AreaDTO>, area: AreaDTO) =
                UIUtils.createButton(VaadinIcon.TRASH, ButtonVariant.LUMO_SMALL,
                                     ButtonVariant.LUMO_TERTIARY).apply {
                    addClickListener {
                        val assignAreaDTO = service.unAssignArea(AsignaAreaDTO(area.idArea, usuario)).block()!!

                        usuario.areas = assignAreaDTO.usuarioDTO.areas
                        UIUtils.showNotification("Se desasignó el área:${area.nombre} al empleado:${usuario.nombre}")
                        grid.setItems(DataProvider.ofCollection(usuario.areasDTOs()))
                        // ^ refresh the grid
                    }
                }
    }
}


/**
 *  Clase que desasigna los Fondos a los Empleados desde
 *  el grid de Areas
 *
 *  - Se pueden consultar y borrar los empleados asignados al área.
 *  - Se tiene un solo grid sobre la relación dentro del DTO de fondos y sus usuarios
 *
 *  Utiliza un grid de los empleados 'ya' asignadas al fondo.
 *
 */
@PageTitle("Area Empleados Detalle")
@Route(value = "area-empleados-detalle", layout = MainLayout::class)
@RolesAllowed(Role.USER_IAM, Role.ADMIN_IAM, Role.ALL)
class AreaEmpleadosDetalle: ViewFrame, HasUrlParameter<String> {

    private val service: UsuarioCompaniaService
    private var title = " "
    private var appBarListener: Registration? = null

    constructor(service: UsuarioCompaniaService) {
        this.service = service
    }

    override fun onAttach(attachEvent: AttachEvent?) {
        super.onAttach(attachEvent)
        initAppBar()
    }

    override fun onDetach(dettachEvent: DetachEvent?) {
        super.onDetach(dettachEvent)
        appBarListener!!.remove()
    }

    override fun setParameter(beforeEvent: BeforeEvent?, navHistory: String?) {
        title = navHistory!!.substring(navHistory.indexOf(':') + 1 ,navHistory.length)

        UI.getCurrent().page.setTitle(title)
        setViewContent(Content(navHistory.substring(0, navHistory.indexOf(':')).toLong(), service))
    }

    private fun initAppBar(): AppBar? {
        val appBar = MainLayout.get().appBar

        appBar.setNaviMode(AppBar.NaviMode.CONTEXTUAL)
        appBarListener = appBar.contextIcon.addClickListener { _: ClickEvent<Button?>? -> UI.getCurrent().navigate(Areas::class.java) }
        appBar.title = "Empleados asignados a: $title"

        return appBar
    }

    class Content(private val idArea: Long,
                  val service: UsuarioCompaniaService
    ): KComposite() {


        val root = ui {
            verticalLayout {
                isPadding = false; content { align(stretch, top) }
                label("EMPLEADOS ASIGNADOS:")
                grid(dataProvider = DataProvider.ofCollection(service.findEmpleadosAssigned(idArea))) {
                    addThemeName("mobile")
                    setId("fondos-usuarios")
                    setSelectionMode(Grid.SelectionMode.SINGLE)
                    columnFor(UsuarioDTO::nombre) {
                        isAutoWidth = true
                        flexGrow = 0
                        isFrozen = true
                        setHeader("Nombre")
                    }
                    addComponentColumn { usuario -> createUnAssign(this@grid, usuario, idArea) }.apply {
                        isAutoWidth = true
                        flexGrow = 0
                    }
                }
            }
        }

        private fun createUnAssign(grid: Grid<UsuarioDTO>, usuario: UsuarioDTO, idArea: Long) =
                UIUtils.createButton(VaadinIcon.TRASH, ButtonVariant.LUMO_SMALL,
                        ButtonVariant.LUMO_TERTIARY).apply {
                    addClickListener {
                        val assignAreaDTO = service.unAssignArea(AsignaAreaDTO(idArea, usuario)).block()!!

                        usuario.areas = assignAreaDTO.usuarioDTO.areas
                        UIUtils.showNotification("Se desasignó el área:$idArea al empleado:${usuario.nombre}")
                        grid.setItems(DataProvider.ofCollection(service.findEmpleadosAssigned(idArea)))
                        // ^ refresh the grid
                    }
                }
    }
}


