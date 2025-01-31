/**
 * Invoked by AjaxUpdateLink.
 * Initiates a GET request to the given URL and updates the container identified by 'id' with the returned response content.
 * 
 * FIXME: Currently, null can be passed in as an id parameter. Invoking an action without a resulting update feels like such a special situation (WRT caching, rendering and other handling on the server side) that this might warrant a separate function // Hugi 2024-10-05    
 */
function ajaxUpdateLinkClick( url, id ) {
	const xhttp = new XMLHttpRequest();
	xhttp.open("GET", url, true);
	xhttp.setRequestHeader( 'x-updatecontainerid', id )

	if( id ) {
		var updateContainer = document.getElementById(id);
		
		if( !updateContainer ) {
			alert( 'No AjaxUpdateContainer on the page with id ' + id );
		}

		xhttp.onload = () => {
			updateContainer.innerHTML = xhttp.responseText;
	    };
	}
	
	xhttp.send();
}

/**
 * Invoked on the click of an AjaxSubmitButton
 * 
 * @param button The button clicked to submit the form
 * @param updateContainerID The ID of an update container to display the returned HTML
 */
function ajaxSubmitButtonClick(button,updateContainerID) {

	// The form we are submitting is always the clicked button's containing form
	var form = button.form;

	// The URL we'll be targeting is the form's action URL.
	// Just like with regular forms, deciding which button was actually clicked is based on the element name of the button.
	// Due to this The name of the button gets added to the request's query parameters below.
	var url = form.action;
	
	// Get the values from the form to submit and wrap them in URLSearchParams for x-www-form-urlencoded
	var params = new URLSearchParams(new FormData(form));
	
	// Since JS won't add the name of the clicked button to the request's parameters for us
	// (as is usually done by a regular form submit using an input type="submit"),
	// we add the button's name to the submitted values ourselves, allowing the framework to determine the pressed button.
	params.append(button.name,button.value);

	// The update container that will be targeted by the update
	// FIXME: Allow form submission without a specified updateContainer 
	var updateContainer = document.getElementById(updateContainerID);

	fetch(url, {
	  method: form.method,
	  body: params,
	  headers: {
	    'Content-Type': 'application/x-www-form-urlencoded',
	    'x-updatecontainerid': updateContainerID
	  }
	})
	.then(response => response.text() )
	.then(text => {
		updateContainer.innerHTML = text;
	});
}

/**
 * WIP; AjaxObserveField
 * 
 * Submits an observed field's form every time a change is observed
 */
function performSubmit( form ) {
    var data = new FormData(form);
    
    // Add extra data to form if required submission.
    // We can use this to submit a subset of the form fields!
    data.append("someField","someValue");

	// Obtain the form's action url for use when submitting    
	const uri = form.getAttribute('action');

	// var data = '3.1.1=0&3.1.3=200&3.1.5=0&3.1.7=200';

    var xhr = new XMLHttpRequest();
	xhr.open('POST',uri);
    // xhr.setRequestHeader('content-type','application/x-www-form-urlencoded')
    xhr.send(data);

	console.log( data );

	/*
    xhr.onreadystatechange = function() {
        if (xhr.readyState == XMLHttpRequest.DONE) {
            form.reset(); //reset form after AJAX success.
        }
    }
    */
}

/**
 * WIP; AjaxObserveField
 * 
 * Observes all input fields contained witin the given element
 */
function observeDescendantFields( containerElement) {
	
	const list = containerElement.getElementsByTagName("input"); // FIXME: We need to add all field types 

	for( i in list ) {
		const item = list[i];
		item.onchange = function() {
			performSubmit(item.form); // FIXME: We need to perform the action bound to the AjaxUpdateContainer instead
		};
	}
}

/**
 * WIP; AjaxObserveField
 * 
 * Activates observation on all fields contained by an AjaxObserveField
 */
function activateObservation() {
	const list = document.getElementsByClassName( "ng-observe-descendant-fields" );

	for (let i = 0; i < list.length; i++) {
		const containerElement = list[i];
		console.log( containerElement );
		observeDescendantFields( containerElement );
	}
}

/**
 * WIP; AjaxObserveField
 * Activates AjaxObserveField observation on page load.
 * 
 * FIXME: This isn't currently enough, we're going to have to initiate observation every time page is modified/the DOM model changes 
 */
document.addEventListener('DOMContentLoaded', function() {
    activateObservation();
}, false);