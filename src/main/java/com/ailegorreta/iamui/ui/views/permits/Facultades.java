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
 *  Facultaeds.java
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.ailegorreta.iamui.ui.views.permits;

import java.time.LocalDateTime;
import java.util.Objects;

import com.ailegorreta.client.components.ui.FlexBoxLayout;
import com.ailegorreta.client.components.ui.Initials;
import com.ailegorreta.client.components.ui.ListItem;
import com.ailegorreta.client.components.ui.SearchBar;
import com.ailegorreta.client.components.ui.layout.size.*;
import com.ailegorreta.client.components.utils.LumoStyles;
import com.ailegorreta.client.components.utils.SimpleDialog;
import com.ailegorreta.client.components.utils.UIUtils;
import com.ailegorreta.client.components.utils.css.*;
import com.ailegorreta.client.navigation.detailsdrawer.DetailsDrawer;
import com.ailegorreta.client.navigation.detailsdrawer.DetailsDrawerFooter;
import com.ailegorreta.client.navigation.detailsdrawer.DetailsDrawerHeader;
import com.ailegorreta.client.security.service.CurrentSession;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.radiobutton.RadioGroupVariant;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BinderValidationStatus;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.validator.BeanValidator;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.ailegorreta.iamui.backend.data.Role;
import com.ailegorreta.iamui.backend.data.dto.facultad.FacultadDTO;
import com.ailegorreta.iamui.backend.data.dto.facultad.FacultadTipo;
import com.ailegorreta.iamui.backend.data.service.FacultadService;
import com.ailegorreta.iamui.ui.MainLayout;
import com.ailegorreta.iamui.ui.dataproviders.FacultadesGridDataProvider;
import com.ailegorreta.iamui.ui.views.SplitViewFrame;
import org.springframework.context.annotation.Scope;
import org.vaadin.artur.spring.dataprovider.FilterablePageableDataProvider;

import jakarta.annotation.security.RolesAllowed;

import static com.ailegorreta.client.dataproviders.DataProviderUtil.createItemLabelGenerator;

/**
 * Page to handle new/update and suspend Facultades.
 *
 * This page was developed with the following characteristics:
 * - It is a SplitView frame (best Vaadin practices)
 * - All is done in Java
 * - We do not use any Polymeter or Vaadin Kotlin. So it is the
 *   most simple and less efficient solution.
 * - This code is similar to the Vaadin example.
 *
 *
 *  @project : iam-ui
 *  @author rlh
 *  @date September 2023
 */
@org.springframework.stereotype.Component
@Scope("prototype")
@Route(value = "facultades", layout = MainLayout.class)
@PageTitle("Facultades")
@RolesAllowed({Role.ADMIN_IAM, Role.ALL})
public class Facultades extends SplitViewFrame implements SimpleDialog.Caller {

	private final FacultadesGridDataProvider	dataProvider;
	private final FacultadService				service;
	private final CurrentSession 				securityService;
	
	private DetailsDrawer 						detailsDrawer;
	private DetailsDrawerHeader 				detailsDrawerHeader;
	
	private final BeanValidationBinder<FacultadDTO> binder = new BeanValidationBinder<>(FacultadDTO.class);
	private Button							saveButton;
	private String							prevNombre;
	
	public Facultades(CurrentSession securityService,
					  FacultadesGridDataProvider dataProvider,
					  FacultadService service) {
		this.dataProvider = dataProvider;
		this.service = service;
		this.securityService  = securityService;
		setViewContent(createContent());
		setViewDetails(createDetailsDrawer());
	}

	private Component createContent() {
		FlexBoxLayout content = new FlexBoxLayout(new VerticalLayout(createSearchBar(), createGrid()));
		
		content.setBoxSizing(BoxSizing.BORDER_BOX);
		content.setHeightFull();
		content.setPadding(Horizontal.RESPONSIVE_X, Top.RESPONSIVE_X);

		return content;
	}

