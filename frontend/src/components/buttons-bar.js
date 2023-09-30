import { PolymerElement } from '@polymer/polymer/polymer-element.js';
import { html } from '@polymer/polymer/lib/utils/html-tag.js';
{
  class ButtonsBarElement extends PolymerElement {
    static get template() {
      return html`
    
    <slot name="left"></slot>
    <slot name="info"><div class="info"></div></slot>
    <slot name="right"></slot>
`;
    }

    static get is() {
      return 'buttons-bar';
    }
  }

  window.customElements.define(ButtonsBarElement.is, ButtonsBarElement);
}
