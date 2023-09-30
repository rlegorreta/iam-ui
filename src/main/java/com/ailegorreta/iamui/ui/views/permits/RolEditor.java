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
 *  RolEditor.java
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.ailegorreta.iamui.ui.views.permits;

import java.util.*;
import java.util.stream.Stream;

import com.ailegorreta.client.components.ui.Initials;
import com.ailegorreta.client.components.ui.ListItem;
import com.ailegorreta.client.components.ui.SearchBar;
import com.ailegorreta.client.components.ui.layout.size.*;
import com.ailegorreta.client.components.utils.UIUtils;
import com.ailegorreta.client.dataproviders.events.CancelEvent;
import com.ailegorreta.client.dataproviders.events.SaveEvent;
import com.ailegorreta.commons.utils.FormattingUtils;
import com.vaadin.flow.component.grid.dnd.GridDropMode;
import com.vaadin.flow.component.littemplate.LitTemplate;
import com.vaadin.flow.component.template.Id;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.BindingValidationStatus;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.converter.StringToLongConverter;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.shared.Registration;
import com.ailegorreta.iamui.backend.data.dto.facultad.AssignFacultadDTO;
import com.ailegorreta.iamui.backend.data.dto.facultad.FacultadDTO;
import com.ailegorreta.iamui.backend.data.dto.Node;
import com.ailegorreta.iamui.backend.data.dto.facultad.RolDTO;
import com.ailegorreta.iamui.backend.data.service.RolService;
import com.ailegorreta.iamui.ui.components.Graph;
import com.ailegorreta.iamui.ui.dataproviders.FacultadesGridDataProvider;
import com.ailegorreta.iamui.ui.exceptions.SaveValidationException;
import com.ailegorreta.iamui.ui.exceptions.DeleteValidationException;
import com.ailegorreta.iamui.backend.data.dto.DnDDTO;
import org.vaadin.artur.spring.dataprovider.FilterablePageableDataProvider;

/**
 * Polymeter components form for Roles.
 *
 * This class is called by Roles.java class
 *
 * Also this class uses the Graph component in order to graph the
 * Role and its assigned permits.
 *
 * The format of the page is done in HTML using rol-editor.js  file
 *
 *  @author rlh
 *  @project : iam-ui
 *  @date september 2023
 */
@org.springframework.stereotype.Component
@Scope("prototype")
@Tag("rol-editor")
@JsModule("./views/iam/rol-editor.js")
public class RolEditor extends LitTemplate {

	@Id("idNeo4j")
	private Span idNeo4j;

	@Id("idRol")
	private TextField idRol;

	@Id("nombre")
	private TextField nombre;

	@Id("activo")
	private RadioButtonGroup<String> activo;

	@Id("fechaModificacion")
	private Span fechaModificacion;
	
	@Id("cancel")
	private Button cancel;

	@Id("save")
	private Button save;
	
	@Id("searchFacultades")
	private SearchBar 			searchBarFacultades;

	@Id("gridFacultades")
	private Grid<FacultadDTO> 	gridFacultades;

	private BeanValidationBinder<RolDTO> binder = new BeanValidationBinder<>(RolDTO.class);
	
	private final FacultadesGridDataProvider 	facultadDataProvider;
	private final RolService					service;
	private Graph								graph;

	private List<FacultadDTO>	  				draggedItems;

	private Long								prevIdRol;

	@Autowired
	public RolEditor(FacultadesGridDataProvider facultadDataProvider, RolService service) {
		this.facultadDataProvider = facultadDataProvider;
		this.service = service;
	
		this.setId("rol-editor");
		
		configureEditForm();
		configureAssignFacultad();
	}
	
