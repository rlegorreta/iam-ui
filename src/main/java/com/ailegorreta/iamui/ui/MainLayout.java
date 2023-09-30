/* Copyright (c) 2022, LMASS Desarrolladores, S.C.
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
 *  MainLayout.kt
 *
 *  Developed 2022 by LMASS Desarrolladores, S.C. www.lmass.com.mx
 */
package com.ailegorreta.iamui.ui;

import com.ailegorreta.client.components.ui.FlexBoxLayout;
import com.ailegorreta.client.components.utils.UIUtils;
import com.ailegorreta.client.components.utils.css.Overflow;
import com.ailegorreta.client.navigation.navigation.drawer.NaviDrawer;
import com.ailegorreta.client.navigation.navigation.drawer.NaviItem;
import com.ailegorreta.client.navigation.navigation.drawer.NaviMenu;
import com.ailegorreta.client.security.vaadin.LogoutUtil;
import com.ailegorreta.client.security.service.CurrentSession;
import com.ailegorreta.commons.utils.HasLogger;
import com.ailegorreta.iamui.backend.data.service.CacheService;
import com.ailegorreta.iamui.backend.data.service.EventService;
import com.ailegorreta.iamui.backend.service.MessageService;
import com.ailegorreta.iamui.ui.components.WebNotification;
import com.ailegorreta.iamui.config.ServiceConfig;
import com.ailegorreta.iamui.ui.components.navigation.bar.AppBar;
import com.ailegorreta.iamui.ui.components.navigation.bar.TabBar;
import com.ailegorreta.iamui.ui.exceptions.RestClientException;
import com.ailegorreta.iamui.ui.views.operation.Areas;
import com.ailegorreta.iamui.ui.views.operation.SolicitudesAsignacion;
import com.ailegorreta.iamui.ui.views.permits.Perfiles;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.theme.lumo.Lumo;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.server.*;
import com.ailegorreta.iamui.security.SecurityUtils;
import com.ailegorreta.iamui.ui.views.Home;
import com.ailegorreta.iamui.ui.views.permits.Vista;
import com.ailegorreta.iamui.ui.views.permits.Facultades;
import com.ailegorreta.iamui.ui.views.permits.Roles;
import com.ailegorreta.iamui.ui.views.operation.Operation;
import com.ailegorreta.iamui.ui.views.operation.Empleados;
import org.springframework.boot.autoconfigure.web.ServerProperties;

import java.time.LocalDate;

/**
 * Principal view of the IAM-UI
 *
 *  @author rlh
 *  @project : iam-ui
 *  @date September 2023
 */
@SuppressWarnings("serial")
@CssImport(value = "./styles/components/floating-action-button.css", themeFor = "vaadin-button")
@CssImport(value = "./styles/components/grid.css", themeFor = "vaadin-grid")
@CssImport("./styles/lumo/border-radius.css")
@CssImport("./styles/lumo/icon-size.css")
@CssImport("./styles/lumo/margin.css")
@CssImport("./styles/lumo/padding.css")
@CssImport("./styles/lumo/shadow.css")
@CssImport("./styles/lumo/spacing.css")
@CssImport("./styles/lumo/typography.css")
@CssImport("./styles/misc/box-shadow-borders.css")
@CssImport(value = "./styles/styles.css", include = "lumo-badge")
@CssImport("./styles/components/brand-expression.css")
@CssImport("./styles/components/details-drawer.css")
@CssImport("./styles/components/details-drawer-l.css")
@CssImport("./styles/components/navi-menu.css")
@CssImport("./styles/components/navi-item.css")
@CssImport("./styles/components/navi-drawer.css")
@CssImport("./styles/components/list-item.css")
@JsModule("@vaadin/vaadin-lumo-styles/badge")
@JsModule("./src/components/search-bar-combo.js")
public class MainLayout extends FlexBoxLayout implements RouterLayout, HasLogger {

	private static final String CLASS_NAME = "root";

	private final CurrentSession securityService;
	private final ServiceConfig 	serviceConfig;
	private final MessageService messageService;
	private final EventService		eventService;
	private final String            appName;
	private final String            appVersion;
	private final ServerProperties 	serverProperties;

