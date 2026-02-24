// ============================================================================
// ng-js.js — Client-side Ajax support for the ng-objects framework
// ============================================================================

// --- Debounce utility -------------------------------------------------------

const _ngDebounceTimers = new Map();

/**
 * Delays invocation of [fn] by [delay] ms, resetting the timer if called again with the same [key].
 */
function ngDebounce( key, fn, delay ) {
	if( _ngDebounceTimers.has(key) ) {
		clearTimeout( _ngDebounceTimers.get(key) );
	}
	_ngDebounceTimers.set( key, setTimeout( () => {
		_ngDebounceTimers.delete(key);
		fn();
	}, delay ));
}

// --- Focus restoration ------------------------------------------------------

let _ngFocusState = null;

/**
 * Captures the currently focused element's id and cursor position.
 * Called before Ajax requests that will re-render form fields.
 */
function ngCaptureFocus() {
	const el = document.activeElement;
	if( el && el.id && (el.tagName === 'INPUT' || el.tagName === 'TEXTAREA' || el.tagName === 'SELECT') ) {
		_ngFocusState = {
			id: el.id,
			selectionStart: el.selectionStart,
			selectionEnd: el.selectionEnd
		};
	}
	else {
		_ngFocusState = null;
	}
}

/**
 * Restores focus and cursor position to the previously captured element.
 * Called after a container's content has been swapped.
 */
function ngRestoreFocus() {
	if( _ngFocusState ) {
		const el = document.getElementById( _ngFocusState.id );
		if( el ) {
			el.focus();
			if( el.setSelectionRange && _ngFocusState.selectionStart != null ) {
				try {
					el.setSelectionRange( _ngFocusState.selectionStart, _ngFocusState.selectionEnd );
				}
				catch(e) {
					// Some input types (number, email) don't support setSelectionRange
				}
			}
		}
		_ngFocusState = null;
	}
}

// --- Container content update -----------------------------------------------

/**
 * Update the given UpdateContainer with the given HTML content
 */
function setUpdateContainerContent( updateContainerID, content ) {

	const updateContainer = document.getElementById(updateContainerID);

	if( !updateContainer ) {
		console.error( 'No UpdateContainer found on page with id ' + updateContainerID );
		return;
	}

	updateContainer.innerHTML = content;

	// Fire custom event for post-swap initialization (e.g. re-binding observe fields, tooltips, etc.)
	updateContainer.dispatchEvent( new CustomEvent( 'ng-content-updated', { bubbles: true } ) );
}

// Focus restoration is handled in ajaxRequest() after all containers have been updated.

// --- Core Ajax request ------------------------------------------------------

/**
 * Submit an ajax request and update the given updateContainerID with the response.
 *
 * FIXME: Allow form submission without a specified updateContainer? // Hugi 2025-04-14
 */
function ajaxRequest( url, fetchOptions, updateContainerID ) {

	// Add loading class to all targeted containers before the request
	const containerIDs = updateContainerID ? updateContainerID.split(';') : [];
	containerIDs.forEach( id => {
		const el = document.getElementById(id);
		if( el ) el.classList.add('ng-loading');
	});

	fetch( url, fetchOptions )
	.then(
		response => {
			if( !response.ok ) {
				// Server returned an error (e.g. 500). Read the response body so we can
				// display the server's error page (or message) in an overlay.
				return response.text().then( errorBody => {
					throw { status: response.status, body: errorBody };
				});
			}

			// Determine if it's a multipart response (multiple containers) or regular (single container)
			const contentType = response.headers.get('content-type');

			if( contentType && contentType.startsWith('multipart/form-data') ) {
				return response.formData();
			}
			else {
				return response.text();
			}
		}
	)
	.then(
		responseData => {
			if( responseData instanceof FormData ) {
				for( const pair of responseData.entries() ) {
					console.debug( '----- [' + pair[0] + '] -------------\n' + pair[1] );
					setUpdateContainerContent( pair[0], pair[1] );
				}
			}
			else {
				console.debug( '----- [' + updateContainerID + '] -------------\n' + responseData );
				setUpdateContainerContent( updateContainerID, responseData );
			}

			// Restore focus after ALL containers have been updated.
			// The setTimeout lets the browser finish layout and lets the MutationObserver
			// re-bind observe fields before we attempt to focus.
			setTimeout( ngRestoreFocus, 0 );
		}
	)
	.catch(
		error => {
			console.error( 'Ajax request failed:', error );

			if( error.body ) {
				// Server returned an error response with a body (e.g. an exception page).
				// Display it in an overlay so the developer can see the full error.
				_ngShowErrorOverlay( error.body );
			}
			else {
				// Network error or other failure — show inline error in the targeted containers
				const message = error.message || String(error);
				containerIDs.forEach( id => {
					const el = document.getElementById(id);
					if( el ) {
						el.innerHTML = '<div class="ng-error">Villa: ' + message + '</div>';
					}
				});
			}
		}
	)
	.finally(
		() => {
			containerIDs.forEach( id => {
				const el = document.getElementById(id);
				if( el ) el.classList.remove('ng-loading');
			});
		}
	);
}

