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
 *  operation.kt
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.ailegorreta.iamui.ui.views.operation

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.mvysny.karibudsl.v10.*
import com.ailegorreta.iamui.backend.data.Role
import com.ailegorreta.iamui.backend.data.dto.*
import com.ailegorreta.iamui.backend.data.dto.compania.*
import com.ailegorreta.iamui.backend.service.*
import com.ailegorreta.client.components.ui.*
import com.ailegorreta.client.components.ui.layout.size.*
import com.ailegorreta.client.components.utils.SimpleDialog
import com.ailegorreta.client.components.utils.UIUtils
import com.ailegorreta.client.components.utils.css.*
import com.ailegorreta.client.navigation.detailsdrawer.DetailsDrawer
import com.ailegorreta.client.navigation.detailsdrawer.DetailsDrawerFooter
import com.ailegorreta.client.navigation.detailsdrawer.DetailsDrawerHeader
import com.ailegorreta.client.security.service.CurrentSession
import com.ailegorreta.commons.utils.FormattingUtils
import com.ailegorreta.iamui.backend.data.dto.DnDDTO
import com.ailegorreta.iamui.backend.data.dto.GraphDTO
import com.ailegorreta.iamui.backend.data.dto.Link
import com.ailegorreta.iamui.backend.data.dto.Node
import com.ailegorreta.iamui.backend.data.dto.compania.CompaniaDTO
import com.ailegorreta.iamui.backend.data.dto.compania.GrupoDTO
import com.ailegorreta.iamui.backend.data.dto.compania.UsuarioDTO
import com.ailegorreta.iamui.backend.data.service.CompaniaService
import com.ailegorreta.iamui.backend.data.service.GrupoService
import com.ailegorreta.iamui.backend.data.service.UsuarioCompaniaService
import com.ailegorreta.iamui.ui.MainLayout
import com.ailegorreta.iamui.ui.components.*
import com.ailegorreta.iamui.ui.views.SplitViewFrame
import com.vaadin.flow.component.AttachEvent
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.formlayout.FormLayout
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.icon.Icon
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.radiobutton.RadioGroupVariant
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.binder.BeanValidationBinder
import com.vaadin.flow.data.converter.StringToLongConverter
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import jakarta.annotation.security.RolesAllowed

/**
 *  Principal page for Administrator operation:
 *  - Add new (delete) administrator´´ group to a Company
 *  - Add new Administrator and assign them to a group.
 *  - Add new Employees for a specific Company.
 *
 *  The main age display a graph for al Companies, Groups and Employees
 *  that the login user can have access. Id the Administrator is a
 *  master Administrator, he(she) can see other Administrators graphs.
 *  And shows the appropriate actions when the Administrator clicks
 *  on one graph node.
 *
 *  Kotlin classes in this file
 *  - Operation: main page that display the graph.
 *  - GraphAdministrador: small data class to un-parse the graph that came from the
 *    IAM-repo-server
 *  - GrupoDetail: view to handle Groups.
 *  - AdministradorDetail: view to handle Administrators.
 *  - UsuarioDetail: View to handle the Employees for the selected Company.
 *
 * @author rlh
 * @project : iam-ui
 * @date September 2023
 */
