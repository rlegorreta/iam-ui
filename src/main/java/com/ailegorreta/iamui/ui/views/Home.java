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
 *  Home.java
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.ailegorreta.iamui.ui.views;

import com.ailegorreta.client.components.ui.FlexBoxLayout;
import com.ailegorreta.client.components.ui.layout.size.Horizontal;
import com.ailegorreta.client.components.ui.layout.size.Uniform;
import com.ailegorreta.client.components.utils.UIUtils;
import com.ailegorreta.client.navigation.detailsdrawer.DetailsDrawer;
import com.ailegorreta.client.security.service.CurrentSession;
import com.ailegorreta.iamui.ui.MainLayout;
import com.ailegorreta.iamui.ui.components.Graph;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ailegorreta.iamui.backend.data.dto.DnDDTO;
import com.ailegorreta.iamui.backend.data.dto.GraphDTO;
import com.ailegorreta.iamui.backend.data.service.UsuarioService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import org.jetbrains.annotations.NotNull;

import jakarta.annotation.security.PermitAll;

/**
 * Home page of the IAM UI that show the user permits in a graph
 *
 *  @author rlh
 *  @project : iam-ui
 *  @date September 2023
 */
@PageTitle("IAM")
@Route(value = "home", layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
@PermitAll
public class Home extends ViewFrame {

	private final Graph				graph;
	private final UsuarioService 	usuarioService;
	private String					username, loggedUser;

	public Home(CurrentSession securityService,
				Graph graph,
				UsuarioService usuarioService) {
		this.graph = graph;
		this.usuarioService = usuarioService;
		loggedUser = username = securityService.currentUser().getName();
		setId("home");
		setViewContent(createContent());
	}

	private Component createContent() {
		Html intro = new Html("<p>Bienvenido a la aplicación IAM (Identity Access Management) de LMASS Desarrolladores, S.C." +
				              "La aplicación IAM sirve para la administración de las facultades, roles y permisos." +
							  "</p>");

		Html productivity = new Html("<p>Cada usuario de ACME tiene asignado un conjunto de permisos con " +
				"los que puede operar y que son leídos en el momento del log-in del Usuario.  " +
				"Para facilidad en la administración de las diferentes facultades que existen en ACME estas pueden " +
				"ser agrupadas en Roles. Un puesto de trabajo de la empresa contiene varios Roles y a cada usuario se le asigna " +
				"un solo puesto. En caso de excepciones y no tener la necesidad de crear nuevos puestos se les pueden " +
				"asignar o suprimir facultades individuales a los Usuarios. Ya asignados los perfiles por puestos " +
				"de acuerdo a la compañía que trabaje el Usuario se le asigna asi como la asignación las diferentes areas " +
				"de las compañías que son clientes de ACME a los vendedores.</p>");

		Html features = new Html("<p>El usuario que se encuentra en el sistema ACME es: <b>'" +
				loggedUser + "'</b>. Al usuario ya le fueron validadas sus facultades que se encuentran definidas en la " +
				" base de datos y se generó un menú dinámico de acuerdo a sus facultades." +
				"Las facultades del usuario son visualizadas como sigue:</p>");


		TextField usuarioTextField = new TextField("Log-in del usuario:");

		usuarioTextField.setWidth("140px");
		usuarioTextField.setValue(username);
		usuarioTextField.addValueChangeListener(event -> {
			if (!event.getValue().isEmpty()) {
				var usuario = usuarioService.findByNameUsuario(event.getValue()).block();

				if (usuario != null) {
					username = event.getValue();
					features.setVisible(username.equals(loggedUser));

					var grafo = usuarioService.findGrafoFacultades(username).block();

					graph.startGraph(new UsuarioFacultadesGrafo(grafo));
				} else {
					usuarioTextField.setValue(username);
					UIUtils.showNotification("Usuario no existente");
				}
			}
		});

		Div div = new Div();

		DetailsDrawer detailsDrawer = new DetailsDrawer(DetailsDrawer.Position.RIGHT, graph);

		detailsDrawer.setSizeFull();
		graph.setVisibleDnDropZone(false);
		graph.setWidth(600);
		div.add(detailsDrawer);

		FlexBoxLayout content = new FlexBoxLayout(intro, productivity, features,
												  usuarioTextField, graph.createRefresh(""),  div);
		content.setFlexDirection(FlexLayout.FlexDirection.COLUMN);
		content.setMargin(Horizontal.AUTO);
		content.setMaxWidth("840px");
		content.setPadding(Uniform.RESPONSIVE_L);

		return content;
	}

	@Override
	protected void onAttach(AttachEvent attachEvent) {
		graph.startGraph(new UsuarioFacultadesGrafo(usuarioService.findGrafoFacultades(username).block()));
	}

	private class UsuarioFacultadesGrafo implements GraphDTO {

		private final String data;

		public  UsuarioFacultadesGrafo(String data) {
			this.data = data;
		}

		@NotNull
		@Override
		public String jsonString(@NotNull ObjectMapper mapper) {
			return data;
		}

		@NotNull
		@Override
		public String addNode(@NotNull ObjectMapper mapper, @NotNull DnDDTO node) { return ""; }

		@Override
		public void deleteNode(@NotNull DnDDTO node) {
		}

		@NotNull
		@Override
		public String getDndDropTitle() {
			return "";
		}
	}

}
