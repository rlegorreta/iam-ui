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
 *  Empleados.kt
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.ailegorreta.iamui.ui.views.operation

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.mvysny.karibudsl.v10.*
import com.github.mvysny.kaributools.refresh
import com.ailegorreta.iamui.backend.data.Role
import com.ailegorreta.iamui.backend.data.dto.DnDDTO
import com.ailegorreta.iamui.backend.data.dto.GraphDTO
import com.ailegorreta.iamui.backend.data.dto.Link
import com.ailegorreta.iamui.backend.data.dto.Node
import com.ailegorreta.iamui.backend.data.dto.facultad.AssignToUserDTO
import com.ailegorreta.iamui.backend.data.dto.facultad.FacultadDTO
import com.ailegorreta.iamui.backend.data.dto.facultad.PerfilDTO
import com.ailegorreta.iamui.backend.data.dto.facultad.UsuarioDTO
import com.ailegorreta.iamui.backend.data.service.CompaniaService
import com.ailegorreta.iamui.backend.data.service.FacultadService
import com.ailegorreta.iamui.backend.data.service.UsuarioService
import com.ailegorreta.client.components.ui.Initials
import com.ailegorreta.client.components.ui.ListItem
import com.ailegorreta.client.components.ui.layout.size.*
import com.ailegorreta.client.components.ui.searchBar
import com.ailegorreta.client.components.utils.SimpleDialog
import com.ailegorreta.client.components.utils.UIUtils
import com.ailegorreta.client.navigation.detailsdrawer.DetailsDrawer
import com.ailegorreta.client.security.service.CurrentSession
import com.ailegorreta.iamui.ui.MainLayout
import com.ailegorreta.iamui.ui.components.Graph
import com.ailegorreta.iamui.ui.components.navigation.bar.AppBar
import com.ailegorreta.iamui.ui.dataproviders.FacultadesGridDataProvider
import com.ailegorreta.iamui.ui.dataproviders.FacultadesGridDataProvider.FacultadFilter
import com.ailegorreta.iamui.ui.dataproviders.PerfilFilter
import com.ailegorreta.iamui.ui.dataproviders.PerfilesGridDataProvider
import com.ailegorreta.iamui.ui.views.SplitViewFrame
import com.ailegorreta.iamui.ui.views.ViewFrame
import com.vaadin.flow.component.*
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.grid.ColumnTextAlign
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.grid.dnd.*
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.icon.Icon
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.data.provider.DataProvider
import com.vaadin.flow.data.provider.ListDataProvider
import com.vaadin.flow.data.renderer.ComponentRenderer
import com.vaadin.flow.router.BeforeEvent
import com.vaadin.flow.router.HasUrlParameter
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.shared.Registration
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import java.io.IOException
import java.time.LocalDateTime
import java.util.*
import jakarta.annotation.security.RolesAllowed

/**
 *  Principal page for Employees administration:
 *  - Assign a profile to an employee
 *  - Assign an extra permit to an employee (or unassigned the extra permit).
 *  - Forbid an individual permit to an employee (or unforbid the permit).
 *
 *  The main age display a graph for all Companies that the Administrator can
 *  access. And shows the appropriate actions when the Administrator clicks
 *  on one Company graph node.
 *
 *  Kotlin classes in this file
 *  - Empleados: main page that display the companies graph.
 *  - GraphAdministrador: small data class to unparse the graph came from the
 *    IAM-repo-server
 *  - EmpleadosPerfiles: page that assign and updates the Usuario->Perfil
 *    relationship.
 *  - EmpleadosFacultades: page that assign individual permits to the User
 *  - EmpleadosFacultadesDetalle: page tha queries or deletes extra permits
 *    assigned to a user. Called by EmpleadosFacultades page.
 *  - EmpleadosForbidFacultades: page thta adds forbidden permit to an Employee
 *    or can erase the forbidden permit.
 *
 * @author rlh
 * @project : iam-ui
 * @date September 2023
 */