	private SearchBar createSearchBar() {
		SearchBar searchBar = new SearchBar();

        searchBar.setActionText("Nueva facultad");
		searchBar.setCheckboxText("Activas");
        searchBar.addFilterChangeListener(e -> {
             if (!searchBar.getFilter().isEmpty() || searchBar.isCheckboxChecked()) 
            	dataProvider.setFilter(new FacultadesGridDataProvider.FacultadFilter(searchBar.getFilter(), searchBar.isCheckboxChecked()));
             else
            	dataProvider.setFilter(FacultadesGridDataProvider.FacultadFilter.getEmptyFilter());          
        });
        searchBar.getActionButton().getElement().setAttribute("new-button", true);
        searchBar.addActionClickListener(e -> {
        							FacultadDTO facultad = newFacultad();
        							
    								detailsDrawerHeader.setTitle(facultad.getNombre());
    								detailsDrawer.show();        	
        						});
        
        return searchBar;
	}
	
	private Grid<FacultadDTO> createGrid() {
		Grid<FacultadDTO> grid = new Grid<>();
		
		grid.addSelectionListener(event -> event.getFirstSelectedItem()
											    .ifPresent(this::showDetails));
		grid.setItems((FilterablePageableDataProvider) dataProvider);
		grid.setHeightFull();

		grid.addColumn(FacultadDTO::getId)
				.setAutoWidth(true)
				.setFlexGrow(0)
				.setFrozen(true)
				.setHeader("ID");
		grid.addColumn(new ComponentRenderer<>(this::createUserInfo))
				.setAutoWidth(true)
				.setHeader("Facultad")
				.setSortProperty("nombre");
		grid.addColumn(new ComponentRenderer<>(this::createActive))
				.setAutoWidth(true)
				.setFlexGrow(0)
				.setHeader("Activa")
				.setTextAlign(ColumnTextAlign.END);
		grid.addColumn(FacultadDTO::getUsuarioModificacion)
				.setAutoWidth(true)
				.setFlexGrow(0)
				.setHeader("Usuario modificó")
				.setTextAlign(ColumnTextAlign.START);
 		grid.addColumn(new ComponentRenderer<> (this::createDate))
				.setAutoWidth(true)
				.setFlexGrow(0)
				.setHeader("Ultima modificación")
				.setTextAlign(ColumnTextAlign.END);

		return grid;
	}

	private Component createUserInfo(FacultadDTO facultad) {
		ListItem item = new ListItem(new Initials(facultad.getTipo().getInitials()),
									 facultad.getNombre(),
									 facultad.getDescripcion());
		
		item.setPadding(Vertical.XS);
		item.setSpacing(Right.M);
		
		return item;
	}

	private Component createActive(FacultadDTO facultad) {		
		if (facultad.getActivo())
			return UIUtils.createPrimaryIcon(VaadinIcon.CHECK);
		else 
			return UIUtils.createDisabledIcon(VaadinIcon.CLOSE);
	}

	private Component createDate(FacultadDTO facultad) {
		return new Span(UIUtils.formatLocalDateTime(facultad.getFechaModificacion()));
	}

	private DetailsDrawer createDetailsDrawer() {
		detailsDrawer = new DetailsDrawer(DetailsDrawer.Position.RIGHT);

		// Header
		detailsDrawerHeader = new DetailsDrawerHeader("");
		detailsDrawerHeader.addCloseListener(buttonClickEvent -> detailsDrawer.hide());
		detailsDrawer.setHeader(detailsDrawerHeader);

		// Contents
		detailsDrawer.setContent(createDetails());
		
		// Footer
		DetailsDrawerFooter footer = new DetailsDrawerFooter();
		
		footer.addSaveListener(e -> {   try {
											BinderValidationStatus<FacultadDTO> status = binder.validate();

											if (status.isOk()) {
												binder.getBean().setUsuarioModificacion(securityService.currentUser().getName());
												binder.getBean().setFechaModificacion(LocalDateTime.now());
												service.saveFacultad(binder.getBean()).block();
												detailsDrawer.hide();
												dataProvider.refreshAll();
												UIUtils.showNotification("Facultad almacenada");
											} else {
												StringBuffer errors = new StringBuffer();

												if (status.getBeanValidationErrors().size() == 0)
													errors.append("La facultad tiene errores de edición");
													// ^ sometimes Vaadin does not return the errors. Fix this problem
												else status.getBeanValidationErrors().forEach( error -> {
													errors.append(error.getErrorMessage());
													errors.append("; ");
												});
												Notification.show( errors.toString(),3000, Notification.Position.BOTTOM_START);
											}
										} catch (Exception e1) {
											detailsDrawer.hide();
											UIUtils.showNotification("Error actualizando la facultad:" + e1.getMessage());
											e1.printStackTrace();
										}
									});
		footer.addCancelListener(e -> {
										if (saveButton.isEnabled()) 
											(new SimpleDialog("Se tienen campos pendientes de guardar.", this)).open();											
										else
											detailsDrawer.hide();	
									});
		detailsDrawer.setFooter(footer);
		saveButton = footer.getSaveButton();
		saveButton.setEnabled(false);
		
		return detailsDrawer;
	}
	
