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
 *  Vista.kt
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.ailegorreta.iamui.ui.views.permits

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.mvysny.karibudsl.v10.*
import com.ailegorreta.iamui.backend.data.Role
import com.ailegorreta.iamui.backend.data.dto.facultad.PerfilDTO
import com.ailegorreta.iamui.backend.data.dto.facultad.TreePerfil
import com.ailegorreta.iamui.backend.data.service.PerfilService
import com.ailegorreta.client.components.ui.Initials
import com.ailegorreta.client.components.ui.ListItem
import com.ailegorreta.client.components.ui.flexBoxLayout
import com.ailegorreta.client.components.ui.layout.size.Horizontal
import com.ailegorreta.client.components.ui.layout.size.Right
import com.ailegorreta.client.components.ui.layout.size.Top
import com.ailegorreta.client.components.ui.layout.size.Vertical
import com.ailegorreta.client.components.utils.UIUtils
import com.ailegorreta.client.components.utils.css.BoxSizing
import com.ailegorreta.iamui.ui.MainLayout
import com.ailegorreta.iamui.ui.components.Tree
import com.ailegorreta.iamui.ui.components.navigation.bar.AppBar
import com.ailegorreta.iamui.ui.dataproviders.PerfilesGridDataProvider
import com.ailegorreta.iamui.ui.views.ViewFrame
import com.vaadin.flow.component.AttachEvent
import com.vaadin.flow.component.DetachEvent
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.grid.ColumnTextAlign
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.icon.Icon
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.page.BrowserWindowResizeEvent
import com.vaadin.flow.component.page.ExtendedClientDetails
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
 * Page to display all permits for a specific Profile.
 *
 * It shows the Profile -> Role -> Permit hierarchy.
 *
 * Classes in this file:
 * - Vista: page that display a grid (with paging) with all the Profiles.
 * - VistaDetails: page that display a D3Tree for the profile.
 *
 *  @author rlh
 *  @project : iam-ui
 *  @date September 2023
 */
@Component
@Scope("prototype")
@PageTitle("Vista")
@Route(value = "vista", layout = MainLayout::class)
@RolesAllowed(*arrayOf(Role.ADMIN_IAM, Role.USER_IAM, Role.ALL))
class Vista(dataProvider: PerfilesGridDataProvider,
            service: PerfilService
) : ViewFrame() {
    private var     resizeListener: Registration? = null
    private var     content: Content = Content(service, dataProvider)

    companion object {
        const val MOBILE_BREAKPOINT = 480
    }

    init {
        setViewContent(content)
    }

    class Content(val service: PerfilService,
                  dataProvider: PerfilesGridDataProvider
    ) : KComposite() {

        lateinit var  grid: Grid<PerfilDTO>

        val root = ui {
            flexBoxLayout {
                grid = grid(dataProvider = dataProvider) {
                    addThemeName("mobile")
                    setId("perfiles")
                    setSizeFull()
                    addSelectionListener{ it.firstSelectedItem
                                            .ifPresent(this@Content::showDetails)}

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
                    addColumn(ComponentRenderer { perfil -> createActive(perfil) }).apply {
                        isAutoWidth = true
                        flexGrow = 0
                        setHeader("Activo")
                        setTextAlign(ColumnTextAlign.END)
                    }
                    columnFor(PerfilDTO::usuarioModificacion) {
                        isAutoWidth = true
                        flexGrow = 0
                        isFrozen = true
                        setHeader("Usuario modificó")
                    }
                    addColumn(ComponentRenderer { perfil -> createDate(perfil) }).apply {
                        isAutoWidth = true
                        flexGrow = 0
                        setHeader("Fecha última modificación")
                        setTextAlign(ColumnTextAlign.END)
                    }
                }
                setBoxSizing(BoxSizing.BORDER_BOX)
                setHeightFull()
                setPadding(Horizontal.RESPONSIVE_X, Top.RESPONSIVE_X)
            }
        }

        private fun createPerfilInfo(perfil: PerfilDTO): ListItem =
                ListItem(
                    Initials(if (perfil.activo) (if (perfil.patron) " P" else " E") else (if (perfil.patron) "XP" else "XE")),
                        perfil.nombre,
                        perfil.descripcion).apply {
                                                setPadding(Vertical.XS)
                                                setSpacing(Right.M)
                                            }

        private fun createActive(perfil: PerfilDTO): Icon =
                if (perfil.activo)
                    UIUtils.createPrimaryIcon(VaadinIcon.CHECK)
                else
                    UIUtils.createDisabledIcon(VaadinIcon.CLOSE)

        private fun createDate(perfil: PerfilDTO): Span = Span(UIUtils.formatLocalDateTime(perfil.fechaModificacion))

        private fun showDetails(perfil: PerfilDTO) {
            UI.getCurrent().navigate(VistaDetails::class.java, perfil.id)
        }
    }

    override fun onAttach(attachEvent: AttachEvent) {
        super.onAttach(attachEvent)
        ui.ifPresent { ui: UI ->
            val page = ui.page
            resizeListener = page.addBrowserWindowResizeListener { event: BrowserWindowResizeEvent -> updateVisibleColumns(event.width) }
            page.retrieveExtendedClientDetails { details: ExtendedClientDetails -> updateVisibleColumns(details.bodyClientWidth) }
        }
    }

    override fun onDetach(detachEvent: DetachEvent) {
        resizeListener!!.remove()
        super.onDetach(detachEvent)
    }

    private fun updateVisibleColumns(width: Int) {
        val mobile = width < MOBILE_BREAKPOINT
        val columns = content.grid.columns
        // "Mobile" column
        columns[0].isVisible = mobile
        // "Desktop" columns
        for (i in 1 until columns.size)
            columns[i].isVisible = !mobile
    }
}

