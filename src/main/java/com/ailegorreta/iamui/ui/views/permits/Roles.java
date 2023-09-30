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
 *  Rolse.java
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.ailegorreta.iamui.ui.views.permits;

import java.time.LocalDateTime;

import com.ailegorreta.client.components.ui.FlexBoxLayout;
import com.ailegorreta.client.components.ui.SearchBar;
import com.ailegorreta.client.components.ui.layout.size.*;
import com.ailegorreta.client.components.utils.SimpleDialog;
import com.ailegorreta.client.components.utils.UIUtils;
import com.ailegorreta.client.components.utils.css.*;
import com.ailegorreta.client.navigation.detailsdrawer.DetailsDrawer;
import com.ailegorreta.client.navigation.detailsdrawer.DetailsDrawerHeader;
import com.ailegorreta.client.security.service.CurrentSession;
import com.ailegorreta.iamui.backend.data.Role;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.data.binder.BinderValidationStatus;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.ailegorreta.iamui.backend.data.dto.facultad.RolDTO;
import com.ailegorreta.iamui.backend.data.service.RolService;
import com.ailegorreta.iamui.ui.MainLayout;
import com.ailegorreta.iamui.ui.components.Graph;
import com.ailegorreta.iamui.ui.dataproviders.RolesGridDataProvider;
import com.ailegorreta.iamui.ui.dataproviders.RolFilter;
import com.ailegorreta.iamui.ui.views.SplitViewFrame;
import org.springframework.context.annotation.Scope;
import org.vaadin.artur.spring.dataprovider.FilterablePageableDataProvider;

import jakarta.annotation.security.RolesAllowed;

/**
 * Page to handle new/update and suspend Roles.
 *
 * This page was developed with the following characteristics:
 * - It is a SplitView frame (best Vaadin practices)
 * - All is done in Java
 * - We use Polymeter component for the Rol form making a better
 *   solution (compare to Facultades class)
 * - This code is similar to the Vaadin example for custom polymeter
 *   web components.
 * - We use the Graph component to display a graph of Roles with
 *   its assigned permits.
 *
 *
 *  @author rlh
 *  @project : iam-ui
 *  @date September 2023
 */
@org.springframework.stereotype.Component
@Scope("prototype")
@Route(value = "roles", layout = MainLayout.class)
@PageTitle("Roles")
@RolesAllowed({Role.ADMIN_IAM, Role.ALL})
public class Roles extends SplitViewFrame implements SimpleDialog.Caller {

	private final RolesGridDataProvider		dataProvider;
	private final RolService				service;
	private final CurrentSession 			securityService;
	
	private DetailsDrawer 					detailsDrawer;
	private DetailsDrawerHeader 			detailsDrawerHeader;
		
	private final RolEditor					editor;
	private final Graph						graph;
	
	public Roles(CurrentSession securityService,
				 RolesGridDataProvider dataProvider, RolService service,
				 RolEditor editor, Graph graph) {
		this.securityService = securityService;
		this.dataProvider = dataProvider;
		this.service = service;
		this.editor = editor;
		this.graph = graph;
		editor.setGraph(graph);
		this.setId("roles");
		setViewContent(createContent());
		setViewDetails(createDetailsDrawer());
	}

	private Component createContent() {
		var content = new FlexBoxLayout(new VerticalLayout(createSearchBar(), createGrid()));
		
		content.setBoxSizing(BoxSizing.BORDER_BOX);
		content.setHeightFull();
		content.setPadding(Horizontal.RESPONSIVE_X, Top.RESPONSIVE_X);
		
		return content;
	}

	private SearchBar createSearchBar() {
		var searchBar = new SearchBar();
		
        searchBar.setActionText("Nuevo rol");
		searchBar.setCheckboxText("Activos");
        searchBar.setPlaceHolder("Búsqueda");
        searchBar.addFilterChangeListener(e -> {  
             if (!searchBar.getFilter().isEmpty() || searchBar.isCheckboxChecked()) 
            	dataProvider.setFilter(new RolFilter(searchBar.getFilter(), searchBar.isCheckboxChecked()));
             else
            	dataProvider.setFilter(RolFilter.getEmptyFilter());          
        });
        searchBar.getActionButton().getElement().setAttribute("new-button", true);
        searchBar.addActionClickListener(e -> {
        							RolDTO rol = newRol();
        							
    								detailsDrawerHeader.setTitle(rol.getNombre());
    								detailsDrawer.show();        	
        						});
        
        return searchBar;
	}
	
