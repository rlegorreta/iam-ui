/* Copyright (c) 2023, LMASSDesarrolladores, S.C.
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
 *  WebNotification.js
 *
 *  Developed 2023 by LMASS Desarrolladores, S.C. www.lmass.com.mx
 */
import {LitElement, html} from 'lit';
import {state} from 'lit/decorators.js';
import '@vaadin/tabs';
import '@vaadin/button/vaadin-button.js';
import '@vaadin/vaadin-lumo-styles/typography';
import '@vaadin/icons';
import '@vaadin/icon';

/**
 * Javascript LitElement to subscribe and unsubscribe web browser notifications.
 *
 * For more information how to call Javascript files see: https://www.youtube.com/watch?v=nkmN5H1e3FE
 *
 * @Date: December, 2021
 */
class WebNotification extends LitElement {

    render() {
        return html`
            <div>
                ${this.denied
            ? html`
                        <vaadin-icon class="me-l text-l" icon="vaadin:bell-slash-o" title="Notificaciones bloqueada por el browser"></vaadin-icon>
                        ` : ""}
                ${this.subscribed
            ? html`
                        <vaadin-button id="activeNotificationButton" theme="icon primary" icon="vaadin:bell" title="Notificaciones activa" @click="${this.unsubscribe}">
                            <vaadin-icon icon="vaadin:bell" slot="prefix"></vaadin-icon>
                        </vaadin-button>
                        `
            : html`
                        <vaadin-button id="suspendNotificationButton" theme="icon secondary error" title="Notificación no activa" @click="${this.subscribe}">
                            <vaadin-icon icon="vaadin:bell-slash" slot="prefix"></vaadin-icon>
                        </vaadin-button>
                        `}
            </div>`;
    }

    @state()
    denied= Notification.permission === "denied";
    @state()
    subscribed = false;
    view:any | undefined = undefined

    async firstUpdated() {
        try {
            if ('serviceWorker' in navigator) {
                const registration = await navigator.serviceWorker.getRegistration();
                this.subscribed = !!(await registration?.pushManager.getSubscription());
            } else {
                console.log("El navigator carece de serviceWorker secure context:", window.isSecureContext);
                console.log(navigator)
            }
        } catch (e) {
            console.log("Error al tratar de leer el registro de notificaciones " + e);
        }
    }

    async subscribe() {
        const publicKey = await this.view.$server.getPublicKey();
        const notificationPermission = await Notification.requestPermission();

        if (notificationPermission === "granted") {
            const registration = await navigator.serviceWorker.getRegistration();
            const subscription = await registration?.pushManager.subscribe({
                userVisibleOnly: true,
                applicationServerKey: this.urlB64ToUint8Array((<string>publicKey)),
            });

            if (subscription) {
                this.subscribed = true;
                // Serialize keys uint8array -> base64
                this.view.$server.subscribe(JSON.stringify(JSON.parse(JSON.stringify(subscription))));
            }
        } else {
            console.log("notification permission in NOT granted:" + notificationPermission);
            this.denied = true;
        }
    }

    async unsubscribe() {
        const registration = await navigator.serviceWorker.getRegistration();
        const subscription = await registration?.pushManager.getSubscription();
        if (subscription) {
            await subscription.unsubscribe();
            await this.view.$server.unsubscribe(subscription.endpoint);
            this.subscribed = false;
        }
    }

    private urlB64ToUint8Array(base64String: string) {
        const padding = "=".repeat((4 - (base64String.length % 4)) % 4);
        const base64 = (base64String + padding)
            .replace(/\-/g, "+")
            .replace(/_/g, "/");
        const rawData = window.atob(base64);
        const outputArray = new Uint8Array(rawData.length);
        for (let i = 0; i < rawData.length; ++i) {
            outputArray[i] = rawData.charCodeAt(i);
        }
        return outputArray;
    }

    setView(view: any) {
        this.view = view;
    }

}

customElements.define('web-notification', WebNotification);