@PageTitle("Vista Details")
@Route(value = "vista-details", layout = MainLayout::class)
@RolesAllowed(*arrayOf(Role.ADMIN_IAM, Role.USER_IAM, Role.ALL))
class VistaDetails(private val service: PerfilService,
                   private val mapper: ObjectMapper,
                   private val tree: Tree) : ViewFrame(), HasUrlParameter<Long> {

    private var title = " "

    override fun onAttach(attachEvent: AttachEvent?) {
        super.onAttach(attachEvent)
        initAppBar()
    }

    override fun setParameter(beforeEvent: BeforeEvent?, id: Long?) {
        val item = service.findById(id!!).block()

        if (item != null) {
            val content = Content(item, mapper, tree)

            title = item.nombre
            UI.getCurrent().page.setTitle(title)
            setViewContent(content)
            content.updateTree()
        } else
            UIUtils.showNotification("El nombre del perfil no se encontró en la base de datos")
    }

    private fun initAppBar(): AppBar? {
        val appBar = MainLayout.get().appBar

        appBar.setNaviMode(AppBar.NaviMode.CONTEXTUAL)
        appBar.contextIcon.addClickListener { UI.getCurrent().navigate(Vista::class.java) }
        appBar.title = title

        return appBar
    }

    class Content(val item: PerfilDTO, val mapper: ObjectMapper, val tree: Tree): KComposite() {
        private var treeAlreadyRendered = false
        private lateinit var div: Div

        val root = ui {
            verticalLayout {
                minHeight = "400px"

                label {
                    item.descripcion
                }

                div = div { }
            }
        }

        fun updateTree() {
            val datum = ArrayList<TreePerfil>(1)

            datum.add(TreePerfil.mapFromEntity(item))

            val data = mapper.writeValueAsString(datum)

            if (!treeAlreadyRendered) {
                div.add(tree)
                tree.startTree(data)
                treeAlreadyRendered = true
            } else {
                val content = div.parent.get() as VerticalLayout

                content.remove(div)
                div = Div()
                div.add(tree)
                content.add(div)
                tree.updateTree(data)
            }
        }
    }
}
