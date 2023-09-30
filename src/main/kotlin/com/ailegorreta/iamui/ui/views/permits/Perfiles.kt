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
 *  Perfiles.kt
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.ailegorreta.iamui.ui.views.permits

import com.github.mvysny.karibudsl.v10.*
import com.ailegorreta.iamui.backend.data.dto.DnDDTO
import com.ailegorreta.iamui.backend.data.dto.facultad.AssignRolDTO
import com.ailegorreta.iamui.backend.data.dto.facultad.PerfilDTO
import com.ailegorreta.iamui.backend.data.dto.facultad.RolDTO
import com.ailegorreta.iamui.backend.data.service.PerfilService
import com.ailegorreta.client.components.ui.*
import com.ailegorreta.client.components.ui.layout.size.Horizontal
import com.ailegorreta.client.components.ui.layout.size.Right
import com.ailegorreta.client.components.ui.layout.size.Top
import com.ailegorreta.client.components.ui.layout.size.Vertical
import com.ailegorreta.client.components.utils.LumoStyles
import com.ailegorreta.client.components.utils.SimpleDialog
import com.ailegorreta.client.components.utils.UIUtils
import com.ailegorreta.client.components.utils.css.BoxSizing
import com.ailegorreta.client.dataproviders.events.CancelEvent
import com.ailegorreta.client.dataproviders.events.SaveEvent
import com.ailegorreta.client.navigation.detailsdrawer.DetailsDrawer
import com.ailegorreta.client.navigation.detailsdrawer.DetailsDrawerHeader
import com.ailegorreta.client.security.service.CurrentSession
import com.ailegorreta.commons.utils.FormattingUtils
import com.ailegorreta.iamui.backend.data.Role
import com.ailegorreta.iamui.ui.MainLayout
import com.ailegorreta.iamui.ui.components.Graph
import com.ailegorreta.iamui.ui.dataproviders.PerfilFilter
import com.ailegorreta.iamui.ui.dataproviders.PerfilesGridDataProvider
import com.ailegorreta.iamui.ui.dataproviders.RolFilter
import com.ailegorreta.iamui.ui.dataproviders.RolesGridDataProvider
import com.ailegorreta.iamui.ui.exceptions.DeleteValidationException
import com.ailegorreta.iamui.ui.exceptions.SaveValidationException
import com.ailegorreta.iamui.ui.views.SplitViewFrame
import com.vaadin.flow.component.ComponentEventListener
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.formlayout.FormLayout
import com.vaadin.flow.component.grid.ColumnTextAlign
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.icon.Icon
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.radiobutton.RadioGroupVariant
import com.vaadin.flow.data.binder.BeanValidationBinder
import com.vaadin.flow.data.renderer.ComponentRenderer
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import jakarta.annotation.security.RolesAllowed

/**
 * Page to handle new/update and suspend Profiles.
 *
 * This page was developed with the following characteristics:
 * - It is a SplitView frame (best Vaadin practices)
 * - All is done in Koltin (simplifies the code by 30%)
 * - We use Vaadin on Kotlin form making a better
 *   solution (compare to Roles class)
 *
 *  @see: http://www.vaadinonkotlin.eu for more tutorials.
 *
 *  note: Vaadin on Kotlin is NOT a library but a Kotlin
 *        DSL for Vaadin.
 *        Here we use just the  Karibu-DSL piece of Vaadin
 *        on Kotlin.
 *
 *  * - We use the Graph component to display a Graph of Profiles with
 *   its assigned Roles.
 *
 * Classes in this file:
 * - Perfiles: page thta displays a grid (paging) of Profiles.
 * - Detail: view form for the Profile and its graph
 *
 *  @author rlh
 *  @project : iam-ui
 *  @date September 2023
 */
@Component
@Scope("prototype")
@Route(value = "perfiles", layout = MainLayout::class)
@PageTitle("Perfiles")
@RolesAllowed(*arrayOf(Role.ADMIN_IAM, Role.ALL))
class Perfiles: SplitViewFrame, SimpleDialog.Caller {

	private val 				dataProvider: PerfilesGridDataProvider
	private val 				service: PerfilService
	private val 				securityService: CurrentSession
	private val					graph: Graph

