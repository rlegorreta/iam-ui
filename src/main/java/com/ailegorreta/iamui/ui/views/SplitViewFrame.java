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
 *  SplitViewFrame.java
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.ailegorreta.iamui.ui.views;

import com.ailegorreta.client.components.ui.FlexBoxLayout;
import com.ailegorreta.iamui.ui.MainLayout;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.FlexLayout;

/**
 * A view frame that establishes app design guidelines. It consists of four
 * parts:
 * <ul>
 * <li>Topmost {@link #setViewHeader(Component...) header}</li>
 * <li>Center {@link #setViewContent(Component...) content}</li>
 * <li>Center {@link #setViewDetails(Component...) details}</li>
 * <li>Bottom {@link #setViewFooter(Component...) footer}</li>
 * </ul>
 *
 * This is a more complicate solution as the one explained in the flow-crm-tutorial-example but but for
 * backward compatibility we kee it.
 *
 *  @author rlh
 *  @project : iam-ui
 *  @date September 2023
 */
@CssImport("./styles/components/view-frame.css")
public class SplitViewFrame extends Composite<Div> implements HasStyle {

	private String CLASS_NAME = "view-frame";

	private Div header;

	private FlexBoxLayout wrapper;
	private Div content;
	private Div details;

	private Div footer;

	public enum Position {
		RIGHT, BOTTOM
	}

	public SplitViewFrame() {
		setClassName(CLASS_NAME);

		header = new Div();
		header.setClassName(CLASS_NAME + "__header");

		wrapper = new FlexBoxLayout();
		wrapper.setClassName(CLASS_NAME + "__wrapper");

		content = new Div();
		content.setClassName(CLASS_NAME + "__content");

		details = new Div();
		details.setClassName(CLASS_NAME + "__details");

		footer = new Div();
		footer.setClassName(CLASS_NAME + "__footer");

		wrapper.add(content, details);
		getContent().add(header, wrapper, footer);
	}

	/**
	 * Sets the header slot's components.
	 */
	public void setViewHeader(Component... components) {
		header.removeAll();
		header.add(components);
	}

	/**
	 * Sets the content slot's components.
	 */
	public void setViewContent(Component... components) {
		content.removeAll();
		content.add(components);
	}

	/**
	 * Sets the detail slot's components.
	 */
	public void setViewDetails(Component... components) {
		details.removeAll();
		details.add(components);
	}

	public void setViewDetailsPosition(Position position) {
		if (position.equals(Position.RIGHT))
			wrapper.setFlexDirection(FlexLayout.FlexDirection.ROW);
		else if (position.equals(Position.BOTTOM))
			wrapper.setFlexDirection(FlexLayout.FlexDirection.COLUMN);
	}

	/**
	 * Sets the footer slot's components.
	 */
	public void setViewFooter(Component... components) {
		footer.removeAll();
		footer.add(components);
	}

	@Override
	protected void onAttach(AttachEvent attachEvent) {
		super.onAttach(attachEvent);
		MainLayout.get().getAppBar().reset();
	}

}
