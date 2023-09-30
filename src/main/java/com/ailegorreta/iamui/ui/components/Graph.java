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
 *  Graph.kt
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.ailegorreta.iamui.ui.components;

import java.util.*;

import com.ailegorreta.client.components.utils.UIUtils;
import com.ailegorreta.client.dataproviders.events.DeleteEvent;
import com.ailegorreta.client.dataproviders.events.SaveEvent;
import com.ailegorreta.client.dataproviders.events.SelectEvent;
import com.vaadin.flow.component.*;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.grid.dnd.GridDropMode;

import com.ailegorreta.iamui.backend.data.dto.DnDDTO;
import com.ailegorreta.iamui.backend.data.dto.GraphDTO;

/**
 *  Custom component to display Alchemys graphs y d3.js library
 *
 *  @author rlh
 *  @project : iam-ui
 *  @date September 2022
 */
@SuppressWarnings("serial")
@Tag("graph")
@SpringComponent
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
/*


@JsModule("./js/d3/d3.js")
@JavaScript("./js/lodash/dist/lodash.min.js")
@JavaScript("./js/lodash/dist/lodash.compat.min.js")
@JavaScript("./js/lodash/dist/lodash.underscore.min.js")
@JsModule("./js/jquery/dist/jquery.min.js")
//@JavaScript("./js/bootstrap/dist/js/bootstrap.min.js")
@JavaScript("./js/alchemyjs/dist/alchemy.min.js")
@CssImport("./js/alchemyjs/dist/alchemy.css")
//@JavaScript("./js/alchemyjs/dist/scripts/vendor.js")
@CssImport("./js/alchemyjs/dist/styles/vendor.css")
@JavaScript("./js/graph.js")
@JavaScript("./js/tree.js")

 */
public class Graph extends Composite<Div> {

	private static final String CLASS_NAME = "graph";
    private static Logger logger = LoggerFactory.getLogger(Graph.class);

    private Page 					page = null;
    private Grid<String>			dnDropZone;
    private boolean					droppedObject;
    private final ObjectMapper		mapper;
    private GraphDTO 				item;
    private HashMap<Integer,Integer>selectedNodes = new HashMap<Integer, Integer>();
    
	public Graph(ObjectMapper mapper) {
    	this.mapper = mapper;
        page = UI.getCurrent().getPage();
        this.setId(CLASS_NAME);
        dnDropZone = createDnDropZone();
        getContent().add(dnDropZone);        
 	}


	@Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        getContent().setSizeFull();

        page = attachEvent.getUI().getPage();

		page.addJavaScript("/js/d3/d3.js");
		page.addJavaScript("/js/jquery/dist/jquery.min.js");
		page.addJavaScript("/js/lodash/dist/lodash.min.js");
		page.addJavaScript("/js/lodash/dist/lodash.compat.min.js");
		page.addJavaScript("/js/lodash/dist/lodash.underscore.min.js");
		page.addJavaScript("/js/bootstrap/dist/js/bootstrap.min.js");

        // retrieves Alchemys
        page.addStyleSheet("/js/alchemyjs/dist/alchemy.css");
        page.addStyleSheet("/js/alchemyjs/dist/styles/vendor.css");

        page.addJavaScript("/js/alchemyjs/dist/alchemy.min.js");
        page.addJavaScript("/js/alchemyjs/dist/scripts/vendor.js");


        page.addJavaScript("/js/graph.js"); 	// retrieves from /src/main/resources/META-INF/resources/frontend/js/
		page.addJavaScript("/js/tree.js");

		// ^ This page load all Javascript from all pages (I don´´ know why) so we need
		// to include all js file including the tree.js