	public void setGraph(Graph graph) {
		this.graph = graph;

		graph.addSaveListener(e -> {
									try {
										Object item = e.getItem();
									
										if (item instanceof AssignFacultadDTO)
											service.assignPermit((AssignFacultadDTO)item).block();
										else 
											throw new SaveValidationException("El tipo de datos debe ser AssignFacultadDTO");
									} catch (Exception e1) {
										UIUtils.showNotification("Error al asignar la facultad al rol:" + e1.getMessage());
										e1.printStackTrace();
									}
									});
		graph.addDeleteListener(e -> {
										try {
											Object item = e.getItem();
											
											if (item instanceof HashMap) {
												@SuppressWarnings("unchecked")
												HashMap<Integer, Integer> itemsSelected = (HashMap<Integer, Integer>) item;
												
												// Validate that the user does not select the role node
												final boolean[] areValid = {false};
																								
												itemsSelected.keySet().forEach(key -> {
													Node node = binder.getBean().getGraph().node(itemsSelected.get(key));
													
													if (node.getType().equals("rol"))  // node type role is defined as type_1 see RolDTO class
														areValid[0] = true;
												});
												if (areValid[0])
													UIUtils.showNotification("Se tiene seleccionado al rol, solo se puede seleccionar facultadas");
												else { // do deletion
													ArrayList<DnDDTO> unAssignFacultades = new ArrayList<DnDDTO>();
													
													itemsSelected.keySet().forEach(key -> {
														Node 				facultadNode = binder.getBean().getGraph().node(itemsSelected.get(key));
														FacultadDTO		  	facultad = binder.getBean().facultadById(facultadNode.getIdNeo4j());

														AssignFacultadDTO 	unAssignFacultadDTO = new AssignFacultadDTO( facultad.getNombre(), binder.getBean());
																									
														service.unAssignPermit(unAssignFacultadDTO).block();
														unAssignFacultades.add(facultad);
													});
													graph.deleteNodes(unAssignFacultades);
												}
											} else 
												throw new DeleteValidationException("El tipo de datos debe ser HashMap");											
										} catch (Exception e1) {
											UIUtils.showNotification("Error al desasignar la facultad al rol:" + e1.getMessage());
											e1.printStackTrace();
										}			
									});
	}

	/*
	 * note: since it is a LitElement component just extra no HTML logic is added here
	 */
	private void configureEditForm() {
		// cancel.addClickListener(e -> fireEvent(new CancelEvent(this, false)));
		idRol.setRequired(true);
		binder.forField(idRol)
			  .withConverter(new StringToLongConverter("El campo debe ser entero"))
			  .withValidator((idRol) -> isIdRolUnique(idRol), "El número del rol debe ser único")
			  .bind(RolDTO::getIdRol, RolDTO::setIdRol);
		
		idRol.setRequired(true);
		binder.bind(nombre, "nombre");
		
		activo.setItems("Activo", "Suspendido");
		binder.forField(activo)
		  	  .bind(RolDTO::getActivoStr, (o, s) -> { o.setActivoStr(s); });

		binder.addValueChangeListener(e -> {
			if (e.getOldValue() != null) 
				save.setEnabled(true);
		});
		
		cancel.addClickListener(e -> fireEvent(new CancelEvent(this, true)));
		save.addClickListener(e -> fireEvent(new SaveEvent(this, true)));
	}
	
	private Boolean isIdRolUnique(Long idRol) {
		if ((idRol == null) || (idRol == 0)) return false;

		if ((prevIdRol != null) && (prevIdRol == idRol)) return true;
				
		var checkRol = service.findByIdRol(idRol).blockOptional();
				
		if (checkRol.isEmpty()) return true;
				
		return Objects.equals(binder.getBean().getId(), checkRol.get().getId());
	}
	