// --- Error overlay ----------------------------------------------------------

/**
 * Displays the server's error response (e.g. an exception page) in a dismissable overlay.
 * Click anywhere on the backdrop to close it.
 */
function _ngShowErrorOverlay( html ) {
	// Remove any existing overlay
	const existing = document.getElementById('ng-error-overlay');
	if( existing ) existing.remove();

	const overlay = document.createElement('div');
	overlay.id = 'ng-error-overlay';
	overlay.style.cssText = 'position:fixed;inset:0;z-index:99999;background:rgba(0,0,0,0.6);display:flex;align-items:center;justify-content:center;cursor:pointer;';

	const frame = document.createElement('iframe');
	frame.style.cssText = 'width:90vw;height:85vh;border:3px solid #dc3545;border-radius:8px;background:#fff;box-shadow:0 8px 32px rgba(0,0,0,0.3);';
	frame.srcdoc = html;

	overlay.appendChild( frame );
	overlay.addEventListener( 'click', function(e) {
		if( e.target === overlay ) overlay.remove();
	});

	document.body.appendChild( overlay );
}

// --- AjaxUpdateLink ---------------------------------------------------------

/**
 * Invoked by AjaxUpdateLink.
 * Initiates a GET request to the given URL and updates the container identified by 'id' with the returned response content.
 *
 * FIXME: Currently, null can be passed in as an id parameter. Invoking an action without a resulting update feels like such a special situation (WRT caching, rendering and other handling on the server side) that this might warrant a separate function // Hugi 2024-10-05
 */
function ajaxUpdateLinkClick( url, updateContainerID ) {

	const fetchOptions =
		{
			method: "GET",
			headers: {
				'ng-container-id': updateContainerID
			}
		};

	ajaxRequest( url, fetchOptions, updateContainerID );
}

// --- AjaxSubmitButton -------------------------------------------------------

/**
 * Invoked on the click of an AjaxSubmitButton.
 *
 * @param button The button clicked to submit the form
 * @param updateContainerID The ID of an update container to display the returned HTML
 */
function ajaxSubmitButtonClick( button, updateContainerID ) {

	// The form we are submitting is always the clicked button's containing form
	const form = button.form;

	// The URL we'll be targeting is the form's action URL.
	const url = form.action;

	// Get the values from the form to submit and wrap them in URLSearchParams for x-www-form-urlencoded
	const params = new URLSearchParams(new FormData(form));

	// Since JS won't add the name of the clicked button to the request's parameters for us
	// (as is usually done by a regular form submit using an input type="submit"),
	// we add the button's name to the submitted values ourselves.
	params.append(button.name,button.value);

	const fetchOptions =
		{
			method: form.method,
			body: params,
			headers: {
				'Content-Type': 'application/x-www-form-urlencoded',
				'ng-container-id': updateContainerID
			}
		};

	ajaxRequest( url, fetchOptions, updateContainerID );
}

// --- AjaxObserveField -------------------------------------------------------

/**
 * Keeps track of info for all observed fields in the current page
 */
const _ngFieldObservers = new Map();

/**
 * Triggered when an observed field's value changes.
 * Sends either just the observed field's value (default) or the full containing form (fullSubmit mode)
 * to the server, and delegates to ajaxRequest() for multipart handling, loading states, etc.
 */
