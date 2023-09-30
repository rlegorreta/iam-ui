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
 *  WebNotification.kt
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.ailegorreta.iamui.ui.components;

import com.ailegorreta.iamui.backend.service.MessageService;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.littemplate.LitTemplate;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * This is a LitTemplate to subscribe or unsubscribe web browser notifications. The component
 * works with the Javascript web notification.js and as a back with the Message service class.
 *
 * For more information how to call Javascript files see: https://www.youtube.com/watch?v=nkmN5H1e3FE
 *
 * @project iam-ui
 * @author rlh
 * @Date: September 2022
 */
@Tag("web-notification")
@JsModule("./webnotification.ts")
public class WebNotification extends LitTemplate {
    private final MessageService messageService;

    public WebNotification(MessageService messageService) {
        this.messageService = messageService;
    }

    @Override
    public void onAttach(AttachEvent event) {
        super.onAttach(event);
        getElement().executeJs("this.setView($0)", this);
    }

    @ClientCallable
    public String getPublicKey() {
        return messageService.getPublicKey();
    }

    /**
     * When the user clicks the subscribe button from the Lit element button
     */
    @ClientCallable
    public void subscribe(String subscription) {
        messageService.subscribe(subscription, SecurityContextHolder.getContext().getAuthentication().getName());
    }

    /**
     * When the user clicks the unsubscribe button from the Lit element button
     */
    @ClientCallable
    public void unsubscribe(String endpoint) { messageService.unsubscribe(endpoint); }
}