		addDnd(attachEvent);
    }

    /*
     * This Grid is to support DnD between its caller and the graph.
     *
     * note: The example for Vaadin DnD between differente components
     *       (i.e, a Grid and this component) did not work, so we emulate it
     *       other Grid with one item.
     */
	private Grid<String> createDnDropZone() {
		Grid<String> grid = new Grid<String>();

		grid.addColumn(new ComponentRenderer<>(this::createAssign))
    		.setAutoWidth(true)
    		.setFlexGrow(0)
    		.setTextAlign(ColumnTextAlign.END);
		grid.addColumn(new ComponentRenderer<>(this::createUnAssign))
    		.setAutoWidth(true)
    		.setFlexGrow(0)
    		.setTextAlign(ColumnTextAlign.END);
		grid.addColumn(new ComponentRenderer<>(this::createRefresh))
				.setAutoWidth(true)
				.setFlexGrow(0)
				.setTextAlign(ColumnTextAlign.END);

		grid.setSelectionMode(SelectionMode.NONE);
		grid.setWidthFull();
		grid.setHeight("45px");

		return grid;
	}
	
	public void setVisibleDnDropZone(boolean visible) {
		dnDropZone.setVisible(visible);
	}
	
	private void addDnd(AttachEvent attachevent) {
		dnDropZone.setDropMode(GridDropMode.ON_GRID);
		dnDropZone.setRowsDraggable(true);
		dnDropZone.addDropListener(event -> this.droppedObject = true );
	}
	
	private void setDnDropTitle(String title) {
		List<String> items = new ArrayList<String>();
		
		items.add(title);
		dnDropZone.setItems(items);		
	}

	public boolean itemDropped(DnDDTO itemDropped, boolean notDroppedObject, Object item) {
		if (droppedObject || notDroppedObject) {	
			selectedNodes.clear();	
			page.executeJs("other_data="+this.item.addNode(mapper, itemDropped)+ ";updateGraph();");
			page.executeJs("initOnClick();");

			droppedObject = false;		
			fireEvent(new SaveEvent(this, true, item));
		}
		
		return false;
	}
	
	public void deleteNodes(List<DnDDTO> deleteNodes) {
		selectedNodes.clear();			
		deleteNodes.forEach(node -> item.deleteNode(node));
		page.executeJs("other_data="+item.jsonString(mapper)+ ";updateGraph();");
		page.executeJs("initOnClick();");		
	}
	
	private Component createAssign(String title) {		
		return UIUtils.createH5Label(UIUtils.shortDescription(title, 25));
	}
	
	private Component createUnAssign(String title) {		
		Button unAssignButton =  UIUtils.createButton(VaadinIcon.TRASH, ButtonVariant.LUMO_SMALL,
												      				    ButtonVariant.LUMO_TERTIARY);
		
		unAssignButton.addClickListener(e -> {
			if (selectedNodes.isEmpty())
				UIUtils.showNotification("No se tienen ningún nodo seleccionado para desasignar");
			else 
				fireEvent(new DeleteEvent(this, true, selectedNodes));
		});
		
		return unAssignButton;
	}

	public Component createRefresh(String title) {
		Button refreshButton =  UIUtils.createButton(VaadinIcon.REFRESH, ButtonVariant.LUMO_SMALL,
				ButtonVariant.LUMO_TERTIARY);

		refreshButton.addClickListener(e -> { updateGraph(); });

		return refreshButton;
	}
	
	public void startGraph(GraphDTO item) {
		this.item = item;
		setDnDropTitle(item.getDndDropTitle());
		selectedNodes.clear();	
		page.executeJs("other_data=" + item.jsonString(mapper) + ";renderGraph();");
		page.executeJs("initOnClick();");
	}
	
	public Registration addSaveListener(ComponentEventListener<SaveEvent> listener) {
		return addListener(SaveEvent.class, listener);
	}
	
	public Registration addDeleteListener(ComponentEventListener<DeleteEvent> listener) {
		return addListener(DeleteEvent.class, listener);
	}

	public Registration addSelectListener(ComponentEventListener<SelectEvent> listener) {
    	return addListener(SelectEvent.class, listener);
	}

	/*
	 * The Graph.js call this method is the user clicks a npde
	 */
	@ClientCallable
	public void selectedNode(Integer nodeId, Integer indx) {
		if (selectedNodes.get(indx) != null)		// toggle the selected node. If selected clear selection otherwise select nodes
			selectedNodes.remove(indx);
		else {
			selectedNodes.put(indx, nodeId);
			fireEvent(new SelectEvent(this, true, nodeId));
		}
	}
    
	public void updateGraph() {
		page.executeJs("updateGraph()");
		page.executeJs("initOnClick();");
	}

	public void setWidth(Integer width) {  page.executeJs("graphWidth=" + width +";"); }

	public void setHeight(Integer height) { page.executeJs("graphHeight=" + height +";"); }
}
