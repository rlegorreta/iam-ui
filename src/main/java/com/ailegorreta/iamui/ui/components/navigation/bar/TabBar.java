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
 *  TabBar.kt
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.ailegorreta.iamui.ui.components.navigation.bar;

import com.ailegorreta.client.components.ui.FlexBoxLayout;
import com.ailegorreta.client.components.utils.LumoStyles;
import com.ailegorreta.client.components.utils.UIUtils;
import com.ailegorreta.client.navigation.navigation.tab.NaviTabs;
import com.ailegorreta.client.security.vaadin.LogoutUtil;
import com.ailegorreta.client.security.service.CurrentSession;
import com.ailegorreta.iamui.backend.data.service.EventService;
import com.ailegorreta.iamui.config.ServiceConfig;
import com.ailegorreta.iamui.ui.MainLayout;
import com.ailegorreta.iamui.ui.views.notification.Notifications;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.ailegorreta.iamui.ui.views.Home;

import java.time.LocalDate;

/**
 * This if the TabBar component for the structure defined in the
 * Business App Starter defined in Vaadin.
 *
 * @see //vaadin.com/docs/business-app/overview.html
 *
 * @author Vaadin
 * @project iam-ui
 * @date september 2023
 */
@CssImport("./styles/components/tab-bar.css")
public class TabBar extends FlexBoxLayout {
	public static final String TITLE_LOGOUT = "Salida de ";
	public static final String IMG_PATH = "images/";

	private String CLASS_NAME = "tab-bar";

	private Button 		menuIcon;
	private NaviTabs 	tabs;
	private Button 		addTab;
	private Paragraph   systemDateLabel;
	private Image 		avatar;
	private Dialog		about;
	private final ServiceConfig 	serviceConfig;
	private final EventService		eventService;
	private final CurrentSession 	securityService;
	private final LocalDate systemDate;
	// ^ TODO required the system date (dependening of which system) in what date it is going to operate.
	//        TODO develop es systemdate miro.service that answer this question
	private final LogoutUtil logoutUtil;

	public TabBar(ServiceConfig 	serviceConfig,
				  CurrentSession 	securityService,
				  EventService 		eventService,
				  LocalDate			systemDate,
				  LogoutUtil 		logoutUtil) {
		this.serviceConfig = serviceConfig;
		this.securityService = securityService;
		this.eventService = eventService;
		this.systemDate = systemDate;
		this.logoutUtil = logoutUtil;

		setClassName(CLASS_NAME);

		menuIcon = UIUtils.createTertiaryInlineButton(VaadinIcon.MENU);
		menuIcon.addClassName(CLASS_NAME + "__navi-icon");
		menuIcon.addClickListener(e -> MainLayout.get().getNaviDrawer().toggle());

		systemDateLabel = new Paragraph(systemDate.toString());
		avatar.setClassName(CLASS_NAME + "__systemDate");

		avatar = new Image();
		avatar.setClassName(CLASS_NAME + "__avatar");
		avatar.setSrc(IMG_PATH + "avatar.png");

		ContextMenu contextMenu = new ContextMenu(avatar);
		
		contextMenu.setOpenOnClick(true);

		about = new Dialog();
		VerticalLayout content = new VerticalLayout(
				new HorizontalLayout(new Image(IMG_PATH + "logos/Logo.png", ""),
						new Paragraph("Aplicación:'" + serviceConfig.getAppName() + "'")),
						new Paragraph("Build version " + serviceConfig.getAppVersion() ),
						new Paragraph("Copyright (c) 2022, " + securityService.getAuthenticatedUser().get().getCompany()));

		about.add(content);
		about.setWidth("420px");
		about.setHeight("200px");

		contextMenu.addItem("Notificaciones...",  e -> createNotifications().open());
		contextMenu.addItem("Preferencias",
				e -> Notification.show("Por implementar...", 3000,
						Notification.Position.BOTTOM_CENTER));
		contextMenu.addItem("Acerca de...", e -> about.open());

		createLogout(contextMenu);

		addTab = UIUtils.createSmallButton(VaadinIcon.PLUS);
		addTab.addClickListener(e -> tabs
				.setSelectedTab(addClosableTab("New Tab", Home.class)));
		addTab.setClassName(CLASS_NAME + "__add-tab");

		tabs = new NaviTabs(Home.class);
		tabs.setClassName(CLASS_NAME + "__tabs");

		add(menuIcon, tabs, addTab, systemDateLabel, avatar);
	}

	private Dialog createNotifications() {
		var notifications = new Dialog();

		notifications.add(new Notifications(eventService));
		notifications.setWidth("900px");
		notifications.setHeight("560px");

		return notifications;
	}
	private void createLogout(ContextMenu contextMenu) {
		contextMenu.addItem(UIUtils.createLargeButton(TITLE_LOGOUT + serviceConfig.getAppName(), VaadinIcon.ARROW_RIGHT),
				e -> {
					logoutUtil.logout();
					UI.getCurrent().getPage().executeJs("window.location.href='logout'");
				});
	}

	/* === MENU ICON === */

	public Button getMenuIcon() {
		return menuIcon;
	}

	/* === TABS === */

	public void centerTabs() {
		tabs.addClassName(LumoStyles.Margin.Horizontal.AUTO);
	}

	private void configureTab(Tab tab) {
		tab.addClassName(CLASS_NAME + "__tab");
	}

	public Tab addTab(String text) {
		Tab tab = tabs.addTab(text);
		
		configureTab(tab);
		
		return tab;
	}

	public Tab addTab(String text, Class<? extends Component> navigationTarget) {
		Tab tab = tabs.addTab(text, navigationTarget);
		
		configureTab(tab);
		
		return tab;
	}

	public Tab addClosableTab(String text, Class<? extends Component> navigationTarget) {
		Tab tab = tabs.addClosableTab(text, navigationTarget);
		
		configureTab(tab);
		
		return tab;
	}

	public Tab getSelectedTab() {
		return tabs.getSelectedTab();
	}

	public void setSelectedTab(Tab selectedTab) {
		tabs.setSelectedTab(selectedTab);
	}

	public void updateSelectedTab(String text, Class<? extends Component> navigationTarget) {
		tabs.updateSelectedTab(text, navigationTarget);
	}

	public void addTabSelectionListener(ComponentEventListener<Tabs.SelectedChangeEvent> listener) {
		tabs.addSelectedChangeListener(listener);
	}

	public int getTabCount() {
		return tabs.getTabCount();
	}

	public void removeAllTabs() {
		tabs.removeAll();
	}

	/* === ADD TAB BUTTON === */

	public void setAddTabVisible(boolean visible) {
		addTab.setVisible(visible);
	}
}