	public void dialogResponseOk(SimpleDialog ok) {
		detailsDrawer.hide();
		dataProvider.refreshAll();
	}
	
	public void dialogResponseCancel(SimpleDialog ok) {
		// noop		
	}
	
	private void showDetails(FacultadDTO facultad) {
		detailsDrawerHeader.setTitle(facultad.getNombre());
		selectFacultad(facultad);

		detailsDrawer.show();
	}

	/*
	 * This is the form for a Facultad.
	 * note:The form is done by hand using java Vaadin only
	 */
	private FormLayout createDetails() {
		TextField nombre = new TextField();
		
		binder.forField(nombre)  
			  // .withConverter(nUpper -> nUpper.toUpperCase(Locale.getDefault()), n -> n)
			  .withValidator(this::isNameUnique, "El nombre de la facultad debe ser único")
			  .bind(FacultadDTO::getNombre, FacultadDTO::setNombre);
		
		nombre.addValueChangeListener(e -> detailsDrawerHeader.setTitle(e.getValue()));
		nombre.setWidthFull();

		TextField descripcion = new TextField();
		
		binder.bind(descripcion, "descripcion");
		descripcion.setWidthFull();
		
		RadioButtonGroup<String> activo = new RadioButtonGroup<>();
		
		activo.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);
		activo.setItems("Activa", "Suspendida");
		binder.forField(activo)
		  	  .bind(FacultadDTO::getActivoStr, FacultadDTO::setActivoStr);
		
		var tipo = new ComboBox<FacultadTipo>();

		tipo.setItemLabelGenerator(createItemLabelGenerator(FacultadTipo::getDisplayName));
		tipo.setItems(DataProvider.ofItems(FacultadTipo.values()));
		binder.forField(tipo)
			  .withValidator(new BeanValidator(FacultadDTO.class, "tipo"))
			  .bind(FacultadDTO::getTipo, FacultadDTO::setTipo);
		tipo.setWidthFull();
				
		binder.addValueChangeListener(e -> saveButton.setEnabled(true));
		
		// Form layout
		FormLayout form = new FormLayout();
		
		form.addClassNames(LumoStyles.Padding.Bottom.L,
							LumoStyles.Padding.Horizontal.L,
							LumoStyles.Padding.Top.S);
		form.setResponsiveSteps(
				new FormLayout.ResponsiveStep("0", 1,
						FormLayout.ResponsiveStep.LabelsPosition.TOP),
				new FormLayout.ResponsiveStep("21em", 2,
						FormLayout.ResponsiveStep.LabelsPosition.TOP));
		form.addFormItem(nombre, "Nombre");
		form.addFormItem(descripcion, "Descripción");
		form.addFormItem(activo, "Activo");
		form.addFormItem(tipo, "Tipo facultad");

		return form;
	}
	
	private Boolean isNameUnique(String nombre) {
		if ((nombre == null) || nombre.isEmpty()) return false;

		if (prevNombre != null && prevNombre.equals(nombre)) return true;
				
		var checkFacultad = service.findByName(nombre).blockOptional();
				
		if (checkFacultad.isEmpty()) return true;

		return Objects.equals(binder.getBean().getId(), checkFacultad.get().getId());
	}
	
	private void selectFacultad(FacultadDTO facultad) {
		binder.setBean(facultad);
		prevNombre = facultad.getNombre();
		saveButton.setEnabled(false);
	}
	
	private FacultadDTO newFacultad() {
		FacultadDTO facultad = new FacultadDTO(null, "", null,
							    				FacultadTipo.SIMPLE, securityService.getAuthenticatedUser().get().getName(), LocalDateTime.now());

		binder.removeBean();
		binder.setBean(facultad);
		saveButton.setEnabled(false);
		
		return facultad;
	}
}