@Component
@Scope("prototype")
@PageTitle("Empleados")
@Route(value = "empleados", layout = MainLayout::class)
@RolesAllowed(Role.USER_IAM, Role.ADMIN_IAM, Role.ALL)
class Empleados(securityService: CurrentSession,
                service: CompaniaService,
                graph: Graph,
                mapper: ObjectMapper) : SplitViewFrame() {

    init {
        setViewContent(Content(securityService, service, graph, mapper))
    }

    class Content(val securityService: CurrentSession,
                  val service: CompaniaService,
                  val graph: Graph, val mapper: ObjectMapper ) : KComposite() {

        private var node: Node? = null
        var graphAdministrador: GraphAdministrador? = null
        private lateinit var showEmployeesProfiles: Button
        private lateinit var showEmployeesExtraPermits: Button
        private lateinit var showEmployeesForbidPermits: Button

        val root = ui {
            verticalLayout {
                showEmployeesProfiles = button("Asignar perfiles") {
                    icon = Icon(VaadinIcon.GROUP)
                    isVisible = false
                    onLeftClick {
                        UI.getCurrent().navigate(EmpleadosPerfiles::class.java, node!!.idNeo4j)
                    }
                }
                showEmployeesExtraPermits = button("Facultades extraordinarias") {
                    icon = Icon(VaadinIcon.PLUS_SQUARE_O)
                    isVisible = false
                    onLeftClick {
                        UI.getCurrent().navigate(EmpleadosFacultades::class.java, node!!.idNeo4j)
                    }
                }
                showEmployeesForbidPermits = button("Prohibir facultades") {
                    icon = Icon(VaadinIcon.MINUS_SQUARE_O)
                    isVisible = false
                    onLeftClick {
                        UI.getCurrent().navigate(EmpleadosForbidFacultades::class.java, node!!.idNeo4j)
                    }
                }
                label{
                    text = "Dar 'doble-click' en el nodo de Compañía que se desea  trabajar"
                }
                div {
                    val detailsDrawer = DetailsDrawer(DetailsDrawer.Position.BOTTOM, graph)

                    detailsDrawer.setSizeFull()
                    add(graph.createRefresh(""))
                    add(detailsDrawer)
                }
            }
        }

        init {
            graph.addSelectListener{
                val item = it.item

                if (item is Int) {
                    @SuppressWarnings("unchecked")
                    val itemSelected = item

                    node = setButton(itemSelected)
                }
            }
        }

        override fun onAttach(attachEvent: AttachEvent?) {
            graph.setVisibleDnDropZone(false)
            graph.setWidth(700)
            graph.setHeight(700)
            initGraph()
        }

        fun initGraph() {
            val data = service.graphCompaniasByAdministrador(securityService.authenticatedUser.get().name).block()

            try {
                graphAdministrador = mapper.readValue(data, GraphAdministrador::class.java)
                graph.startGraph(CompaniasGrafo(data!!))
            } catch (e: IOException) {
                UIUtils.showNotification("Error NO se pudo leer los datos de la gráfica")
            }
        }

        private fun setButton(itemSelected : Int): Node {
            val node = graphAdministrador!!.getNode(itemSelected)

            showEmployeesProfiles.text = "Asignar perfiles a los empleados de la compañía:" + node.caption
            showEmployeesProfiles.isVisible = true
            // if (SecurityUtils.hasPermit("Admin_Iam")) { // Temporal debe ser ADMIN_IAM ya que usuarios externos no debe de asignar facultades individuales
                showEmployeesExtraPermits.text = "Facultades 'extras' a empleados de la compañía:" + node.caption
                showEmployeesExtraPermits.isVisible = true
                showEmployeesForbidPermits.text = "Prohibir facultades 'individuales' a empleados de la compañia:" + node.caption
                showEmployeesForbidPermits.isVisible = true
            //}

            return node
        }
    }

    private class CompaniasGrafo(private val data: String) : GraphDTO {

        override fun jsonString(mapper: ObjectMapper): String {
            return data
        }

        override fun addNode(mapper: ObjectMapper, node: DnDDTO): String = ""

        override fun deleteNode(node: DnDDTO) {}

        override val dndDropTitle: String
            get() = ""

    }

    /*
     * DTO to read the nodes and edges graph comming from the server
    */
    data class GraphAdministrador(val nodes: List<Node>,
                                  val edges: List<Link>) {
        fun getNode(id: Int) = nodes.asSequence().filter{it.id == id}.first()

        override fun toString() = "nodes:" + nodes.size + " edges:" + edges.size
    }

}

/**
 *  Clase que administra la asignación de Perfiles a los Empleados de una compañía.
 *
 *  La asignación es a través de D&D entre el grid de Empleados y el grid de Perfiles.
 *  EL grid de perfiles utiliza paginación.
 *
 */
