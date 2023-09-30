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
 *  AppBar.kt
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.ailegorreta.iamui.ui.components.navigation.bar;

import java.time.LocalDate;
import java.util.ArrayList;

import com.ailegorreta.client.components.ui.FlexBoxLayout;
import com.ailegorreta.client.components.utils.LumoStyles;
import com.ailegorreta.client.components.utils.UIUtils;
import com.ailegorreta.client.navigation.navigation.tab.NaviTab;
import com.ailegorreta.client.navigation.navigation.tab.NaviTabs;
import com.ailegorreta.client.security.vaadin.LogoutUtil;
import com.ailegorreta.client.security.service.CurrentSession;
import com.ailegorreta.iamui.backend.data.service.EventService;
import com.ailegorreta.iamui.config.ServiceConfig;
import com.ailegorreta.iamui.ui.MainLayout;
import com.ailegorreta.iamui.ui.views.notification.Notifications;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.shared.Registration;
import com.ailegorreta.iamui.ui.views.Home;

/**
 * This if the AppBar component for the structure defined in the
 * Business App Starter defined in Vaadin.
 *
 * AppBar consists of a main menu icon, contextual navigation icon, title, tabs, action items and avatar.
 * All building blocks are optional.
 *
 * @see //vaadin.com/docs/business-app/overview.html
 *
 * @author Vaadin
 * @project iam-ui
 * @date September 2023
 */
@CssImport("./styles/components/app-bar.css")
public class AppBar extends FlexBoxLayout {
	private static final String TITLE_LOGOUT = "Salida de ";
	private static final String IMG_PATH = "images/";

	private String CLASS_NAME = "app-bar";

	private FlexBoxLayout 	container;

	private Button 			menuIcon;
	private Button 			contextIcon;

	private H4 				title;
	private FlexBoxLayout 	actionItems;
	private Paragraph		systemDateLabel;
	private Image 			avatar;
	private Dialog			about;

	private FlexBoxLayout 	tabContainer;
	private NaviTabs 		tabs;
	private ArrayList<Registration> tabSelectionListeners;
	private Button 			addTab;

	private TextField 		search;
	private Registration 	searchRegistration;

	private final ServiceConfig 	serviceConfig;
	private final EventService		eventService;
	private final CurrentSession 	securityService;
	private final LocalDate			systemDate;
	private final LogoutUtil		logoutUtil;

	public enum NaviMode {
		MENU, CONTEXTUAL
	}

	public AppBar(ServiceConfig 	serviceConfig,
				  CurrentSession 	securityService,
			  	  EventService		eventService,
				  LocalDate			systemDate,
				  LogoutUtil 		logoutUtil,
				  String title,
				  NaviTab... tabs) {
		this.serviceConfig = serviceConfig;
		this.securityService = securityService;
		this.eventService = eventService;
		this.systemDate = systemDate;
		this.logoutUtil = logoutUtil;
		setClassName(CLASS_NAME);

		initMenuIcon();
		initContextIcon();
		initTitle(title);
		initSearch();
		initSystemDate();
		initAvatar();
		initActionItems();
		initContainer();
		initTabs(tabs);
	}

	public void setNaviMode(NaviMode mode) {
		if (mode.equals(NaviMode.MENU)) {
			menuIcon.setVisible(true);
			contextIcon.setVisible(false);
		} else {
			menuIcon.setVisible(false);
			contextIcon.setVisible(true);
		}
	}

	private void initMenuIcon() {
		menuIcon = UIUtils.createTertiaryInlineButton(VaadinIcon.MENU);
		menuIcon.addClassName(CLASS_NAME + "__navi-icon");
		menuIcon.addClickListener(e -> MainLayout.get().getNaviDrawer().toggle());
		UIUtils.setAriaLabel("Menu", menuIcon);
		UIUtils.setLineHeight("1", menuIcon);
	}

	private void initContextIcon() {
		contextIcon = UIUtils.createTertiaryInlineButton(VaadinIcon.ARROW_LEFT);
		contextIcon.addClassNames(CLASS_NAME + "__context-icon");
		contextIcon.setVisible(false);
		UIUtils.setAriaLabel("Back", contextIcon);
		UIUtils.setLineHeight("1", contextIcon);
	}

	private void initTitle(String title) {
		this.title = new H4(title);
		this.title.setClassName(CLASS_NAME + "__title");
	}

	private void initSearch() {
		search = new TextField();
		search.setPlaceholder("Buscar");
		search.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
		search.setVisible(false);
	}