	private lateinit var 		detailsDrawer: DetailsDrawer
	private lateinit var 		detailsDrawerHeader: DetailsDrawerHeader
	
	private val 				binder = BeanValidationBinder(PerfilDTO::class.java)
	private lateinit var		editor: Detail

	constructor(securityService: CurrentSession,
				dataProvider: PerfilesGridDataProvider,
				service: PerfilService,
				rolesDataProvider: RolesGridDataProvider,
				graph: Graph) {
		this.securityService = securityService
		this.dataProvider = dataProvider
		this.service = service
		this.graph = graph

		setViewDetails(createDetailDrawer(rolesDataProvider))
		setViewContent(Content(securityService, service, dataProvider, detailsDrawer, editor, binder))
	}
	
	fun createDetailDrawer(rolesDataProvider: RolesGridDataProvider): DetailsDrawer {
		detailsDrawer = DetailsDrawer(DetailsDrawer.Position.RIGHT)
		
		// Header
		detailsDrawerHeader = DetailsDrawerHeader("")
		detailsDrawerHeader.addCloseListener{ detailsDrawer.hide() }
		detailsDrawer.setHeader(detailsDrawerHeader)
		
		//	Contents
		editor = Detail(binder, detailsDrawerHeader, service, rolesDataProvider, graph)
		detailsDrawer.setContent(editor, graph)
		binder.addValueChangeListener{ editor.setSaveEnabled(true) }		

		// Footer
		/* We include the Save and cancel buttons inside Detail because we want the roles grid and
         * the graph to be at the button of the Detaildrwaer, so we create us as listeners for the save
		 * and cancel button using evens. 
		 * note: This is different way to solve the same problem, so for this POC we
		 *       declare us as listeners
		 *       
		 * So in conclusion we do not create a detailDrawers footer. i.e., all funcionality
		 * is done in class Detail as it shoul be.

		DetailsDrawerFooter footer = new DetailsDrawerFooter();
		
		footer.addSaveListener(e -> {
										....
									});
		footer.addCancelListener(e -> {
										....	
									});
		detailsDrawer.setFooter(footer);
		saveButton = footer.getSaveButton();
		saveButton.setEnabled(false);
		 */
		
		editor.addSaveListener {
									try {
			            				val status = binder.validate()

										if (status.isOk) {
											binder.getBean().usuarioModificacion = securityService.authenticatedUser.get().name
											binder.getBean().fechaModificacion = LocalDateTime.now()
											service.savePerfil(binder.bean).block()
											detailsDrawer.hide()
											dataProvider.refreshAll()
											UIUtils.showNotification("Perfil almacenado")
										} else
											if (status.validationErrors.size == 0)
												Notification.show("El perfil tiene errores de edición", 3000, Notification.Position.BOTTOM_START)
												// ^ sometimes Vaadin does not return the errors. Fix this problem
											else
												Notification.show(status.validationErrors.joinToString("; ") { it.errorMessage }, 3000, Notification.Position.BOTTOM_START)
									} catch (e: Exception) {
										detailsDrawer.hide()
										UIUtils.showNotification("Error actualizando el perfil:" + e.message)
										e.printStackTrace()
									}
								}
		editor.addCancelListener {
								  if (editor.isSaveEnabled()) 
									(SimpleDialog("Se tienen campos pendientes de guardar.", this)).open()									
								  else
									detailsDrawer.hide()
								 }
		editor.setSaveEnabled(false)
		
		return detailsDrawer
	}
	
	override fun dialogResponseCancel(dialog: SimpleDialog?) { /* noop */ }

	override fun dialogResponseOk(dialog: SimpleDialog?) {
		detailsDrawer.hide()
		dataProvider.refreshAll()
	}
	