	private Div 			appHeaderOuter;
	private FlexBoxLayout 	row;
	private NaviDrawer 		naviDrawer;
	private FlexBoxLayout 	column;

	private Div 			appHeaderInner;
	private FlexBoxLayout 	viewContainer;
	private Div 			appFooterInner;

	private Div 			appFooterOuter;

	private TabBar 			tabBar;
	private boolean 		navigationTabs = false;
	private AppBar 			appBar;

	private final			LocalDate systemDate;
	private final 			LogoutUtil logoutUtil;

	public MainLayout(CurrentSession securityService,
					  MessageService messageService,
					  EventService eventService,
					  CacheService cacheService,
					  ServiceConfig serviceConfig,
					  ServerProperties serverProperties) {
		VaadinSession.getCurrent()
					 .setErrorHandler((ErrorHandler) errorEvent -> {
						getLogger().error("Uncaught UI exception", errorEvent.getThrowable());
						Notification.show( "Una disculpa, ocurrió algún error interno en el sistema IAM-UI!!");
					});
		this.securityService = securityService;
		this.serviceConfig = serviceConfig;
		this.messageService = messageService;
		this.eventService = eventService;
		this.serverProperties = serverProperties;
		appName = serviceConfig.getAppName();       	// in properties file
		appVersion = serviceConfig.getAppVersion();
		systemDate = cacheService.getDay(0);		// Read system date from cache microservice
		if (systemDate == null)
			throw new RestClientException(500, "El servicio de consulta de la fecha del sistema esta fuera de línea");
		logoutUtil = new LogoutUtil(serverProperties);

		addClassName(CLASS_NAME);
		setFlexDirection(FlexDirection.COLUMN);
		setSizeFull();

		// Initialize the UI building blocks
		initStructure();
		// Populate the navigation drawer
		initNaviItems();
		// Configure the headers and footers (optional)
		initHeadersAndFooters();
	}

	@Override
	protected void onAttach(AttachEvent attachEvent) {
		super.onAttach(attachEvent);
	}

	/**
	 * Initialize the required components and containers.
	 */
	private void initStructure() {
		naviDrawer = new NaviDrawer();

		var company = securityService.getAuthenticatedUser().get().getCompany();

		naviDrawer.notificationView.add(new WebNotification(messageService), 
										new Span(company.length() > 12 ? company.substring(0,11) + "..." : company));
		naviDrawer.notificationView.setVisible(true);
		// ^ add the bell for subscribe and unsubscribe to web notifications

		viewContainer = new FlexBoxLayout();
		viewContainer.addClassName(CLASS_NAME + "__view-container");
		viewContainer.setOverflow(Overflow.HIDDEN);

		column = new FlexBoxLayout(viewContainer);
		column.addClassName(CLASS_NAME + "__column");
		column.setFlexDirection(FlexDirection.COLUMN);
		column.setFlexGrow(1, viewContainer);
		column.setOverflow(Overflow.HIDDEN);

		row = new FlexBoxLayout(naviDrawer, column);
		row.addClassName(CLASS_NAME + "__row");
		row.setFlexGrow(1, column);
		row.setOverflow(Overflow.HIDDEN);
		add(row);
		setFlexGrow(1, row);
	}

	/**
	 * Initialize the navigation items.
	 */
	private void initNaviItems() {
		NaviMenu menu = naviDrawer.getMenu();

		menu.addNaviItem(VaadinIcon.HOME, "Home", Home.class);

		NaviItem operations = menu.addNaviItem(VaadinIcon.OFFICE, "Operación", null);

		if (SecurityUtils.isAccessGranted(Operation.class))
			menu.addNaviItem(operations, "Grupo Cliente", Operation.class);
		if (SecurityUtils.isAccessGranted(Empleados.class))
			menu.addNaviItem(operations, "Asignación perfiles", Empleados.class);
		if (SecurityUtils.isAccessGranted(Areas.class))
			menu.addNaviItem(operations, "Asignación vendedores", Areas.class);
		if (SecurityUtils.isAccessGranted(SolicitudesAsignacion.class))
			menu.addNaviItem(operations, "Aut.de asignación", SolicitudesAsignacion.class);

		NaviItem permits = menu.addNaviItem(VaadinIcon.USER_STAR, "Adm.Facultades", null);

		if (SecurityUtils.isAccessGranted(Facultades.class))
			menu.addNaviItem(permits, "Facultades", Facultades.class);
		if (SecurityUtils.isAccessGranted(Roles.class))
			menu.addNaviItem(permits, "Roles", Roles.class);
		if (SecurityUtils.isAccessGranted(Perfiles.class))
			menu.addNaviItem(permits, "Perfiles", Perfiles.class);
		if (SecurityUtils.isAccessGranted(Vista.class))
			menu.addNaviItem(permits, "Vista perfiles", Vista.class);
	}