	private Grid<RolDTO> createGrid() {
		Grid<RolDTO> grid = new Grid<>();

		grid.addSelectionListener(event -> event.getFirstSelectedItem()
											    .ifPresent(this::showDetails));
		grid.setItems((FilterablePageableDataProvider) dataProvider);
		grid.setHeightFull();

		grid.addColumn(RolDTO::getId)
				.setAutoWidth(true)
				.setFlexGrow(0)
				.setFrozen(true)
				.setHeader("ID");
		grid.addColumn(RolDTO::getIdRol)
				.setAutoWidth(true)
				.setHeader("Id Rol")
				.setSortProperty("idRol");
		grid.addColumn(RolDTO::getNombre)
				.setAutoWidth(true)
				.setHeader("Rol")
				.setSortProperty("nombre");
		grid.addColumn(new ComponentRenderer<>(this::createActive))
				.setAutoWidth(true)
				.setFlexGrow(0)
				.setHeader("Activo")
				.setTextAlign(ColumnTextAlign.END);
		grid.addColumn(RolDTO::getUsuarioModificacion)
				.setAutoWidth(true)
				.setHeader("Usuario modificó");
 		grid.addColumn(new ComponentRenderer<> (this::createDate))
				.setAutoWidth(true)
				.setFlexGrow(0)
				.setHeader("Fecha última modificación")
				.setTextAlign(ColumnTextAlign.END);

		return grid;
	}

	private Component createActive(RolDTO rol) {		
		if (rol.getActivo())
			return UIUtils.createPrimaryIcon(VaadinIcon.CHECK);
		else 
			return UIUtils.createDisabledIcon(VaadinIcon.CLOSE);
	}

	private Component createDate(RolDTO rol) {
		return new Span(UIUtils.formatLocalDateTime(rol.getFechaModificacion()));
	}

	private DetailsDrawer createDetailsDrawer() {
		detailsDrawer = new DetailsDrawer(DetailsDrawer.Position.RIGHT);

		// Header
		detailsDrawerHeader = new DetailsDrawerHeader("");
		detailsDrawerHeader.addCloseListener(buttonClickEvent -> detailsDrawer.hide());
		detailsDrawer.setHeader(detailsDrawerHeader);

		// Contents
		detailsDrawer.setContent(createDetails(), graph);
		
		/* Footer
		/* Since for this case we are using Polymeter template we include the Save and 
		 * cancel buttons inside RolEditor, so we create us as listeners for the save
		 * and cancel button using evens. 
		 * note: This is different way to solve the same problem, so for this POC we
		 *       declare us as listeners
		 *       
		 * So in conclusion we do not create a detailDrawers footer. i.e., all funcionality
		 * is done in RolEditor as it should be.

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
		editor.addSaveListener(e -> { try {
											BinderValidationStatus<RolDTO> status = editor.getBinder().validate();

											if (status.isOk()) {
												editor.getBean().setUsuarioModificacion(securityService.currentUser().getName());
												editor.getBean().setFechaModificacion(LocalDateTime.now());
												service.saveRol(editor.getBean()).block();
												detailsDrawer.hide();
												dataProvider.refreshAll();
												UIUtils.showNotification("Rol almacenado");
											} else {
												StringBuffer errors = new StringBuffer();

												if (status.getBeanValidationErrors().size() == 0)
													errors.append("El Rol tiene errores de edición");
													// ^ sometimes Vaadin does not return the errors. Fix this problem
												else status.getBeanValidationErrors().forEach( error -> {
													errors.append(error.getErrorMessage());
													errors.append("; ");
												});
												Notification.show( errors.toString(),3000, Notification.Position.BOTTOM_START);
											}
										} catch (Exception e1) {
											detailsDrawer.hide();
											UIUtils.showNotification("Error actualizar el rol:" + e1.getMessage());
											e1.printStackTrace();
										}});
		editor.addCancelListener(e -> {
										if (editor.hasChanges()) 
											(new SimpleDialog("Se tienen campos pendientes de guardar.", this)).open();											
										else
											detailsDrawer.hide();	
										}); 
		
		return detailsDrawer;
	}
	
	public void dialogResponseOk(SimpleDialog ok) {
		detailsDrawer.hide();
		dataProvider.refreshAll();
	}
	
	public void dialogResponseCancel(SimpleDialog ok) {
		// noop		
	}
	
	private void showDetails(RolDTO rol) {
		detailsDrawerHeader.setTitle(rol.getNombre());
		selectRol(rol);

		detailsDrawer.show();
	}

	private Component createDetails() {
		editor.setVisible(true);
		
		return editor;
	}
	
	private void selectRol(RolDTO rol) {
		// We re-read the DTO in order to get its facultades (i.e., Neo4j depth > 0)
		rol = service.findById(rol.getId()).block();
		editor.read(rol, false);
	}
	
	private RolDTO newRol() {
		RolDTO rol = new RolDTO(null, 0L, "ROL_NUEVO", true,
								securityService.currentUser().getName(), LocalDateTime.now());

		editor.read(rol, true);
		
		return rol;
	}
}