@PageTitle("Empleados Perfiles")
@Route(value = "empleados-perfiles", layout = MainLayout::class)
@RolesAllowed(Role.USER_IAM, Role.ADMIN_IAM, Role.ALL)
class EmpleadosPerfiles(private val usuarioService: UsuarioService,
                        private val companiaService: CompaniaService,
                        private val dataProvider: PerfilesGridDataProvider
) : ViewFrame(), HasUrlParameter<Long> {

    private var title = " "
    private var appBarListener: Registration? = null

    override fun onAttach(attachEvent: AttachEvent?) {
        super.onAttach(attachEvent)
        initAppBar()
    }

    override fun onDetach(dettachEvent: DetachEvent?) {
        super.onDetach(dettachEvent)
        appBarListener!!.remove()
    }

    override fun setParameter(beforeEvent: BeforeEvent?, id: Long?) {
        val compania = companiaService.findById(id!!).block()

        if (compania != null) {
            title = compania.nombre
            UI.getCurrent().page.setTitle(compania.nombre)
            setViewContent(Content(compania.nombre, usuarioService, dataProvider))
        } else
            UIUtils.showNotification("La compañía no se encontró en la base de datos")
    }

    private fun initAppBar(): AppBar? {
        val appBar = MainLayout.get().appBar

        appBar.setNaviMode(AppBar.NaviMode.CONTEXTUAL)
        appBarListener = appBar.contextIcon.addClickListener { UI.getCurrent().navigate(Empleados::class.java) }
        appBar.title = "Asignación de perfiles a los empleados de la compañía:$title"

        return appBar
    }

    class Content(val nombreCompania: String,
                  val usuarioService: UsuarioService,
                  val dataProvider: PerfilesGridDataProvider
    ): KComposite() {

        private var draggedItems: List<PerfilDTO> = ArrayList()
        private var selectedUsuario: UsuarioDTO? = null
        private val dialogCaller = DialogCaller(usuarioService, this)
        lateinit var gridUsuarios:Grid<UsuarioDTO>

        val root = ui {
            verticalLayout {
                isPadding = false; content { align(stretch, top) }

                label("EMPLEADOS:")
                gridUsuarios = grid(dataProvider = DataProvider.ofCollection(usuarioService.findByCompany(nombreCompania))) {
                    setId("usuarios")
                    dropMode = GridDropMode.ON_GRID
                    isRowsDraggable = true
                    setSelectionMode(Grid.SelectionMode.SINGLE)
                    addSelectionListener{ it.getFirstSelectedItem()
                                            .ifPresent{selectedUsuario = it}}
                    addDropListener{ 
                        if (selectedUsuario != null) {
                            if (draggedItems.isEmpty())
                                UIUtils.showNotification("Se requiere seleccionar a un Usuario para asignarle un perfil")
                            else {
                                dialogCaller.nombrePerfil = draggedItems.first().nombre
                                dialogCaller.usuarioDTO = selectedUsuario
                                if (selectedUsuario!!.perfil != null)
                                    (SimpleDialog("El usuario:" + selectedUsuario!!.nombreUsuario +
                                                  " tiene el perfil asignado:" + selectedUsuario!!.perfil!!.nombre +
                                                  " ¿Se desea cambiar al perfil:" + draggedItems.first().nombre + "?",
                                                  dialogCaller)).open()
                                else
                                    dialogCaller.dialogResponseOk(null)
                                this.refresh()
                            }
                        } else
                            UIUtils.showNotification("Se requiere seleccionar a un Usuario para asignarle un perfil")
                    }
                    columnFor(UsuarioDTO::id)   {
                        isAutoWidth = true
                        flexGrow = 0
                        isFrozen = true
                        setHeader("ID")
                    }
                    columnFor(UsuarioDTO::idUsuario)   {
                        isAutoWidth = true
                        flexGrow = 0
                        isFrozen = true
                        setHeader("Id Usuario")
                    }
                    columnFor(UsuarioDTO::nombre)   {
                        isAutoWidth = true
                        flexGrow = 0
                        isFrozen = true
                        setHeader("Log-in")
                    }
                    addColumn(ComponentRenderer { usuario -> createUserInfo(usuario) }).apply {
                        setHeader("Nombre")
                        setSortProperty("nombreUsuario")
                    }
                    addColumn(ComponentRenderer { usuario -> createPerfil(usuario) }).apply {
                        isAutoWidth = true
                        flexGrow = 0
                        setHeader("Perfil")
                        setTextAlign(ColumnTextAlign.END)
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
                    columnFor(UsuarioDTO::telefono)   {
                        isAutoWidth = true
                        flexGrow = 0
                        isFrozen = true
                        setHeader("Teléfono")
                    }
                }
                div {  }
                label("PERFILES:")
                searchBar {         // search field de paginación de perfiles
                    setCheckboxText("Activos")
                    setPlaceHolder("Búsqueda")
                    addFilterChangeListener{
                        if (this@searchBar.filter.isNotEmpty() || this@searchBar.isCheckboxChecked)
                            dataProvider.setFilter( PerfilFilter(this@searchBar.filter, this@searchBar.isCheckboxChecked))
                        else
                            dataProvider.setFilter(PerfilFilter.emptyFilter)
                    }
                    actionButton.isVisible = false
                }
                grid(dataProvider = dataProvider) {
                    addThemeName("mobile")
                    setId("perfiles")
                    isRowsDraggable = true
                    addDragStartListener { draggedItems = it.draggedItems }
                    setSelectionMode(Grid.SelectionMode.NONE)
                    addDragEndListener {
                                            if (draggedItems.isNotEmpty())  draggedItems = ArrayList()
                                        }
                    columnFor(PerfilDTO::id) {
                        isAutoWidth = true
                        flexGrow = 0
                        isFrozen = true
                        setHeader("ID")
                    }
                    addColumn(ComponentRenderer { perfil -> createPerfilInfo(perfil) }).apply {
                        setHeader("Perfil")
                        setSortProperty("nombre")
                    }
                    addColumn(ComponentRenderer { perfil -> createActive(perfil.activo) }).apply {
                        isAutoWidth = true
                        flexGrow = 0
                        setHeader("Activo")
                        setTextAlign(ColumnTextAlign.END)
                    }
                    addColumn(ComponentRenderer { perfil -> createDate(perfil) }).apply {
                        isAutoWidth = true
                        flexGrow = 0
                        setHeader("Fecha modificación")
                        setTextAlign(ColumnTextAlign.END)
                    }
                }
            }
        }

        private fun createDateIngreso(usuario: UsuarioDTO): Span = Span(UIUtils.formatLocalDate(usuario.fechaIngreso))

        private fun createPerfilInfo(perfil: PerfilDTO) =
                ListItem(Initials(if (perfil.activo) (if (perfil.patron) " P" else " E") else (if (perfil.patron) "XP" else "XE")),
                        perfil.nombre,
                        perfil.descripcion).apply {
                    setPadding(Vertical.XS)
                    setSpacing(Right.M)
                }

        private fun createActive(isActivo: Boolean): Icon =
                if (isActivo)
                    UIUtils.createPrimaryIcon(VaadinIcon.CHECK)
                else
                    UIUtils.createDisabledIcon(VaadinIcon.CLOSE)

        private fun createDate(perfil: PerfilDTO): Span = Span(UIUtils.formatLocalDateTime(perfil.fechaModificacion))

        private fun createUserInfo(usuario: UsuarioDTO) =
                ListItem( Initials(if (usuario.interno) (if (usuario.administrador) "EA" else "E_") else (if (usuario.administrador) "CA" else "C_")),
                                      usuario.nombreUsuario,
                                      usuario.mail).apply {
                    setPadding(Vertical.XS)
                    setSpacing(Right.M)
                }

        private fun createPerfil(usuario: UsuarioDTO)= if (usuario.perfil == null) Span("*Sin perfil*") else Span(usuario.perfil!!.nombre)

        class DialogCaller(val usuarioService: UsuarioService,
                           val caller: Content
        ) : SimpleDialog.Caller {
            var nombrePerfil: String? = null
            var usuarioDTO : UsuarioDTO? = null

            override fun dialogResponseCancel(dialog: SimpleDialog?) { /* noop */ }

            override fun dialogResponseOk(dialog: SimpleDialog?) {
                val result = usuarioService.assignPerfil(AssignToUserDTO(nombrePerfil!!,  usuarioDTO!!)).block()

                UIUtils.showNotification("Se asignó el perfil:" + nombrePerfil + " al usuario:" + usuarioDTO!!.nombre)
                usuarioDTO!!.perfil = result!!.usuarioDTO.perfil  // update Grid
                caller.gridUsuarios.refresh()
            }
        }

     }

}

/**
 *  Clase que administra la asignación de Facultades extraordinararios a los Empleados de una Compañía.
 *
 *  Se realiza por medio de dos grids: el grid de Empleados y el grid de facultades.
 *  El grid de Facultad maneja paginación para efecto de performance ya que pueden ser muchas facultades.
 *
 *  Para ver y actualizar las facultades 'extras' de un empleado se llama desde esta ventana a la
 *  página de EmpleadosFacultadesDetalle
 *
 */
@PageTitle("Empleados Facultades")
@Route(value = "empleados-facultades", layout = MainLayout::class)
@RolesAllowed(Role.USER_IAM, Role.ADMIN_IAM, Role.ALL)  // Temporal debe ser ADMIN_IAM ya que usuarios externos no debe de asignar facultades individuales
class EmpleadosFacultades(private val securityService: CurrentSession,
                          private val usuarioService: UsuarioService,
                          private val companiaService: CompaniaService,
                          private val dataProvider: FacultadesGridDataProvider) : ViewFrame(), HasUrlParameter<Long> {

    private var title = " "
    private var appBarListener: Registration? = null


    override fun onAttach(attachEvent: AttachEvent?) {
        super.onAttach(attachEvent)
        initAppBar()
    }

    override fun onDetach(dettachEvent: DetachEvent?) {
        super.onDetach(dettachEvent)
        appBarListener!!.remove()
    }

    override fun setParameter(beforeEvent: BeforeEvent?, id: Long?) {
        val compania = companiaService.findById(id!!).block()

        if (compania != null) {
            title = compania.nombre
            UI.getCurrent().page.setTitle(compania.nombre)
            setViewContent(
                Content(securityService, compania.id, compania.nombre,
                                   usuarioService, dataProvider)
            )
        } else
            UIUtils.showNotification("La compañía no se encontró en la base de datos")
    }

    private fun initAppBar(): AppBar? {
        val appBar = MainLayout.get().appBar

        appBar.setNaviMode(AppBar.NaviMode.CONTEXTUAL)
        appBarListener = appBar.contextIcon.addClickListener { UI.getCurrent().navigate(Empleados::class.java) }
        appBar.title = "Facultades 'extraordinarias' para los empleado de la compañía:$title"

        return appBar
    }

    class Content(val securityService: CurrentSession,
                  val companiaId: Long?,
                  val nombreCompania: String,
                  val usuarioService: UsuarioService,
                  val dataProvider: FacultadesGridDataProvider): KComposite() {

        private var draggedItems: List<FacultadDTO> = ArrayList()
        private var selectedUsuario: UsuarioDTO? = null

        val root = ui {
            verticalLayout {
                isPadding = false; content { align(stretch, top) }

                label("EMPLEADOS:")
                grid(dataProvider = DataProvider.ofCollection(usuarioService.findByCompany(nombreCompania))) {
                    setId("usuarios")
                    dropMode = GridDropMode.ON_GRID
                    isRowsDraggable = true
                    setSelectionMode(Grid.SelectionMode.SINGLE)
                    addSelectionListener{ it.getFirstSelectedItem()
                            .ifPresent{selectedUsuario = it}}
                    addDropListener{
                        if (selectedUsuario != null) {
                            if (draggedItems.isEmpty())
                                UIUtils.showNotification("Se requiere seleccionar a un Usuario para asignarle una facultad")
                            else {
                                val nombreFacultad = draggedItems.first().nombre
                                val usuarioDTO = selectedUsuario

                                usuarioDTO!!.fechaModificacion = LocalDateTime.now()
                                usuarioDTO.usuarioModificacion = securityService.authenticatedUser.get().name

                                val result = usuarioService.assignFacultadExtra(AssignToUserDTO(nombreFacultad,  usuarioDTO)).block()

                                UIUtils.showNotification("Se asignó la facultad:$nombreFacultad como faculta 'extra' al usuario:${usuarioDTO.nombre}")
                                usuarioDTO.extraFacultades = result!!.usuarioDTO.extraFacultades  // update Grid
                                this.refresh()
                            }
                        } else
                            UIUtils.showNotification("Se requiere seleccionar a un Usuario para asignarle una facultad")
                    }
                    columnFor(UsuarioDTO::idUsuario)   {
                        isAutoWidth = true
                        flexGrow = 0
                        isFrozen = true
                        setHeader("Id Usuario")
                    }
                    columnFor(UsuarioDTO::nombre)   {
                        isAutoWidth = true
                        flexGrow = 0
                        isFrozen = true
                        setHeader("Log-in")
                    }
                    addColumn(ComponentRenderer{ usuario -> createUserInfo(usuario) }).apply {
                        setHeader("Nombre")
                        setSortProperty("nombreUsuario")
                    }
                    addColumn(ComponentRenderer { usuario -> createFacultadesExtraordinarias(usuario) }).apply {
                        isAutoWidth = true
                        flexGrow = 0
                        setHeader("Facultades Extraordinarias")
                        setTextAlign(ColumnTextAlign.END)
                    }
                    addComponentColumn { usuario -> createFacultadesExtraordinariasButton(usuario) }.apply {
                        isAutoWidth = true
                        flexGrow = 0
                        setHeader("Ver facultades")
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
                    columnFor(UsuarioDTO::telefono)   {
                        isAutoWidth = true
                        flexGrow = 0
                        isFrozen = true
                        setHeader("Teléfono")
                    }
                }
                div {  }
                label("FACULTADES:")
                searchBar {  // search field para manejar paginación y filtrado de las facultades
                    setCheckboxText("Activas")
                    setPlaceHolder("Búsqueda")
                    addFilterChangeListener{
                        if (this@searchBar.filter.isNotEmpty() || this@searchBar.isCheckboxChecked)
                            dataProvider.setFilter( FacultadFilter(this@searchBar.filter, this@searchBar.isCheckboxChecked))
                        else
                            dataProvider.setFilter(FacultadFilter.getEmptyFilter())
                    }
                    actionButton.isVisible = false
                }
                grid(dataProvider = dataProvider) {
                    addThemeName("mobile")
                    setId("facultades")
                    isRowsDraggable = true
                    addDragStartListener { draggedItems = it.draggedItems }
                    setSelectionMode(Grid.SelectionMode.NONE)
                    addDragEndListener {
                        if (draggedItems.isNotEmpty())  draggedItems = ArrayList()
                    }
                    addColumn(ComponentRenderer { facultad -> createUserInfo(facultad) }).apply {
                        setHeader("Facultad")
                        setSortProperty("nombre")
                    }
                    addColumn(ComponentRenderer { facultad -> createActive(facultad.activo) }).apply {
                        isAutoWidth = true
                        flexGrow = 0
                        setHeader("Activa")
                        setTextAlign(ColumnTextAlign.END)
                    }
                    addColumn(ComponentRenderer { facultad -> createDate(facultad) }).apply {
                        isAutoWidth = true
                        flexGrow = 0
                        setHeader("Fecha modificación")
                        setTextAlign(ColumnTextAlign.END)
                    }
                }
            }
        }

        private fun createDateIngreso(usuario: UsuarioDTO): Span = Span(UIUtils.formatLocalDate(usuario.fechaIngreso))

        private fun createUserInfo(facultad: FacultadDTO) =
                ListItem(Initials(facultad.tipo.initials),
                                  facultad.nombre,
                                  UIUtils.shortDescription(facultad.descripcion, 15)).apply{
                            setPadding(Vertical.XS)
                            setSpacing(Right.M)
                }

        private fun createActive(isActivo: Boolean): Icon =
                if (isActivo)
                    UIUtils.createPrimaryIcon(VaadinIcon.CHECK)
                else
                    UIUtils.createDisabledIcon(VaadinIcon.CLOSE)

        private fun createDate(facultad: FacultadDTO): Span = Span(UIUtils.formatLocalDateTime(facultad.fechaModificacion))

        private fun createUserInfo(usuario: UsuarioDTO) =
                ListItem( Initials(if (usuario.interno) (if (usuario.administrador) "EA" else "E_") else (if (usuario.administrador) "CA" else "C_")),
                        usuario.nombreUsuario,
                        usuario.mail).apply {
                    setPadding(Vertical.XS)
                    setSpacing(Right.M)
                }

        private fun createFacultadesExtraordinarias(usuario: UsuarioDTO) =
                if (usuario.extraFacultades.isEmpty())
                    Span("*Sin facultades extras*")
                else if (usuario.extraFacultades.size == 1)
                    Span(usuario.extraFacultades.iterator().next().nombre)
                else
                    Span("(" + usuario.extraFacultades.size + ") " + usuario.extraFacultades.iterator().next().nombre + "...")

        private fun createFacultadesExtraordinariasButton(usuario: UsuarioDTO) =
                UIUtils.createSmallButton("Detalle", VaadinIcon.EYE).apply {
                    isVisible = usuario.extraFacultades.isNotEmpty()
                    onLeftClick {
                        UI.getCurrent().navigate(
                            EmpleadosFacultadesDetalle::class.java,
                                                companiaId!!.toString() + ":" +  usuario.id!!.toString())
                    }
                }
    }

}

/**
 *  Clase que edita las Facultades extraordinarias a los Empleados de una Compañía.
 *
 *  - Se pueden consultar y borrar las facultades extras asignadas al empleado.
 *  - Se tiene un solo grid sobre la relación dentro del DTO de usuarios de sus facultades
 *    extras.
 *
 *  Utiliza un grid de las facultades 'ya' asignadas al empleado.
 *
 */
@PageTitle("Empleados Facultades Detalle")
@Route(value = "empleados-facultades-detalle", layout = MainLayout::class)
@RolesAllowed(Role.USER_IAM, Role.ADMIN_IAM, Role.ALL)  // Temporal debe ser ADMIN_IAM ya que usuarios externos no debe de asignar facultades individuales
class EmpleadosFacultadesDetalle(private val securityService: CurrentSession,
                                 private val service: UsuarioService
) : ViewFrame(), HasUrlParameter<String> {

    private var title = " "
    private var companiaId:Long = 0L
    private var appBarListener: Registration? = null

    override fun onAttach(attachEvent: AttachEvent?) {
        super.onAttach(attachEvent)
        initAppBar()
    }

    override fun onDetach(dettachEvent: DetachEvent?) {
        super.onDetach(dettachEvent)
        appBarListener!!.remove()
    }

    override fun setParameter(beforeEvent: BeforeEvent?, navHistory: String?) {
        val parms = navHistory!!.split(":")

        companiaId = parms.get(0).toLong()

        val usuario = service.findById(parms.get(1).toLong()).block()

        if (usuario != null) {
            title = usuario.nombre
            UI.getCurrent().page.setTitle(usuario.nombre)
            setViewContent(Content(securityService, usuario, service))
        } else
            UIUtils.showNotification("El usuario no se encontró en la base de datos")
    }

    private fun initAppBar(): AppBar? {
        val appBar = MainLayout.get().appBar

        appBar.setNaviMode(AppBar.NaviMode.CONTEXTUAL)
        appBarListener = appBar.contextIcon.addClickListener { UI.getCurrent().navigate(EmpleadosFacultades::class.java, companiaId) }
        appBar.title = "Edición de las facultades 'extraordinarias' del empleado:$title"

        return appBar
    }

    class Content(val securityService: CurrentSession,
                  val usuario: UsuarioDTO,
                  val usuarioService: UsuarioService
    ): KComposite() {


        val root = ui {
            verticalLayout {
                isPadding = false; content { align(stretch, top) }
                label("FACULTADES EXTRAS:")
                grid(dataProvider = DataProvider.ofCollection(usuario.extraFacultades)) {
                    addThemeName("mobile")
                    setId("facultades-extras")
                    setSelectionMode(Grid.SelectionMode.SINGLE)
                    columnFor(FacultadDTO::id) {
                        isAutoWidth = true
                        flexGrow = 0
                        isFrozen = true
                        setHeader("ID")
                    }
                    addColumn(ComponentRenderer{ facultad -> createUserInfo(facultad) }).apply {
                        setHeader("Facultad Extra")
                        setSortProperty("nombre")
                    }
                    addColumn(ComponentRenderer { facultad -> createActive(facultad.activo) }).apply {
                        isAutoWidth = true
                        flexGrow = 0
                        setHeader("Activa")
                        setTextAlign(ColumnTextAlign.END)
                    }
                    addColumn(ComponentRenderer{ facultad -> createDate(facultad) }).apply {
                        isAutoWidth = true
                        flexGrow = 0
                        setHeader("Fecha modificación")
                        setTextAlign(ColumnTextAlign.END)
                    }
                    addComponentColumn { facultad -> createUnAssign(this@grid, facultad) }.apply {
                        isAutoWidth = true
                        flexGrow = 0
                    }
                }
            }
        }

        private fun createUserInfo(facultad: FacultadDTO) =
                ListItem(Initials(facultad.tipo.initials),
                        facultad.nombre,
                        UIUtils.shortDescription(facultad.descripcion, 15)).apply{
                    setPadding(Vertical.XS)
                    setSpacing(Right.M)
                }

        private fun createActive(isActivo: Boolean): Icon =
                if (isActivo)
                    UIUtils.createPrimaryIcon(VaadinIcon.CHECK)
                else
                    UIUtils.createDisabledIcon(VaadinIcon.CLOSE)

        private fun createDate(facultad: FacultadDTO): Span = Span(UIUtils.formatLocalDateTime(facultad.fechaModificacion))

        private fun createUnAssign(grid: Grid<FacultadDTO>, facultad: FacultadDTO) =
                UIUtils.createButton(VaadinIcon.TRASH, ButtonVariant.LUMO_SMALL,
                                                       ButtonVariant.LUMO_TERTIARY).apply {
                    addClickListener {
                        usuario.fechaModificacion = LocalDateTime.now()
                        usuario.usuarioModificacion = securityService.authenticatedUser.get().name

                        val assignUserDTO = usuarioService.unAssignFacultadExtra(AssignToUserDTO(facultad.nombre, usuario)).block()

                        usuario.extraFacultades = assignUserDTO!!.usuarioDTO.extraFacultades
                        UIUtils.showNotification("Se eliminó la facultad:${facultad.nombre} como 'extra' del usuario:${usuario.nombre}" )
                        grid.setItems(DataProvider.ofCollection(usuario.extraFacultades))
                        // ^ refresh the grid
                    }
                }
    }

}

/**
 *  Clase que administra la prohibición de Facultades 'individuales' a los Empleados de una Compañía.
 *
 *  La página tiene tres grids:
 *
 *  - Grid de los empleados de la compañía.
 *  - Grid de las facultades que tiene el empleado (incluyendo facultades extras y facultades prohibidas).
 *  - Grid de las facultades prohibidas del usuario. relación dentro del usuarioDTO seleccionado.
 *
 *  Dentro del segundo y tercer grid se realizan operaciones de DnD de facultadDTO.
 *
 *  nota: Para consultar correctamente el usuario, este debe de tener un perfil y facultad minima. No
 *        funciona correctamente si el usuario solo tiene facultades extras.
 *
 */
@PageTitle("Empleados Prohibicion Facultades")
@Route(value = "empleados-prohibe-facultades", layout = MainLayout::class)
@RolesAllowed(Role.USER_IAM, Role.ADMIN_IAM, Role.ALL) // Temporal debe ser ADMIN_IAM ya que usuarios externos no debe de asignar facultades individuales
class EmpleadosForbidFacultades(private val securityService: CurrentSession,
                                private val service: UsuarioService,
                                private val facultadService: FacultadService,
                                private val companiaService: CompaniaService
) : ViewFrame(), HasUrlParameter<Long> {

    private var title = " "
    private var appBarListener: Registration? = null

    override fun onAttach(attachEvent: AttachEvent?) {
        super.onAttach(attachEvent)
        initAppBar()
    }

    override fun onDetach(dettachEvent: DetachEvent?) {
        super.onDetach(dettachEvent)
        appBarListener!!.remove()
    }

    override fun setParameter(beforeEvent: BeforeEvent?, id: Long?) {
        val compania = companiaService.findById(id!!).block()

        if (compania != null) {
            title = compania.nombre
            UI.getCurrent().page.setTitle(compania.nombre)
            setViewContent(Content(securityService, compania.nombre, service))
        } else
            UIUtils.showNotification("La compañía no se encontró en la base de datos")
    }

    private fun initAppBar(): AppBar? {
        val appBar = MainLayout.get().appBar

        appBar.setNaviMode(AppBar.NaviMode.CONTEXTUAL)
        appBarListener = appBar.contextIcon.addClickListener { UI.getCurrent().navigate(Empleados::class.java) }
        appBar.title = "Prohibición de facultades 'individuales' para los empleado de la compañía:$title"

        return appBar
    }

    class Content(val securityService: CurrentSession,
                  val nombreCompania: String,
                  val service: UsuarioService
    ): KComposite() {

        private var draggedItems: List<FacultadDTO>? = null
        private var dragSource:Grid<FacultadDTO>? = null
        private var selectedUsuario: UsuarioDTO? = null
        private lateinit var gridFacultades:Grid<FacultadDTO>
        private lateinit var gridForbidFacultades:Grid<FacultadDTO>

        /*
         * Para mayor información ver el ejemplo de Vaadin DnD de grids:
         * @see: https://vaadin.com/components/vaadin-grid/java-examples/drag-and-drop
         */
        private val dragStartListener =  ComponentEventListener<GridDragStartEvent<FacultadDTO>>() {
            draggedItems = it.draggedItems
            dragSource = it.source
            gridFacultades.dropMode = GridDropMode.BETWEEN
            gridForbidFacultades.dropMode = GridDropMode.BETWEEN
        }

        /*
         * Para maor información ver el ejemplo de Vaadin DnD de grids:
         * @see: https://vaadin.com/components/vaadin-grid/java-examples/drag-and-drop
         */
        private val dragEndListener =  ComponentEventListener<GridDragEndEvent<FacultadDTO>>() {
            draggedItems = null
            dragSource = null
            gridFacultades.dropMode = null
            gridForbidFacultades.dropMode = null
        }

        /*
         * Para mayor información ver el ejemplo de Vaadin DnD de grids:
         * @see: https://vaadin.com/components/vaadin-grid/java-examples/drag-and-drop
         */
        private val dropListener =  ComponentEventListener<GridDropEvent<FacultadDTO>>() {
            val event = it
            val target = it.dropTargetItem

            if (target.isPresent && draggedItems!!.contains(target.get()))
                return@ComponentEventListener

            // Remove items from the source Grid
            val sourceDataProvider = dragSource!!.dataProvider as ListDataProvider<FacultadDTO>
            val sourceItems = ArrayList(sourceDataProvider.items)

            sourceItems.removeAll(draggedItems!!)
            dragSource!!.setItems(sourceItems)
            draggedItems!!.forEach{
                if (dragSource == gridFacultades) { // it is new forbidden permit
                    selectedUsuario!!.fechaModificacion = LocalDateTime.now()
                    selectedUsuario!!.usuarioModificacion = securityService.authenticatedUser.get().name

                    val result = service.forbidFacultad(AssignToUserDTO(it.nombre, selectedUsuario!!)).block()

                    UIUtils.showNotification("Se le bloqueo la facultad:${it.nombre} al usuario:${selectedUsuario!!.nombreUsuario}")
                    selectedUsuario!!.sinFacultades = result!!.usuarioDTO.sinFacultades // update grid
                } else { // unforbid the permit
                    selectedUsuario!!.fechaModificacion = LocalDateTime.now()
                    selectedUsuario!!.usuarioModificacion = securityService.authenticatedUser.get().name

                    val result = service.unForbidFacultad(AssignToUserDTO(it.nombre, selectedUsuario!!)).block()

                    UIUtils.showNotification("Se le desbloqueo la facultad:${it.nombre} al usuario:${selectedUsuario!!.nombreUsuario}")
                    selectedUsuario!!.sinFacultades = result!!.usuarioDTO.sinFacultades // update grid
                }
            }

            // Add dragged items to the target Grid
            val targetGrid = it.source
            val targetDataProvider = targetGrid.dataProvider as ListDataProvider<FacultadDTO>
            val targetItems = ArrayList(targetDataProvider.items)
            val index = target.map{
                                    targetItems.indexOf(it) + (if (event.dropLocation == GridDropLocation.BELOW) 1 else 0)
                                  }
                            .orElse(0)
            targetItems.addAll(index, draggedItems!!)
            targetGrid.setItems(targetItems)
        }

        val root = ui {
            verticalLayout {
                isPadding = false; content { align(stretch, top) }

                label("EMPLEADOS:")
                grid(dataProvider = DataProvider.ofCollection(service.findByCompany(nombreCompania))) {
                    setId("usuarios")
                    setSelectionMode(Grid.SelectionMode.SINGLE)
                    addSelectionListener{ it.getFirstSelectedItem()
                                            .ifPresent{fillGrids(it) }
                                        }
                    columnFor(UsuarioDTO::id)   {
                        isAutoWidth = true
                        flexGrow = 0
                        isFrozen = true
                        setHeader("ID")
                    }
                    columnFor(UsuarioDTO::idUsuario)   {
                        isAutoWidth = true
                        flexGrow = 0
                        isFrozen = true
                        setHeader("Id Usuario")
                    }
                    columnFor(UsuarioDTO::nombre)   {
                        isAutoWidth = true
                        flexGrow = 0
                        isFrozen = true
                        setHeader("Log-in")
                    }
                    addColumn(ComponentRenderer { usuario -> createUserInfo(usuario) }).apply {
                        setHeader("Nombre")
                        setSortProperty("nombreUsuario")
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
                    columnFor(UsuarioDTO::telefono)   {
                        isAutoWidth = true
                        flexGrow = 0
                        isFrozen = true
                        setHeader("Teléfono")
                    }
                }
                div {  }
                label("FACULTADES:")
                horizontalLayout {
                    gridFacultades = grid() {
                        addThemeName("mobile")
                        setId("facultades")
                        isRowsDraggable = true
                        addDropListener(dropListener)
                        addDragStartListener(dragStartListener)
                        addDragEndListener(dragEndListener)
                        setSelectionMode(Grid.SelectionMode.SINGLE)
                        columnFor(FacultadDTO::id) {
                            isAutoWidth = true
                            flexGrow = 0
                            isFrozen = true
                            setHeader("ID")
                        }
                        addColumn(ComponentRenderer{ facultad -> createUserInfo(facultad) }).apply {
                            setHeader("Facultad permitida")
                            setSortProperty("nombre")
                        }
                        addColumn(ComponentRenderer{ facultad -> createActive(facultad.activo) }).apply {
                            isAutoWidth = true
                            flexGrow = 0
                            setHeader("Activa")
                            setTextAlign(ColumnTextAlign.END)
                        }
                    }
                    gridForbidFacultades = grid() {
                        addThemeName("mobile")
                        setId("facultades-forbid")
                        isRowsDraggable = true
                        addDropListener(dropListener)
                        addDragStartListener(dragStartListener)
                        addDragEndListener(dragEndListener)
                        setSelectionMode(Grid.SelectionMode.SINGLE)
                        columnFor(FacultadDTO::id) {
                            isAutoWidth = true
                            flexGrow = 0
                            isFrozen = true
                            setHeader("ID")
                        }
                        addColumn(ComponentRenderer { facultad -> createUserInfo(facultad) }).apply {
                            setHeader("Facultad no permitida")
                        }
                        addColumn(ComponentRenderer { facultad -> createActive(facultad.activo) }).apply {
                            isAutoWidth = true
                            flexGrow = 0
                            setHeader("Activa")
                            setTextAlign(ColumnTextAlign.END)
                        }
                    }
                }
            }
        }

        private fun createUserInfo(facultad: FacultadDTO) =
                ListItem(Initials(facultad.tipo.initials),
                        facultad.nombre,
                        UIUtils.shortDescription(facultad.descripcion, 15)).apply{
                    setPadding(Vertical.XS)
                    setSpacing(Right.M)
                }

        private fun createActive(isActivo: Boolean): Icon =
                if (isActivo)
                    UIUtils.createPrimaryIcon(VaadinIcon.CHECK)
                else
                    UIUtils.createDisabledIcon(VaadinIcon.CLOSE)

        private fun createDateIngreso(usuario: UsuarioDTO): Span = Span(UIUtils.formatLocalDate(usuario.fechaIngreso))

        private fun createUserInfo(usuario: UsuarioDTO) =
                ListItem( Initials(if (usuario.interno) (if (usuario.administrador) "EA" else "E_") else (if (usuario.administrador) "CA" else "C_")),
                        usuario.nombreUsuario,
                        usuario.mail).apply {
                    setPadding(Vertical.XS)
                    setSpacing(Right.M)
                }

        private fun fillGrids(usuario: UsuarioDTO) {
            selectedUsuario = usuario
            gridFacultades.setItems(DataProvider.ofCollection(service.findFacultadesDetail(usuario.nombreUsuario)))
            gridForbidFacultades.setItems(DataProvider.ofCollection(usuario.sinFacultades))
        }
    }
}