	/**
	 * Configure the app's inner and outer headers and footers.
	 */
	private void initHeadersAndFooters() {
		setAppHeaderOuter();
		setAppFooterInner();
		setAppFooterOuter();

		// Default inner header setup:
		// - When using tabbed navigation the view title, user avatar and main menu button will appear in the TabBar.
		// - When tabbed navigation is turned off they appear in the AppBar.

		appBar = new AppBar(serviceConfig, securityService, eventService, systemDate, logoutUtil, "");

		// Tabbed navigation
		if (navigationTabs) {
			tabBar = new TabBar(serviceConfig, securityService, eventService, systemDate, logoutUtil);
			UIUtils.setTheme(Lumo.DARK, tabBar);

			// Shift-click to add a new tab
			for (NaviItem item : naviDrawer.getMenu().getNaviItems()) {
				item.addClickListener(e -> {
					if (e.getButton() == 0 && e.isShiftKey())
						tabBar.setSelectedTab(tabBar.addClosableTab(item.getText(), item.getNavigationTarget()));
				});
			}
			appBar.getAvatar().setVisible(false);
			setAppHeaderInner(tabBar, appBar);
			// Default navigation
		} else {
			UIUtils.setTheme(Lumo.DARK, appBar);
			setAppHeaderInner(appBar);
		}
	}

	private void setAppHeaderOuter(Component... components) {
		if (appHeaderOuter == null) {
			appHeaderOuter = new Div();
			appHeaderOuter.addClassName("app-header-outer");
			getElement().insertChild(0, appHeaderOuter.getElement());
		}
		appHeaderOuter.removeAll();
		appHeaderOuter.add(components);
	}

	private void setAppHeaderInner(Component... components) {
		if (appHeaderInner == null) {
			appHeaderInner = new Div();
			appHeaderInner.addClassName("app-header-inner");
			column.getElement().insertChild(0, appHeaderInner.getElement());
		}
		appHeaderInner.removeAll();
		appHeaderInner.add(components);
	}

	private void setAppFooterInner(Component... components) {
		if (appFooterInner == null) {
			appFooterInner = new Div();
			appFooterInner.addClassName("app-footer-inner");
			column.getElement().insertChild(column.getElement().getChildCount(), appFooterInner.getElement());
		}
		appFooterInner.removeAll();
		appFooterInner.add(components);
	}

	private void setAppFooterOuter(Component... components) {
		if (appFooterOuter == null) {
			appFooterOuter = new Div();
			appFooterOuter.addClassName("app-footer-outer");
			getElement().insertChild(getElement().getChildCount(), appFooterOuter.getElement());
		}
		appFooterOuter.removeAll();
		appFooterOuter.add(components);
	}

	@Override
	public void showRouterLayoutContent(HasElement content) {
		this.viewContainer.getElement().appendChild(content.getElement());
	}

	public NaviDrawer getNaviDrawer() {
		return naviDrawer;
	}

	public static MainLayout get() {
		return (MainLayout) UI.getCurrent().getChildren()
										   .filter(component -> component.getClass() == MainLayout.class)
									       .findFirst()
									       .get();
	}
	public AppBar getAppBar() {
		return appBar;
	}

	@NotNull
	@Override
	public Logger getLogger() { return HasLogger.DefaultImpls.getLogger(this); }

}
