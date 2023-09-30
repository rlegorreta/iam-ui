import { html, LitElement } from 'lit';
import '@vaadin/icons/vaadin-icons.js';
import '@vaadin/text-field/src/vaadin-text-field.js';
import '@vaadin/button/src/vaadin-button.js';
import '@vaadin/form-layout/src/vaadin-form-layout.js';
import '@vaadin/form-layout/src/vaadin-form-item.js';
import '@vaadin/combo-box/src/vaadin-combo-box.js';
import '@vaadin/date-picker/src/vaadin-date-picker.js';
import '../../src/components/buttons-bar.js';
// import '../../src/components/utils-mixin.js';
// import '../../styles/shared-styles.js';

class RolEditor extends LitElement {

  render() {
    return html`
    <style include="shared-styles">
      :host {
        display: flex;
        flex-direction: column;
        flex: auto;
      }

      .meta-row {
        display: flex;
        justify-content: space-between;
        padding-bottom: var(--lumo-space-s);
      }

      .dim {
        color: var(--lumo-secondary-text-color);
        text-align: right;
        white-space: nowrap;
        line-height: 2.5em;
      }

      .status {
        width: 10em;
      }
    </style>

    <div class="scrollable flex1" id="main">
    
      <div class="meta-row" id="metaContainer">
        <span class="dim">Rol id:<span id="idNeo4j"></span></span>
      </div>

      <vaadin-form-layout id="form1">

        <vaadin-form-layout id="form2">
          <vaadin-text-field label="Id Rol:" id="idRol">
          </vaadin-text-field>
          <vaadin-text-field label="nombre" id="nombre">
            <vaadin-icon slot="prefix" icon="vaadin:user"></vaadin-icon>
          </vaadin-text-field>
        </vaadin-form-layout>

        <vaadin-form-layout id="form3" colspan="3">
          <vaadin-radio-group id="activo" label="" colspan="2">
          </vaadin-radio-group>

          <span class="dim">Fecha modificación:<span id="fechaModificacion"></span></span>
        </vaadin-form-layout>      

	    <buttons-bar id="footer" no-scroll\$="[[noScroll]]">
	      <vaadin-button slot="left" id="save" theme="primary">
	      		Guardar Rol
	      		<vaadin-icon icon="vaadin:arrow-right" slot="suffix"></vaadin-icon>
	      </vaadin-button>
	      <vaadin-button slot="right" id="cancel">Cancelar</vaadin-button>
	    </buttons-bar>
    </div>
    
	<div class="scrollable flex1" id="main">
		<vaadin-form-layout id="form4" colspan="3">
    		<search-bar id="searchFacultades" show-checkbox=""></search-bar>
    		<vaadin-grid id="gridFacultades" theme="orders no-row-borders"></vaadin-grid>
        </vaadin-form-layout>
    </div>
`;
  }

  static get is() {
    return 'rol-editor';
  }

  static get properties() {
    return {
      status: {
        type: String,
        observer: '_onStatusChange'
      }
    };
  }

  ready() {
    super.ready();

    // Not using attributes since Designer does not support single-quote attributes
    this.$.form1.responsiveSteps = [
      {columns: 1, labelsPosition: 'top'},
      {minWidth: '600px', columns: 4, labelsPosition: 'top'}
    ];
    this.$.form2.responsiveSteps = [
      {columns: 1, labelsPosition: 'top'},
      {minWidth: '360px', columns: 2, labelsPosition: 'top'}
    ];
    this.$.form3.responsiveSteps = [
      {columns: 1, labelsPosition: 'top'},
      {minWidth: '500px', columns: 3, labelsPosition: 'top'}
    ];
    this.$.form4.responsiveSteps = [
        {columns: 1, labelsPosition: 'top'},
        {minWidth: '500px', columns: 2, labelsPosition: 'top'}
	];
  }

  _onStatusChange() {
    const status = this.status ? this.status.toLowerCase() : this.status;
    // In case we have a status  ag but for this simple case
    // we do not need to do anything this.$.status.$.input.setAttribute('status', status);
  }
}

customElements.define(RolEditor.is, RolEditor);