@Component
@Scope("prototype")
@Route(value = "operation", layout = MainLayout::class)
@PageTitle("Operation")
@RolesAllowed(Role.USER_IAM, Role.ADMIN_IAM, Role.ALL)
class  Operation(private val securityService: CurrentSession,
                 private val usuarioCompaniaService: UsuarioCompaniaService,
                 private val companiaService: CompaniaService,
                 private val grupoService: GrupoService,
                 private val graph: Graph,
                 private val mapper: ObjectMapper) : SplitViewFrame(), SimpleDialog.Caller {

    var graphFilter = ""

    private final var detailsDrawer: DetailsDrawer
    private var detailsDrawerHeader: DetailsDrawerHeader? = null
    private var saveButton: Button? = null
    private var currentDetailController: DetailController? = null
    var graphAdministrador: GraphAdministrador? = null

    init {
        setViewContent(Content(securityService, this, graph), graph)
        detailsDrawer = createDetailsDrawer()
        setViewDetails(detailsDrawer)
        setViewDetailsPosition(Position.BOTTOM)
    }

    override fun onAttach(attachEvent: AttachEvent?) {
        graph.setVisibleDnDropZone(false)
        graph.setWidth(800)
        graph.setHeight(800)
        updateGraphData()
    }

    /**
     * This method reads the graph again form the iam server repo, because refresh
     * operation or the filter for the supervisor changed.
     */
    private fun updateGraphData() {
        val data =  if (securityService.hasRole(Role.MASTER_ADMIN))
                        usuarioCompaniaService.findGrafoAdministradorMaestro(securityService.authenticatedUser.get().name)
                    else
                        usuarioCompaniaService.findGrafoAdministrador(securityService.authenticatedUser.get().name)
        // ^note: Master administrator has access to ALL corporate Groups.
        //        The 'no master' Administrator as access only to his(her) group

        try {
            graphAdministrador = mapper.readValue(data.block(), GraphAdministrador::class.java)
            graph.startGraph(AdministradorGrafo(graphAdministrador!!.filteredData(mapper, graphFilter)))
        } catch (e: IOException) {
            UIUtils.showNotification("Error NO se pudo leer los datos de la gráfica")
        }
    }

    /**
     * The filter for the graph has been changed. We update the display of the Graph
     */
    fun updateGraphFilter() {

        graph.startGraph(AdministradorGrafo(graphAdministrador!!.filteredData(mapper, graphFilter)))
        graph.updateGraph()
    }

    private fun createDetailsDrawer(): DetailsDrawer {
        val detailsDrawer = DetailsDrawer(DetailsDrawer.Position.BOTTOM)
        // Header
        detailsDrawerHeader = DetailsDrawerHeader("")

        detailsDrawerHeader!!.addCloseListener { detailsDrawer.hide() }
        detailsDrawer.setHeader(detailsDrawerHeader!!)
        // Footer
        val footer = DetailsDrawerFooter()

        footer.addSaveListener {
            if (currentDetailController!!.save()) {
                detailsDrawer.hide()
                updateGraphData()
                UIUtils.showNotification("Cambios realizados.")
            }
        }
        saveButton = footer.saveButton
        footer.addCancelListener {
            if (saveButton!!.isEnabled)
                (SimpleDialog("Se tienen campos pendientes de guardar.", this)).open()
            else
                detailsDrawer.hide()
        }
        detailsDrawer.setFooter(footer)

        return detailsDrawer
    }

    fun grupoDetail( item: Any?) {
        val grupoDTO = if (item == null)
                         GrupoDTO(null, "Nuevo grupo", true, securityService.authenticatedUser.get().name)
                       else
                         grupoService.findById( (item as Node).idNeo4j).block()!!

        detailsDrawerHeader!!.setTitle(grupoDTO.nombre)
        currentDetailController = GrupoDetail(securityService = securityService,
                                              item = grupoDTO,
                                              service =  grupoService,
                                              companiaService = companiaService,
                                              usuarioCompaniaService = usuarioCompaniaService,
                                              detailsDrawerHeader = detailsDrawerHeader!!,
                                              saveButton = saveButton!!,
                                              validCompanies = graphAdministrador!!.validCompanies)
        detailsDrawer.setContent(currentDetailController!! as KComposite)
        detailsDrawer.show()
    }

    fun usuarioDetail( item: Any?) {
        val usuarioDTO = if (item is String)
                            UsuarioDTO( telefono = "00-0000-0000",
                                        mail = "staff@lmass.com.mx",
                                        interno = graphAdministrador!!.validCompanies.contains(securityService.authenticatedUser.get().company),
                                        // ^ the use is an employee if a in properties iam.customer belongs
                                        administrador = false,
                                        zonaHoraria = TimeZone.getDefault().displayName,
                                        usuarioModificacion = securityService.authenticatedUser.get().name)
                         else
                            usuarioCompaniaService.findUsuarioByIdWithSupervisor((item as Node).idNeo4j, true).block()!!

        detailsDrawerHeader!!.setTitle(usuarioDTO.nombreUsuario)
        currentDetailController = UsuarioDetail(item = usuarioDTO,
                                                service =  usuarioCompaniaService,
                                                companiaService = companiaService,
                                                detailsDrawerHeader = detailsDrawerHeader!!,
                                                saveButton = saveButton!!,
                                                validCompanies = graphAdministrador!!.validCompanies,
                                                startCompany = if (item is String) item else null)
        detailsDrawer.setContent(currentDetailController!! as KComposite)
        detailsDrawer.show()
    }

    fun administradorDetail( item: Any?) {
        val usuarioDTO = if (item is String)
                            UsuarioDTO( nombreUsuario = "",
                                        nombre = "",
                                        telefono = "00-0000-0000",
                                        mail = "staff@lmass.com.mx",
                                        interno = graphAdministrador!!.validCompanies.contains(securityService.authenticatedUser.get().company),
                                        // ^ the use is an employee if a in properties iam.customer belongs
                                        administrador = true,
                                        zonaHoraria = TimeZone.getDefault().displayName,
                                        usuarioModificacion = securityService.authenticatedUser.get().name)
                         else
                            usuarioCompaniaService.findUsuarioByIdWithSupervisor((item as Node).idNeo4j).block()!!
        detailsDrawerHeader!!.setTitle(usuarioDTO.nombreUsuario)
        currentDetailController = AdministradorDetail(item = usuarioDTO,
                                                      service =  usuarioCompaniaService,
                                                      companiaService = companiaService,
                                                      grupoService = grupoService,
                                                      detailsDrawerHeader = detailsDrawerHeader!!,
                                                      saveButton = saveButton!!,
                                                      validGroups = graphAdministrador!!.validGroups,
                                                      startGroup = if (item is String) item else null,
                                                      validCompanies = graphAdministrador!!.validCompanies,
                                                      startCompany = null)
        detailsDrawer.setContent(currentDetailController!! as KComposite)
        detailsDrawer.show()
    }

    override fun dialogResponseCancel(dialog: SimpleDialog?) { /* noop */ }

    override fun dialogResponseOk(dialog: SimpleDialog?) {
        detailsDrawer.hide()
        updateGraphData()
    }

    /**
     * Content Component. It is the graph and the buttons array
     */
    class Content(val securityService: CurrentSession,
                  val caller: Operation,
                  val graph: Graph): KComposite() {
        private var node: Node? = null
        private lateinit var nuevoGrupo: Button
        private lateinit var editarGrupo: Button
        private lateinit var nuevoAdministrador: Button
        private lateinit var editarAdministrador: Button
        private lateinit var nuevoUsuario: Button
        private lateinit var editarUsuario: Button

        val root = ui {
            flexBoxLayout {
                setBoxSizing(BoxSizing.BORDER_BOX)
                setPadding(Horizontal.RESPONSIVE_X, Top.RESPONSIVE_X)
                verticalLayout {
                    horizontalLayout {
                        nuevoGrupo = button("Nuevo grupo") {
                            icon = Icon(VaadinIcon.GROUP)
                            isVisible = false
                            onLeftClick { caller.grupoDetail(null) }
                        }
                        editarGrupo = button("Editar grupo") {
                            icon = Icon(VaadinIcon.EDIT)
                            isVisible = false
                            onLeftClick { caller.grupoDetail(node) }
                        }
                    }
                    horizontalLayout {
                        nuevoAdministrador = button("Nuevo Administrador") {
                            icon = Icon(VaadinIcon.SPECIALIST)
                            isVisible = false
                            onLeftClick { caller.administradorDetail(this.text.substring(this.text.indexOf(":") + 1)) }
                        }
                        editarAdministrador = button("Editar Administrador") {
                            icon = Icon(VaadinIcon.EDIT)
                            isVisible = false
                            onLeftClick { caller.administradorDetail(node) }
                        }
                    }
                    horizontalLayout {
                        nuevoUsuario = button("Nuevo Usuario") {
                            icon = Icon(VaadinIcon.USER)
                            isVisible = false
                            onLeftClick { caller.usuarioDetail(this.text.substring(this.text.indexOf(":") + 1)) }
                        }
                        editarUsuario = button(" Editar Usuario") {
                            icon = Icon(VaadinIcon.USER_CARD)
                            isVisible = false
                            onLeftClick { caller.usuarioDetail(node) }
                        }
                    }
                    label{
                        text = "Dar 'doble-click' en el nodo de Compañía, Grupo, Administrador o Usuario que se desea  trabajar"
                    }
                    horizontalLayout {
                        add(graph.createRefresh(""))
                        searchBar {
                            setPlaceHolder("Filtro por nombre")
                            setCheckboxText("")
                            setActionIcon("vaadin:ellipsis-dots-h")
                            addFilterChangeListener {
                                if (caller.graphFilter != this@searchBar.filter) {
                                    caller.graphFilter = this@searchBar.filter
                                    caller.updateGraphFilter()
                                }
                            }
                        }
                    }
                }
            }
        }

        init {
            graph.addSelectListener {
                    val item = it.getItem()

                    if (item is Int) {
                        @SuppressWarnings("unchecked")
                        val itemSelected = item

                        node = setButton(itemSelected)
                    }
            }
        }

        private fun setButton(itemSelected : Int): Node {
            val node = caller.graphAdministrador!!.getNode(itemSelected)

            if (node.type.equals("compania")) {
                nuevoGrupo.isVisible = securityService.hasRole(Role.MASTER_ADMIN)
                editarGrupo.isVisible = false
                nuevoAdministrador.isVisible = false
                editarAdministrador.isVisible = false
                nuevoUsuario.text = "Nuevo usuario para la compañía:" + node.caption
                nuevoUsuario.isVisible = true
                editarUsuario.isVisible = false
            } else if (node.type.equals("grupo")) {
                nuevoGrupo.isVisible = securityService.hasRole(Role.MASTER_ADMIN)
                editarGrupo.text = "Editar el grupo:" + node.caption
                editarGrupo.isVisible = securityService.hasRole(Role.MASTER_ADMIN)
                nuevoAdministrador.text = "Nuevo administrador para el grupo:" + node.idNeo4j + ":" + node.caption
                nuevoAdministrador.isVisible = securityService.hasRole(Role.MASTER_ADMIN)
                editarAdministrador.isVisible = false
                nuevoUsuario.isVisible = false
                editarUsuario.isVisible = false
            } else if (node.type.equals("usuario")) {
                nuevoGrupo.isVisible = false
                editarGrupo.isVisible = false
                nuevoAdministrador.isVisible = false
                if (node.subTypeVal == 1) { // it is an administrator
                    editarAdministrador.text = "Editar el administrador:" + node.caption
                    editarAdministrador.isVisible = securityService.hasRole(Role.MASTER_ADMIN)
                } else
                    editarAdministrador.isVisible = false
                nuevoUsuario.isVisible = false
                editarGrupo.text = "Editar al usuario:" + node.caption
                editarUsuario.text = "Editar al usuario:" + node.caption
                editarUsuario.isVisible = true
            } else {  // avoid any unknown node type
                nuevoGrupo.isVisible = false
                editarGrupo.isVisible = false
                nuevoAdministrador.isVisible = false
                editarAdministrador.isVisible = false
                nuevoUsuario.isVisible = false
                editarUsuario.isVisible = false
            }

            return node
        }
    }

    private class AdministradorGrafo(private val data: String? ) : GraphDTO {

        override fun jsonString(mapper: ObjectMapper): String {
            return data?: ""
        }

        override fun addNode(mapper: ObjectMapper, node: DnDDTO): String {
            return ""
        }

        override fun deleteNode(node: DnDDTO) {}

        override val dndDropTitle: String
            get() = ""
    }

    /**
     * DTO to read the nodes and edges graph coming from the server.
     *
     */
    data class GraphAdministrador(val nodes: List<Node>,
                                  val edges: List<Link>) {

        val validCompanies: List<String>
        val validGroups: List<String>
        var filteredNodes: List<Node> = ArrayList()

        data class Filtered(val nodes: List<Node>,
                            val edges: List<Link>)

        init {
            validCompanies = nodes.asSequence()
                                  .filter { it.type.equals("compania") }
                                  .map{ it.caption }
                                  .toList()
            validGroups = nodes.asSequence()
                               .filter { it.type.equals("grupo") }
                               .map{ it.idNeo4j.toString() + ":" + it.caption }
                               .toList()
        }

        fun getNode(id: Int) = nodes.asSequence().filter{it.id == id}.first()

        /**
         * Do graph filtering by nombre Usuario
         */
        fun filteredData(mapper: ObjectMapper, filter: String): String {
            if (filter.isEmpty())       // no filter in needed
                return mapper.writeValueAsString(Filtered(nodes, edges))

            // 1st filter the nodes
            filteredNodes = nodes.asSequence()
                                 .filter { (it.type != "usuario") ||
                                           (it.type == "usuario" && it.caption.contains(filter)) }
                                 .toList()
            // 2nd once the nodes are filter we erase unnecessary edges
            val filteredEdges = edges.asSequence()
                                     .filter { validLink(it) }
                                     .toList()

            return mapper.writeValueAsString(Filtered(filteredNodes, filteredEdges))
        }

        /*
         * Check if the link has a reference to a filtered node.
         */
        private fun validLink(edge: Link): Boolean {
            var found = false

            for (node in filteredNodes)
                if (node.id == edge.source) {
                    found = true
                    break
                }
            if (!found) return false
            for (node in filteredNodes)
                if (node.id == edge.target)
                    return true

            return false
        }

        override fun toString() = "nodes:" + nodes.size + " edges:" + edges.size
    }

    /**
     * Interface that all Detail controllers (Grupos, Usuarios and Administrador)
     * must implement.
     *
     * This is done because the save() method is executed fom the parent (i.e., Content)
     */
    interface DetailController {
        fun save(): Boolean

        fun validateBean(): List<String>
    }

    /**
     * Detail class for new and update Groups.
     */
    class GrupoDetail(val securityService: CurrentSession,
                      val item: GrupoDTO,
                      val service: GrupoService,
                      val companiaService: CompaniaService,
                      val usuarioCompaniaService: UsuarioCompaniaService,
                      detailsDrawerHeader : DetailsDrawerHeader,
                      val saveButton: Button,
                      val validCompanies : List<String>): KComposite(), DetailController {

        private val binder: BeanValidationBinder<GrupoDTO>
        private lateinit var fechaModificacion: Span
        private lateinit var usuarioModificacion: Span
        private var isNewGroup = true
        private var nombreAdministrador = ""
        private lateinit var newGroupView: Div
        private lateinit var updateGroupView:Div
        private lateinit var nombreLargoAdminitrador: Span


        init {
            binder = BeanValidationBinder(GrupoDTO::class.java)
            binder.addStatusChangeListener { saveButton.isEnabled = true }
        }

        val root = ui {
            formLayout {
                setResponsiveSteps(
                        FormLayout.ResponsiveStep("0", 1, FormLayout.ResponsiveStep.LabelsPosition.TOP),
                        FormLayout.ResponsiveStep("360px", 2, FormLayout.ResponsiveStep.LabelsPosition.TOP))
                verticalLayout {
                    textField("Nombre:") {
                        bind(binder).bind(GrupoDTO::nombre)
                        addValueChangeListener { detailsDrawerHeader.setTitle(it.value) }
                    }
                    radioButtonGroup {
                        addThemeVariants(RadioGroupVariant.LUMO_VERTICAL)
                        setItems("Activo", "Suspendido")
                        binder.forField(this@radioButtonGroup).bind(GrupoDTO::getActivoStr, GrupoDTO::setActivoStr)
                    }
                    newGroupView = div {
                        textField("Administrador") {
                            addValueChangeListener { checkNombreAdministrador(it.value) }
                            isRequired = true
                            isClearButtonVisible = true
                        }
                        nombreLargoAdminitrador = span {}
                    }
                    updateGroupView = div {
                        label("Ultima modificación:")
                        fechaModificacion = span { }
                        label(" Modificó:")
                        usuarioModificacion = span { }
                    }
                 }
                verticalLayout {
                    val permiteCompanias = item.permiteCompanias.asSequence().map { it.nombre }.toSet()

                    label("Compañias permitidas") { }
                    twinColSelect(unSelCompanies(permiteCompanias), permiteCompanias) {
                        addSelectionListener {
                            val companiasPermitidas = ArrayList<CompaniaDTO>()

                            it.value.forEach {
                                companiasPermitidas.add(companiaService.findByName(it).block()!!)
                            }
                            binder.bean.permiteCompanias = companiasPermitidas
                            saveButton.isEnabled = true
                        }
                    }
                    val noPermiteCompanias = item.noPermiteCompanias.asSequence().map { it.nombre }.toSet()

                    label("Compañias no permitidas") { }
                    twinColSelect(unSelCompanies(noPermiteCompanias), noPermiteCompanias) {
                        addSelectionListener {
                            val companiasNoPermitidas = ArrayList<CompaniaDTO>()

                            it.value.forEach {
                                companiasNoPermitidas.add(companiaService.findByName(it).block()!!)
                            }
                            binder.bean.noPermiteCompanias = companiasNoPermitidas
                            saveButton.isEnabled = true
                        }
                    }

                    val permiteSinHerencia = item.permiteSinHerencia.asSequence().map { it.nombre }.toSet()

                    span("Compañias permitidas sin sus filiales") {}
                    twinColSelect(unSelCompanies(permiteSinHerencia), permiteSinHerencia) {
                        addSelectionListener {
                            val companiasSinHerencia = ArrayList<CompaniaDTO>()

                            it.value.forEach {
                                companiasSinHerencia.add(companiaService.findByName(it).block()!!)
                            }
                            binder.bean.permiteSinHerencia = companiasSinHerencia
                            saveButton.isEnabled = true
                        }
                    }
                }
            }
        }

        private fun unSelCompanies(companies: Set<String>) = validCompanies.toSet().minus(companies)

        override fun onAttach(attachEvent: AttachEvent?) {
            super.onAttach(attachEvent)
            binder.bean = item
            fechaModificacion.text = FormattingUtils.LOCALDATETIME_FORMATTER.format(item.fechaModificacion)
            usuarioModificacion.text = item.usuarioModificacion
            saveButton.isEnabled = false
            isNewGroup = item.id == null
            if (isNewGroup)
                updateGroupView.isVisible = false
            else
                newGroupView.isVisible = false
        }

        private fun checkNombreAdministrador(nombre : String) {
            if (nombre.isEmpty()) return

            val administrador = usuarioCompaniaService.findByNombreUsuario(nombre).blockOptional()

            if (administrador.isPresent) {
                nombreAdministrador = nombre
                nombreLargoAdminitrador.text = administrador.get().nombreUsuario
            } else
                UIUtils.showNotification("No existe el usuario $nombre en la base de datos")
        }

        override fun save(): Boolean {
            try {
                val status = binder.validate()

                if (status.isOk) {
                    val errors = validateBean()

                    if (errors.isEmpty()) {
                        binder.bean.fechaModificacion = LocalDateTime.now()
                        binder.bean.usuarioModificacion = securityService.authenticatedUser.get().name
                        if (isNewGroup)
                            service.newGrupo(binder.bean, nombreAdministrador).block()
                        else
                            service.updateGrupo(binder.bean).block()
                        return true
                    } else
                        Notification.show(errors.joinToString("; ") { it }, 3000, Notification.Position.BOTTOM_START)
                } else
                    Notification.show(status.validationErrors.joinToString("; ") { it.errorMessage }, 3000, Notification.Position.BOTTOM_START)
            } catch (e: Exception) {
                UIUtils.showNotification("Error actualizando el grupo :${e.message}")
                e.printStackTrace()
            }

            return false
        }

        override fun validateBean(): List<String>  {
            val errors = ArrayList<String>()

            if (binder.bean.permiteCompanias.isEmpty() && binder.bean.permiteSinHerencia.isEmpty())
                errors.add("Se tiene que tener alguna compañía permitida al grupo")
            if (isNewGroup && nombreAdministrador.isEmpty())
                errors.add("Para dar de alta un Grupo se requiere definir a un primer Administrador")

            return errors
        }

    }

    /**
     * Class to handle Administrators
     */
    class AdministradorDetail(var item: UsuarioDTO,
                              val service: UsuarioCompaniaService,
                              val companiaService: CompaniaService,
                              val grupoService: GrupoService,
                              detailsDrawerHeader : DetailsDrawerHeader,
                              val saveButton: Button,
                              val validGroups : List<String>,
                              val startGroup:String?,
                              val validCompanies: List<String>,
                              val startCompany: String?): KComposite(), DetailController {

        private val binder: BeanValidationBinder<UsuarioDTO>
        private lateinit var isNewUserCheckBox: Checkbox
        private lateinit var fromUserTextField: TextField
        private lateinit var nombreTextField: TextField
        private lateinit var supervisorTextField: TextField
        private lateinit var usuarioFormLayout: FormLayout
        private lateinit var gruposTwinColSelect: TwinColSelect<String>
        private lateinit var companiasTwinColSelect: TwinColSelect<String>
        private var originalGroups: Set<String> = setOf()
        private var originalCompanies: Set<String> = setOf()

        private var isNewUser = item.id == null

        init {
            binder = BeanValidationBinder<UsuarioDTO>(UsuarioDTO::class.java)
            binder.addStatusChangeListener { saveButton.isEnabled = true }
        }

        val root = ui {
            formLayout{
                setResponsiveSteps(
                        FormLayout.ResponsiveStep("0", 1, FormLayout.ResponsiveStep.LabelsPosition.TOP),
                        FormLayout.ResponsiveStep("360px", 2, FormLayout.ResponsiveStep.LabelsPosition.TOP))
                isNewUserCheckBox = checkBox("Administrador ya existente") {
                    value = false
                    addValueChangeListener {
                        isNewUser = !it.value
                        usuarioFormLayout.isEnabled = isNewUser
                        fromUserTextField.isVisible = !isNewUser
                    }
                }
                fromUserTextField = textField("Nombre usuario ya existente:") {
                    isVisible = false
                    addValueChangeListener {
                        tryToSetExistingUser(it.value)
                    }
                }
                usuarioFormLayout = formLayout {
                    textField("Id Usuario:") {
                        bind(binder).withConverter(StringToLongConverter("Se espera un entero"))
                                    .withValidator({ idUsuario -> isIdUsuarioUnique(idUsuario) }, "El id del administrador debe ser único")
                                    .bind(UsuarioDTO::idUsuario)
                    }
                    textField("Nombre largo:") {
                        bind(binder).bind(UsuarioDTO::nombre)
                        addValueChangeListener { detailsDrawerHeader.setTitle(it.value) }
                        addValueChangeListener { if (it.value.contains(' ') && item.nombre.isEmpty()) {
                                                    // set nombre with the first letter of the name and the last name
                                                    val words = it.value.split(" ")

                                                    nombreTextField.value = (words[0].first() + words[1]).lowercase(Locale.getDefault())
                                                 }
                                                }
                    }
                    nombreTextField = textField("Nombre:") {
                        bind(binder).withValidator({ nombre -> isNameUnique(nombre) }, "El nombre del administrador debe ser único")
                                    .bind(UsuarioDTO::nombreUsuario)
                    }
                    textField("Teléfono:") {
                        bind(binder).bind(UsuarioDTO::telefono)
                    }
                    textField("mail:") {
                        bind(binder).bind(UsuarioDTO::mail)
                    }
                    textField("Zona horaria:") {
                        bind(binder).bind(UsuarioDTO::zonaHoraria)
                    }
                    datePicker("Fecha de ingreso:"){
                        bind(binder).bind(UsuarioDTO::fechaIngreso)
                        max = LocalDate.now()
                    }
                    supervisorTextField = textField("Nombre del supervisor:") {
                        addValueChangeListener {
                            if (!value.isEmpty()) {
                                if (tryToSetSupervisor(it.value).isEmpty())
                                    it.source.value = it.oldValue
                            } else
                                item.supervisor = null
                        }
                    }
                    radioButtonGroup {
                        addThemeVariants(RadioGroupVariant.LUMO_VERTICAL)
                        setItems("Activo", "Suspendido")
                        binder.forField(this@radioButtonGroup).bind(UsuarioDTO::getActivoStr, UsuarioDTO::setActivoStr)
                    }
                }
                originalGroups = if (startGroup == null)
                                    item.grupos.asSequence().map { it.id.toString() + ":" + it.nombre }.toSet()
                                 else {
                                    item.grupos.add(grupoService.findById(startGroup.substring(0, startGroup.indexOf(":")).toLong()).block()!!)
                                    setOf(startGroup)
                }

                div {
                    span("Grupos en los que es miembro el Administrador") {}
                    gruposTwinColSelect = twinColSelect(unSelGroups(originalGroups), originalGroups) {
                        addSelectionListener {
                            val miembroGrupos = ArrayList<GrupoDTO>()

                            it.value.forEach {
                                miembroGrupos.add(grupoService.findById(it.substring(0, it.indexOf(":")).toLong()).block()!!)
                            }
                            binder.bean.grupos = miembroGrupos
                            saveButton.isEnabled = true
                        }
                    }
                }
                originalCompanies = if (startCompany == null)
                                    item.companias.asSequence().map { it.nombre }.toSet()
                                else {
                                    item.companias.add(companiaService.findByName(startCompany).block()!!)
                                    setOf(startCompany)
                                }
                div {
                    span("Compañias en las que trabaja (se requiere una compañía)") {}
                    companiasTwinColSelect = twinColSelect(unSelCompanies(originalCompanies), originalCompanies) {
                        addSelectionListener {
                            val trabajaCompanias = ArrayList<CompaniaDTO>()

                            it.value.forEach {
                                trabajaCompanias.add(companiaService.findByName(it).block()!!)
                            }
                            binder.bean.companias = trabajaCompanias
                            saveButton.isEnabled = true
                        }
                    }
                }
            }
        }

        private fun tryToSetExistingUser(nombre: String) {
            if (nombre.isNotBlank()) {
                val checkUsuario = service.findByName(nombre).blockOptional()

                if (checkUsuario.isPresent) {
                    item = checkUsuario.get()
                    binder.bean = item                  // reset all fields
                    saveButton.isEnabled = true

                    // set supervisorTextField
                    supervisorTextField.value = if (item.supervisor == null) "" else item.supervisor!!.nombre
                    // set gruposTwinColSelect
                    originalGroups = if (startGroup == null)
                                        item.grupos.asSequence().map { it.id.toString() + ":" + it.nombre }.toSet()
                                    else {
                                        item.grupos.add(grupoService.findById(startGroup.substring(0, startGroup.indexOf(":")).toLong()).block()!!)
                                        item.grupos.asSequence().map {  it.id.toString() + ":" + it.nombre }.toSet()
                                    }
                    // Reset the Groups with the existing user
                    gruposTwinColSelect.setItems(unSelGroups(originalGroups), originalGroups)
                    // set companiasTwinColselect
                    originalCompanies = if (startCompany == null)
                                            item.companias.asSequence().map { it.id.toString() + ":" + it.nombre }.toSet()
                                        else {
                                            item.companias.add(companiaService.findById(startCompany.substring(0, startCompany.indexOf(":")).toLong()).block()!!)
                                            item.companias.asSequence().map {  it.id.toString() + ":" + it.nombre }.toSet()
                                        }
                    // Reset the Companies with the existing user
                    companiasTwinColSelect.setItems(unSelCompanies(originalCompanies), originalCompanies)

                    return
                }
            }

            UIUtils.showNotification("No existe el administrador en la base de datos")

            return
        }

        private fun tryToSetSupervisor(nombreSupervisor: String): String {
            if (nombreSupervisor.isNotBlank()) {
                val checkUsuario = service.findByNombreUsuario(nombreSupervisor).blockOptional()

                if (checkUsuario.isPresent) {
                    binder.bean.supervisor = checkUsuario.get()

                    return checkUsuario.get().nombre
                }
            }
            UIUtils.showNotification("No existe el supervisor $nombreSupervisor en la base de datos")

            return ""
        }

        private fun isNameUnique(nombre: String?): Boolean {
            if (nombre.isNullOrBlank()) return true

            val checkUsuario = service.findByNombreUsuario(nombre).blockOptional()

            if (checkUsuario.isEmpty) return true

            return binder.bean!!.id == checkUsuario.get().id
        }

        private fun isIdUsuarioUnique(idUsuario: Long?): Boolean {
            if (idUsuario == null) return true

            val checkUsuario = service.findByIdUsuario(idUsuario).block() ?: return true

            return binder.bean!!.id == checkUsuario.id
        }

        private fun unSelGroups(groups: Set<String>) = validGroups.toSet().minus(groups)

        private fun unSelCompanies(companies: Set<String>) = validCompanies.toSet().minus(companies)

        override fun onAttach(attachEvent: AttachEvent?) {
            super.onAttach(attachEvent)
            binder.bean = item
            saveButton.isEnabled = false
            isNewUserCheckBox.isVisible = isNewUser
            supervisorTextField.value = if (item.supervisor == null) "" else item.supervisor!!.nombre
        }

        override fun save(): Boolean {
            try {
                val status = binder.validate()

                if (status.isOk) {
                    val errors = validateBean()

                    if (errors.isEmpty()) {
                        service.saveUsuario(binder.bean).block()
                        return true
                    } else
                        Notification.show(errors.joinToString("; ") { it }, 3000, Notification.Position.BOTTOM_START)
                } else
                    Notification.show(status.validationErrors.joinToString("; ") { it.errorMessage }, 3000, Notification.Position.BOTTOM_START)
            } catch (e: Exception) {
                UIUtils.showNotification("Error actualizando el usuario :${e.message}")
                e.printStackTrace()
            }

            return false
        }

        override fun validateBean(): List<String>  {
            val errors = ArrayList<String>()

            if (binder.bean.grupos.isEmpty() && binder.bean.companias.isEmpty())
                errors.add("El administrador debe de ser miembro al menos en un grupo. Y no trabaja en ninguna compañía")
            // Validate that the Group will no be empty for any Amdinistrator
            val groups = binder.bean.grupos.asSequence().map { it.id.toString() + ":" + it.nombre }.toSet()

            originalGroups.minus(groups).forEach {
                if (grupoService.countMiembros(it.substring(0, it.indexOf(":")).toLong()).block()!! <= 1L)
                    errors.add("El administrador NO se puede salir del grupo $it ya que se quedaría el grupo sin ningún administrador")
            }
            if (binder.bean.companias.isEmpty() )
                errors.add("El usuario debe de trabajar al menos en una compañía")

            return errors
        }

    }

    /**
     *  Class to handle Employees for the selected Company.
     */
    class UsuarioDetail(val item: UsuarioDTO,
                        val service: UsuarioCompaniaService,
                        val companiaService: CompaniaService,
                        detailsDrawerHeader : DetailsDrawerHeader,
                        val saveButton: Button,
                        val validCompanies : List<String>,
                        val startCompany:String?): KComposite(), DetailController {

        private val binder: BeanValidationBinder<UsuarioDTO>
        private lateinit var nombreTextField: TextField
        private lateinit var supervisorTextField: TextField

        init {
            binder = BeanValidationBinder<UsuarioDTO>(UsuarioDTO::class.java)
            binder.addStatusChangeListener { saveButton.isEnabled = true }
        }

        val root = ui {
            formLayout{
                setResponsiveSteps(
                        FormLayout.ResponsiveStep("0", 1, FormLayout.ResponsiveStep.LabelsPosition.TOP),
                        FormLayout.ResponsiveStep("360px", 2, FormLayout.ResponsiveStep.LabelsPosition.TOP))
                textField("Id Usuario:") {
                    bind(binder).withConverter(StringToLongConverter("Se espera un entero"))
                                .withValidator({ idUsuario ->  isIdUsuarioUnique(idUsuario)}, "El id del usuario debe ser único")
                                .bind(UsuarioDTO::idUsuario)
                }
                textField("Nombre largo:") {
                    bind(binder).bind(UsuarioDTO::nombre)
                    addValueChangeListener{ detailsDrawerHeader.setTitle(it.getValue()) }
                    addValueChangeListener { if (it.value.contains(' ') && item.nombre.isEmpty()) {
                                                // set nombre with the first letter of the name and the last name
                                                val words = it.value.split(" ")

                                                nombreTextField.value = (words[0].first() + words[1]).lowercase(Locale.getDefault())
                                            }
                    }
                }
                nombreTextField = textField("Nombre:") {
                    bind(binder).withValidator({nombre ->  isNameUnique(nombre)}, "El nombre del usuario debe ser único")
                                .bind(UsuarioDTO::nombreUsuario)
                }
                textField("Teléfono:") {
                    bind(binder).bind(UsuarioDTO::telefono)
                }
                textField("mail:") {
                    bind(binder).bind(UsuarioDTO::mail)
                }
                textField("Zona horaria:") {
                    bind(binder).bind(UsuarioDTO::zonaHoraria)
                }
                datePicker("Fecha de ingreso:") {
                    bind(binder).bind(UsuarioDTO::fechaIngreso)
                }
                supervisorTextField = textField("Nombre del supervisor:") {
                    addValueChangeListener {
                        if (!value.isEmpty()) {
                            if (tryToSetSupervisor(it.value).isEmpty())
                                it.source.value = it.oldValue
                        } else
                            item.supervisor = null
                    }
                }
                radioButtonGroup {
                    addThemeVariants(RadioGroupVariant.LUMO_VERTICAL)
                    setItems("Activo", "Suspendido")
                    binder.forField(this@radioButtonGroup).bind(UsuarioDTO::getActivoStr, UsuarioDTO::setActivoStr)
                }
                val companias = if (startCompany == null)
                                    item.companias.asSequence().map { it.nombre }.toSet()
                                else {
                                    item.companias.add(companiaService.findByName(startCompany).block()!!)
                                    setOf(startCompany)
                                }

                span("Compañias en las que trabaja (se requiere una compañía)") {}
                twinColSelect(unSelCompanies(companias), companias) {
                    addSelectionListener {
                        val trabajaCompanias = ArrayList<CompaniaDTO>()

                        it.value.forEach {
                            trabajaCompanias.add(companiaService.findByName(it).block()!!)
                        }
                        binder.bean.companias = trabajaCompanias
                        saveButton.isEnabled = true
                    }
                }
            }
        }

        private fun isNameUnique(nombre: String?): Boolean {
            if (nombre.isNullOrBlank()) return true

            val checkUsuario = service.findByNombreUsuario(nombre).blockOptional()

            if (checkUsuario.isEmpty) return true

            return binder.bean!!.id == checkUsuario.get().id
        }

        private fun isIdUsuarioUnique(idUsuario: Long?): Boolean {
            if (idUsuario == null) return true

            val checkUsuario = service.findByIdUsuario(idUsuario).blockOptional()

            if (checkUsuario.isEmpty) return true

            return binder.bean!!.id == checkUsuario.get().id
        }

        private fun tryToSetSupervisor(nombreAdministrador: String): String {
            if (nombreAdministrador.isNotBlank()) {

                val checkUsuario = service.findByNombreUsuario(nombreAdministrador).blockOptional()

                if (checkUsuario.isPresent) {
                    binder.bean.supervisor = checkUsuario.get()

                    return checkUsuario.get().nombre
                }
            }
            UIUtils.showNotification("No existe el supervisor $nombreAdministrador en la base de datos YY")

            return ""
        }

        private fun unSelCompanies(companies: Set<String>) = validCompanies.toSet().minus(companies)

        override fun onAttach(attachEvent: AttachEvent?) {
            super.onAttach(attachEvent)
            binder.bean = item
            saveButton.isEnabled = false
            supervisorTextField.value = if (item.supervisor == null) "" else item.supervisor!!.nombreUsuario
        }

        override fun save(): Boolean {
            try {
                val status = binder.validate()

                if (status.isOk) {
                    val errors = validateBean()

                    if (errors.isEmpty()) {
                        service.saveUsuario(binder.bean).block()
                        return true
                    } else
                        Notification.show(errors.joinToString("; ") { it }, 3000, Notification.Position.BOTTOM_START)
                } else
                    Notification.show(status.validationErrors.joinToString("; ") { it.errorMessage }, 3000, Notification.Position.BOTTOM_START)
            } catch (e: Exception) {
                UIUtils.showNotification("Error actuaizando el usuario :${e.message}")
                e.printStackTrace()
            }

            return false
        }

        override fun validateBean(): List<String>  {
            val errors = ArrayList<String>()

            if (binder.bean.companias.isEmpty() )
                errors.add("El usuario debe de trabajar al menos en una compañía")

            return errors
        }
    }

}
