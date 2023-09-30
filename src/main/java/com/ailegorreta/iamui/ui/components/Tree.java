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
 *  Tree.kt
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.ailegorreta.iamui.ui.components;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.spring.annotation.SpringComponent;

/**
 *  Custom component to display Alchemys tree graphs
 *
 *  @author rlh
 *  @project : IAM-UI
 *  @date March 2022
 */
@Tag("tree")
@SpringComponent
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class Tree extends Composite<Div> {


    private static final String CLASS_NAME = "tree";
    private static Logger logger =  LoggerFactory.getLogger(Tree.class);

    private final Page page;

    public Tree() {
        page = UI.getCurrent().getPage();
        this.setId(CLASS_NAME);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        page.addJavaScript("/js/tree.js"); 	// retrieves from /src/main/resources/META-INF/resources/frontend/js/

        // retrieves D3 js library
        //page.addJavaScript("/js/d3/d3.js");

        page.addStyleSheet("/js/alchemyjs/dist/alchemy.css");
        // page.addStyleSheet("frontEnd://js/tree.css");
    }

    public void startTree(String data) {
        page.executeJs("other_data="+ data +";");
        page.executeJs("renderTree();");
    }

    public void updateTree(String data) {
        page.executeJs("other_data=" + data);
        page.executeJs("updateTree()");
    }

}