function _ngObserveFieldTriggered( url, observeFieldID, updateContainerID, fullSubmit ) {
	const field = document.getElementById(observeFieldID);

	// Capture focus before the request so we can restore it after the container re-renders
	ngCaptureFocus();

	let params;

	if( fullSubmit ) {
		// Submit all fields from the containing form
		const form = field.closest('form');
		if( form ) {
			params = new URLSearchParams( new FormData(form) );
		}
		else {
			params = new URLSearchParams();
			params.append( field.name, field.value );
		}
	}
	else {
		// Submit only the observed field's value
		params = new URLSearchParams();
		params.append( field.name, field.value );
	}

	const fetchOptions = {
		method: "POST",
		body: params,
		headers: {
			'Content-Type': 'application/x-www-form-urlencoded',
			'ng-container-id': updateContainerID
		}
	};

	ajaxRequest( url, fetchOptions, updateContainerID );
}

/**
 * Scans the DOM for AjaxObserveField data-attribute markers and binds input event listeners.
 * Called on DOMContentLoaded and after any DOM mutation (via MutationObserver).
 */
function _ngBindObserveFields() {
	const elements = document.querySelectorAll('[data-observefieldid]');

	const func = function(evt) {
		const record = _ngFieldObservers.get(this.id);
		ngDebounce( record.observeFieldID, () => {
			_ngObserveFieldTriggered( record.action, record.observeFieldID, record.updateContainerID, record.fullSubmit );
		}, 300 );
	};

	for( const element of elements ) {
		const observeFieldID = element.getAttribute('data-observefieldid');
		const field = document.getElementById(observeFieldID);

		if( !field ) {
			continue;
		}

		// Remove previous listener if re-binding after a DOM swap
		if( _ngFieldObservers.has( observeFieldID ) ) {
			const existingFunction = _ngFieldObservers.get(observeFieldID).func;
			field.removeEventListener('input', existingFunction );
			_ngFieldObservers.delete( observeFieldID );
		}

		if( !_ngFieldObservers.get( observeFieldID ) ) {
			const record = {};
			record.observeFieldID = observeFieldID;
			record.updateContainerID = element.getAttribute('data-updatecontainerid');
			record.action = element.getAttribute('data-action');
			record.fullSubmit = element.getAttribute('data-fullsubmit') === 'true';
			record.func = func;
			_ngFieldObservers.set( record.observeFieldID, record );

			field.addEventListener('input', record.func );
		}
	}
}

/**
 * Scans the DOM for AjaxObserveField container-mode markers (data-observedescendants)
 * and binds input event listeners to all descendant input/textarea/select elements.
 * Called on DOMContentLoaded and after any DOM mutation (via MutationObserver).
 */
function _ngBindObserveDescendants() {
	const containers = document.querySelectorAll('[data-observedescendants]');

	for( const container of containers ) {
		const updateContainerID = container.getAttribute('data-updatecontainerid');
		const action = container.getAttribute('data-action');
		const fullSubmit = container.getAttribute('data-fullsubmit') === 'true';

		const fields = container.querySelectorAll('input, textarea, select');

		for( const field of fields ) {
			// We need an id to look up the field later. If the field only has a name
			// (which is the case for ng-objects text fields that don't bind an explicit id),
			// promote the name to the id. The framework's element IDs are unique within the page.
			if( !field.id && field.name ) {
				field.id = field.name;
			}

			if( !field.id ) {
				continue;
			}

			// Remove previous listener if re-binding after a DOM swap
			if( _ngFieldObservers.has( field.id ) ) {
				const existingFunction = _ngFieldObservers.get( field.id ).func;
				field.removeEventListener( 'input', existingFunction );
				_ngFieldObservers.delete( field.id );
			}

			const func = function( evt ) {
				const record = _ngFieldObservers.get( this.id );
				ngDebounce( record.observeFieldID, () => {
					_ngObserveFieldTriggered( record.action, record.observeFieldID, record.updateContainerID, record.fullSubmit );
				}, 300 );
			};

			const record = {};
			record.observeFieldID = field.id;
			record.updateContainerID = updateContainerID;
			record.action = action;
			record.fullSubmit = fullSubmit;
			record.func = func;
			_ngFieldObservers.set( field.id, record );

			field.addEventListener( 'input', record.func );
		}
	}
}

// --- Initialization ---------------------------------------------------------

document.addEventListener('DOMContentLoaded', function() {
	_ngBindObserveFields();
	_ngBindObserveDescendants();

	// Re-bind observe fields whenever child elements change (e.g. after a container content swap).
	// Only watches childList changes — attribute-only mutations (class changes, etc.) are ignored.
	const observer = new MutationObserver( function() {
		_ngBindObserveFields();
		_ngBindObserveDescendants();
	});

	observer.observe( document.body, {
		childList: true,
		subtree: true
	});
}, false);