	private void configureAssignFacultad() {
		searchBarFacultades.setActionText("Asignar");
		searchBarFacultades.setCheckboxText("Activas");
		searchBarFacultades.setPlaceHolder("Búsqueda");

		gridFacultades.setSelectionMode(Grid.SelectionMode.SINGLE);

		gridFacultades.addSelectionListener(event -> event.getFirstSelectedItem()
			    							              .ifPresent(this::selectFacultad));
		gridFacultades.setItems((FilterablePageableDataProvider) facultadDataProvider);

		gridFacultades.addColumn(new ComponentRenderer<>(this::createUserInfo))
					  .setAutoWidth(true)
					  .setHeader("Facultad")
				      .setSortProperty("nombre");
		gridFacultades.addColumn(new ComponentRenderer<>(this::createActive))
		    		  .setAutoWidth(true)
		    		  .setFlexGrow(0)
		    		  .setHeader("Activa")
				      .setTextAlign(ColumnTextAlign.END);
		
		createDndFunctionality();
		
		searchBarFacultades.addFilterChangeListener(e -> {  
            if (!searchBarFacultades.getFilter().isEmpty() || searchBarFacultades.isCheckboxChecked()) 
            	facultadDataProvider.setFilter(new FacultadesGridDataProvider.FacultadFilter(searchBarFacultades.getFilter(), 
            																				 searchBarFacultades.isCheckboxChecked()));
            else
            	facultadDataProvider.setFilter(FacultadesGridDataProvider.FacultadFilter.getEmptyFilter());          
		});
		searchBarFacultades.getActionButton().getElement().setAttribute("new-button", true);
		searchBarFacultades.addActionClickListener(e -> {
        							if (gridFacultades.getSelectedItems().isEmpty())
										UIUtils.showNotification("No se ha seleccionado una facultad");
        							else {
        								FacultadDTO 		facultad = (FacultadDTO)gridFacultades.getSelectedItems().toArray()[0];
        								AssignFacultadDTO 	item = new AssignFacultadDTO(facultad.getNombre(), binder.getBean());

										if (binder.getBean().getId() == null || binder.getBean().getId() <= 0)
										   UIUtils.showNotification("El rol  " + binder.getBean().getNombre() +
												   					" es nuevo rol. Guardar primero el rol antes de asignarles facultades");
        								else if (item.validate())
        									graph.itemDropped(facultad, true, item);
        								else
    										UIUtils.showNotification("La facultad " + facultad.getNombre() + " ya está asignada al rol " + binder.getBean().getNombre());        									
        							}
        						});
	}
	
	private void createDndFunctionality() {
		gridFacultades.setRowsDraggable(true);

		gridFacultades.addDragStartListener(event -> draggedItems = event.getDraggedItems());

		gridFacultades.addDragEndListener(event -> {
			if (draggedItems == null || draggedItems.isEmpty()) 
				draggedItems=null;
			else {
				FacultadDTO			facultad = draggedItems.iterator().next();
				AssignFacultadDTO 	item = new AssignFacultadDTO(facultad.getNombre(), binder.getBean());

				if (binder.getBean().getId() == null || binder.getBean().getId() <= 0)
					UIUtils.showNotification("El rol  " + binder.getBean().getNombre() +
											 " es nuevo rol. Guardar primero el rol antes de asignarles facultades");
				else if (item.validate())
					graph.itemDropped(facultad, true, item );
				else
					UIUtils.showNotification("La facultad " + facultad.getNombre() + " ya está asignada al rol " + binder.getBean().getNombre());        														
				draggedItems = null;
			}
		});
	}

	public boolean hasChanges() {
		return binder.hasChanges() || save.isEnabled();
	}

	public void close() {
	}

	public void write(RolDTO rol) throws ValidationException {
		binder.writeBean(rol);		
	}

	public void read(RolDTO rol, boolean isNew) {
		binder.setBean(rol);

		idNeo4j.setText(isNew ? "" : rol.getId().toString());
		prevIdRol = rol.getIdRol();
		fechaModificacion.setText(FormattingUtils.LOCALDATETIME_FORMATTER.format(rol.getFechaModificacion()));
		graph.startGraph(rol);
		save.setEnabled(false);
	}
	
	public RolDTO getBean() {
		return binder.getBean();
	}

	public BeanValidationBinder<RolDTO> getBinder() { return binder; }

	public Stream<HasValue<?, ?>> validate() {
		Stream<HasValue<?, ?>> errorFields = binder.validate()
												   .getFieldValidationErrors()
												   .stream()
												   .map(BindingValidationStatus::getField);

		return errorFields;
	}

	public Registration addSaveListener(ComponentEventListener<SaveEvent> listener) {
		return addListener(SaveEvent.class, listener);
	}

	public Registration addCancelListener(ComponentEventListener<CancelEvent> listener) {
		return addListener(CancelEvent.class, listener);
	}
	
	private Component createUserInfo(FacultadDTO facultad) {
		ListItem item = new ListItem(new Initials(facultad.getTipo().getInitials()),
									 facultad.getNombre(),
									 UIUtils.shortDescription(facultad.getDescripcion(), 15));
		
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
	
	private void selectFacultad(FacultadDTO facultad) {
	}

}