	private void initSystemDate() {
		systemDateLabel = new Paragraph(systemDate.toString());
		systemDateLabel.setClassName(CLASS_NAME + "__systemDate");
	}
	private void initAvatar() {
		avatar = new Image();
		avatar.setClassName(CLASS_NAME + "__avatar");
		avatar.setSrc(IMG_PATH + "avatar.jpeg");
		avatar.setAlt("User menu");
		about = new Dialog();

		VerticalLayout content = new VerticalLayout(
											new HorizontalLayout(new Image(IMG_PATH + "logos/Logo.png", ""),
				                                        		 new Paragraph("Aplicación:'" + serviceConfig.getAppName() + "'")),
											new Paragraph("Build version " + serviceConfig.getAppVersion()),
											new Paragraph("Copyright (c) 2022, " + securityService.getAuthenticatedUser().get().getCompany()));

		about.add(content);
		about.setWidth("420px");
		about.setHeight("240px");

		ContextMenu contextMenu = new ContextMenu(avatar);
		
		contextMenu.setOpenOnClick(true);
		contextMenu.addItem("Notificaciones...",  e -> createNotifications().open());
		contextMenu.addItem("Preferencias",
				e -> Notification.show("Por implementar...", 3000,
										Notification.Position.BOTTOM_CENTER));
		contextMenu.addItem("Acerca de...", e -> about.open());

		createLogout(contextMenu);
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

	private void initActionItems() {
		actionItems = new FlexBoxLayout();
		actionItems.addClassName(CLASS_NAME + "__action-items");
		actionItems.setVisible(false);
	}

	private void initContainer() {
		container = new FlexBoxLayout(menuIcon, contextIcon, this.title, search, actionItems, systemDateLabel, avatar);
		container.addClassName(CLASS_NAME + "__container");
		container.setAlignItems(FlexComponent.Alignment.CENTER);
		container.setFlexGrow(1, search);
		add(container);
	}

	private void initTabs(NaviTab... tabs) {
		addTab = UIUtils.createSmallButton(VaadinIcon.PLUS);
		addTab.addClickListener(e -> this.tabs
				.setSelectedTab(addClosableNaviTab("New Tab", Home.class)));
		addTab.setVisible(false);

		this.tabs = tabs.length > 0 ? new NaviTabs(Home.class, tabs) : new NaviTabs(Home.class);
		this.tabs.setClassName(CLASS_NAME + "__tabs");
		this.tabs.setVisible(false);
		for (NaviTab tab : tabs) 
			configureTab(tab);

		this.tabSelectionListeners = new ArrayList<>();

		tabContainer = new FlexBoxLayout(this.tabs, addTab);
		tabContainer.addClassName(CLASS_NAME + "__tab-container");
		tabContainer.setAlignItems(FlexComponent.Alignment.CENTER);
		add(tabContainer);
	}

	/* === MENU ICON === */

	public Button getMenuIcon() {
		return menuIcon;
	}

	/* === CONTEXT ICON === */

	public Button getContextIcon() {
		return contextIcon;
	}

	public void setContextIcon(Icon icon) {
		contextIcon.setIcon(icon);
	}

	/* === TITLE === */

	public String getTitle() {
		return this.title.getText();
	}

	public void setTitle(String title) {
		this.title.setText(title);
	}

	/* === ACTION ITEMS === */

	public Component addActionItem(Component component) {
		actionItems.add(component);
		updateActionItemsVisibility();
		return component;
	}

	public Button addActionItem(VaadinIcon icon) {
		Button button = UIUtils.createButton(icon, ButtonVariant.LUMO_SMALL,
				ButtonVariant.LUMO_TERTIARY);
		addActionItem(button);
		return button;
	}

	public void removeAllActionItems() {
		actionItems.removeAll();
		updateActionItemsVisibility();
	}

	/* === AVATAR == */

	public Image getAvatar() {
		return avatar;
	}

	/* === TABS === */

	public void centerTabs() {
		tabs.addClassName(LumoStyles.Margin.Horizontal.AUTO);
	}

	private void configureTab(Tab tab) {
		tab.addClassName(CLASS_NAME + "__tab");
		updateTabsVisibility();
	}

	public Tab addTab(String text) {
		Tab tab = tabs.addTab(text);
		configureTab(tab);
		return tab;
	}

	public Tab addTab(String text,
	                  Class<? extends Component> navigationTarget) {
		Tab tab = tabs.addTab(text, navigationTarget);
		configureTab(tab);
		return tab;
	}

	public Tab addClosableNaviTab(String text, Class<? extends Component> navigationTarget) {
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

	public void navigateToSelectedTab() {
		tabs.navigateToSelectedTab();
	}

	public void addTabSelectionListener(ComponentEventListener<Tabs.SelectedChangeEvent> listener) {
		Registration registration = tabs.addSelectedChangeListener(listener);
		
		tabSelectionListeners.add(registration);
	}

	public int getTabCount() {
		return tabs.getTabCount();
	}

	public void removeAllTabs() {
		tabSelectionListeners.forEach(registration -> registration.remove());
		tabSelectionListeners.clear();
		tabs.removeAll();
		updateTabsVisibility();
	}

	/* === ADD TAB BUTTON === */

	public void setAddTabVisible(boolean visible) {
		addTab.setVisible(visible);
	}

	/* === SEARCH === */

	public void searchModeOn() {
		menuIcon.setVisible(false);
		title.setVisible(false);
		actionItems.setVisible(false);
		tabContainer.setVisible(false);

		contextIcon.setIcon(new Icon(VaadinIcon.ARROW_BACKWARD));
		contextIcon.setVisible(true);
		searchRegistration = contextIcon.addClickListener(e -> searchModeOff());

		search.setVisible(true);
		search.focus();
	}

	public void addSearchListener(HasValue.ValueChangeListener listener) {
		search.addValueChangeListener(listener);
	}

	public void setSearchPlaceholder(String placeholder) {
		search.setPlaceholder(placeholder);
	}

	private void searchModeOff() {
		menuIcon.setVisible(true);
		title.setVisible(true);
		tabContainer.setVisible(true);

		updateActionItemsVisibility();
		updateTabsVisibility();

		contextIcon.setVisible(false);
		searchRegistration.remove();

		search.clear();
		search.setVisible(false);
	}

	/* === RESET === */

	public void reset() {
		title.setText("");
		setNaviMode(AppBar.NaviMode.MENU);
		removeAllActionItems();
		removeAllTabs();
	}

	/* === UPDATE VISIBILITY === */

	private void updateActionItemsVisibility() {
		actionItems.setVisible(actionItems.getComponentCount() > 0);
	}

	private void updateTabsVisibility() {
		tabs.setVisible(tabs.getComponentCount() > 0);
	}
}