	// Main content: a SearchBar and a Grid for Perfiles	
	class Content(private val securityService : CurrentSession,
				  val service: PerfilService,
				  dataProvider: PerfilesGridDataProvider,
				  private val detailsDrawer: DetailsDrawer,
				  val editor: Detail,
				  val binder: BeanValidationBinder<PerfilDTO>): KComposite() {

		val root = ui {
					flexBoxLayout {
						setBoxSizing(BoxSizing.BORDER_BOX)
						setHeightFull()
						setPadding(Horizontal.RESPONSIVE_X, Top.RESPONSIVE_X)
						
						verticalLayout {
							isPadding = false; content { align(stretch, top) }
												
							searchBar {
								setActionText("Nuevo perfil") 
								setCheckboxText("Activos")
								setPlaceHolder("Búsqueda")
								addFilterChangeListener{  
									if (!this@searchBar.filter.isEmpty() || this@searchBar.isCheckboxChecked()) 
										dataProvider.setFilter( PerfilFilter(this@searchBar.getFilter(), this@searchBar.isCheckboxChecked()))
									else
										dataProvider.setFilter(PerfilFilter.emptyFilter)
								}
								getActionButton().getElement().setAttribute("new-button", true)
								addActionClickListener{
	        											val perfil = newPerfil()
	        											val detailsDrawerHeader = detailsDrawer.getHeader().getComponentAt(0)
	        											
	        											if (detailsDrawerHeader is DetailsDrawerHeader)
	        												detailsDrawerHeader.setTitle(perfil.nombre)
	        											detailsDrawer.show()     	
	        										}
							}
							grid(dataProvider = dataProvider) {
								setHeightFull()
								addSelectionListener{ it.getFirstSelectedItem()
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
									setAutoWidth(true)
									flexGrow = 0
									setHeader("Activo")
									setTextAlign(ColumnTextAlign.END)
								}
								columnFor(PerfilDTO::usuarioModificacion) {
									isAutoWidth = true
									flexGrow = 0
									setHeader("Usuario modificó")
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
		}
		
		private fun createPerfilInfo(perfil: PerfilDTO): ListItem =
			ListItem(Initials(if (perfil.activo) (if (perfil.patron) " P" else " E") else (if (perfil.patron) "XP" else "XE")),
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
		
		private fun selectPerfil(perfil: PerfilDTO) {
			// we re-read the DTO in order to get its roles (i.e., Neo4j depth > 0)
			editor.read(service.findById(perfil.id!!).block()!!)
		}
				
		private fun newPerfil(): PerfilDTO {
			val perfil = PerfilDTO( nombre = "PERFIL NUEVO", descripcion = "", patron = false, activo = true,
									usuarioModificacion = securityService.authenticatedUser.get().name,
									fechaModificacion = LocalDateTime.now())

			editor.read(perfil)

			return perfil
		}
		
		private fun showDetails(perfil: PerfilDTO) {
			val detailsDrawerHeader = detailsDrawer.getHeader().getComponentAt(0)
        											
			if (detailsDrawerHeader is DetailsDrawerHeader)
				detailsDrawerHeader.setTitle(perfil.nombre)
			selectPerfil(perfil)

			detailsDrawer.show()
		}	
	}

	/*
	 * Profile form
	 */
	class Detail(val binder: BeanValidationBinder<PerfilDTO>,
				 detailsDrawerHeader: DetailsDrawerHeader,
				 val service: PerfilService,
				 rolesDataProvider: RolesGridDataProvider,
				 val graph: Graph): FormLayout() {
	
		private val buttons: HorizontalLayout
		private val saveButton: Button
		private var fechaCreacion: Span = Span()
		private var gridRoles: Grid<RolDTO> = Grid()
		private var searchBar: SearchBar = SearchBar()
		private var draggedItems: List<RolDTO> = ArrayList()
		
		init {
			addClassNames(LumoStyles.Padding.Bottom.L,
						  LumoStyles.Padding.Horizontal.L,
						  LumoStyles.Padding.Top.S)
			setResponsiveSteps(
					ResponsiveStep("0", 1, ResponsiveStep.LabelsPosition.TOP),
					ResponsiveStep("600px", 4, ResponsiveStep.LabelsPosition.TOP))

			formLayout {
				setResponsiveSteps(
						ResponsiveStep("0", 1, ResponsiveStep.LabelsPosition.TOP),
						ResponsiveStep("360px", 2, ResponsiveStep.LabelsPosition.TOP))
				textField("Nombre:") {
					bind(binder).withValidator({nombre ->  isNameUnique(nombre)}, "El nombre del perfil debe ser único")
							    .bind(PerfilDTO::nombre)
					setWidthFull()
					addValueChangeListener{ detailsDrawerHeader.setTitle(it.getValue()) }
				}
				textField("Descripción:") {
					bind(binder).bind(PerfilDTO::descripcion)
					setWidthFull()
				}
			}
			formLayout {
				setResponsiveSteps(
						ResponsiveStep("0", 1, ResponsiveStep.LabelsPosition.TOP),
						ResponsiveStep("500px", 3, ResponsiveStep.LabelsPosition.TOP))
				radioButtonGroup<String> {
					addThemeVariants(RadioGroupVariant.LUMO_VERTICAL)
					setItems("Activo", "Suspendido")
					binder.forField(this@radioButtonGroup).bind(PerfilDTO::getActivoStr, PerfilDTO::setActivoStr)
				}
				checkBox("Patrón") {
					bind(binder).bind(PerfilDTO::patron)
				}
				this@Detail.fechaCreacion = span { }
			}
			buttons = horizontalLayout {
				setWidthFull()
				button("Guardar Perfil") {
					addThemeVariants(ButtonVariant.LUMO_PRIMARY)
					icon = UIUtils.createPrimaryIcon(VaadinIcon.ARROW_RIGHT)
					isIconAfterText = true
					onLeftClick{ this@Detail.fireEvent( SaveEvent(this, true)) }
				}
				button("Cancelar") {
					getStyle().set("margin-left", "auto")
					onLeftClick{ this@Detail.fireEvent( CancelEvent(this, true)) }					
				}					
			}
			div {
				setId("main")
				formLayout {
					setResponsiveSteps(
							ResponsiveStep("0", 1, ResponsiveStep.LabelsPosition.TOP),
							ResponsiveStep("500px", 2, ResponsiveStep.LabelsPosition.TOP))
					this@Detail.searchBar = searchBar {
						setActionText("Asignar")
						setCheckboxText("Activos")
						setPlaceHolder("Búsqueda")
						getActionButton().getElement().setAttribute("new-button", true)
						addFilterChangeListener{
							if (!this@searchBar.filter.isEmpty() || this@searchBar.isCheckboxChecked())
								rolesDataProvider.setFilter(RolFilter(this@searchBar.getFilter(), this@searchBar.isCheckboxChecked()) )
							else
								rolesDataProvider.setFilter(RolFilter("", false))
						}
					}
					this@Detail.gridRoles = grid(dataProvider = rolesDataProvider) {
						setSelectionMode(Grid.SelectionMode.SINGLE)
						columnFor(RolDTO::nombre) {
							setHeader("Rol")
							isAutoWidth = true
							setSortProperty("nombre")
						}
						addColumn(ComponentRenderer<Icon, RolDTO>({ rol -> createActive(rol) })).apply {
							isAutoWidth = true
							flexGrow = 0
							setHeader("Activo")
							setTextAlign(ColumnTextAlign.END)
						}

					}
				}
			}
			saveButton = buttons.getComponentAt(0) as Button
			searchBar.addActionClickListener{
         					if (gridRoles.getSelectedItems().isEmpty())
								UIUtils.showNotification("No se ha seleccionado un rol")
							else {
        						val rol = gridRoles.getSelectedItems().toTypedArray()[0]
        						val item = AssignRolDTO(rol.idRol, binder.getBean())

								if (binder.getBean().id == null || binder.getBean().id!! <= 0)
									UIUtils.showNotification("El perfil  " + binder.getBean().nombre + " es nuevo perfil. Guardar primero el perfil antes de asignarles roles")
								else if (item.validate())
        							graph.itemDropped(rol, true, item)
        						else
    								UIUtils.showNotification("El rol " + rol.nombre + " ya está asignada al perfil " + binder.getBean().nombre)       									
        					}  	
	        			}
			setGraph()
			createDndFunctionality()
	    }
		
		private fun isNameUnique(nombre: String?): Boolean {
			if (nombre.isNullOrBlank()) return true
			
			val checkPerfil = service.findByName(nombre).blockOptional()
						
			if (checkPerfil.isEmpty) return true
						
			return binder.getBean()!!.id == checkPerfil.get().id
		}
		
		private fun createActive(rol: RolDTO): Icon =
			if (rol.activo)
			 	UIUtils.createPrimaryIcon(VaadinIcon.CHECK)
			else 
				UIUtils.createDisabledIcon(VaadinIcon.CLOSE)
		
		fun addSaveListener(listener: ComponentEventListener<SaveEvent> ) = addListener(SaveEvent::class.java, listener)

	    fun addCancelListener(listener: ComponentEventListener<CancelEvent> ) = addListener(CancelEvent::class.java, listener)
		
		fun isSaveEnabled()= saveButton.isEnabled
		
		fun setSaveEnabled(enabled: Boolean) { saveButton.isEnabled = enabled }
		
		private fun setGraph() {
			graph.addSaveListener {
									try {
										val item = it.getItem()
									
										if (item is AssignRolDTO)
											service.assignRole(item).block()
										else 
											throw SaveValidationException("El tipo de datos debe ser AssignRolDTO")
									} catch (e: Exception) {
										UIUtils.showNotification("Error al asignar el rol al perfil:" + e.message)
										e.printStackTrace()
									}
								}
			graph.addDeleteListener {
										try {
											val item = it.getItem()
											
											if (item is HashMap<*, *>) {
												@SuppressWarnings("unchecked")
												val itemsSelected = item as HashMap<Int, Int>
												
												// Validate that the user does not select the perfil node
												var areValid = false
																								
												for (key in itemsSelected.keys) {
													val node = binder.getBean().graph!!.node(itemsSelected.get(key)!!)
													
													if (node!!.type.equals("perfil"))  // node type perfil is defined as type_3 see PerfilDTO class
														areValid = true
												}
												if (areValid)
													UIUtils.showNotification("Se tiene seleccionado al perfil, solo se puede seleccionar roles")
												else { // do deletion
													val unAssignRoles = ArrayList<DnDDTO>()
													
													for (key in itemsSelected.keys) {
														val rolNode = binder.getBean().graph!!.node(itemsSelected.get(key)!!)
														val rol = binder.getBean().rolById(rolNode!!.idNeo4j)
														val unAssignRolDTO = AssignRolDTO(rol!!.idRol, binder.getBean())
																									
														service.unAssignRole(unAssignRolDTO).block()
														unAssignRoles.add(rol)
													}
													graph.deleteNodes(unAssignRoles)
												}
											} else 
												throw DeleteValidationException("El tipo de datos debe ser HashMap")											
										} catch (e: Exception) {
											UIUtils.showNotification("Error al desasignar el rol al perfil:" + e.message)
											e.printStackTrace()
										}			
									}
		}
		
		private fun createDndFunctionality() {
			gridRoles.isRowsDraggable = true
			gridRoles.addDragStartListener{ draggedItems = it.draggedItems}
			gridRoles.addDragEndListener{
									if (draggedItems.isNotEmpty()) {
										val rol = draggedItems.iterator().next()
										val item = AssignRolDTO(rol.idRol, binder.getBean())

										if (binder.bean.id == null || binder.getBean().id!! <= 0)
											UIUtils.showNotification("El perfil  " + binder.getBean().nombre + " es nuevo perfil. Guardar primero el perfil antes de asignarles roles")
										else if (item.validate())
											graph.itemDropped(rol, true, item )
										else
											UIUtils.showNotification("El rol " + rol.nombre + " ya está asignada al perfil " + binder.getBean().nombre)      														
										draggedItems = ArrayList()
									}
			}
		}

		fun close() {}

		fun write(perfil: PerfilDTO) = binder.writeBean(perfil)

		fun read(perfil: PerfilDTO) {
			binder.setBean(perfil)
			fechaCreacion.text = FormattingUtils.LOCALDATETIME_FORMATTER.format(perfil.fechaModificacion)
			graph.startGraph(perfil)
			setSaveEnabled(false)
		}
	}
}
