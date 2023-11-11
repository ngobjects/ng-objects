function invokeUpdate( id, url ) {
	const xhttp = new XMLHttpRequest();
	xhttp.open("GET", url, true);
	xhttp.setRequestHeader( 'x-updatecontainerid', id )

	// FIXME: ugly as all hell, just here for some testing
	if( id != 'null') {
		xhttp.onload = () => {
			var updateContainer = document.getElementById(id);
			
			if( !updateContainer ) {
				// CHECKME: WE should probably abandon the whole operation if there UC is missing
				alert( 'No AjaxUpdateContainer on the page with id ' + id );
			}
	
			updateContainer.innerHTML = xhttp.responseText;
	    };
	}
	
	xhttp.send();
}

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

/*  
var AUC = {
	register: function(id, options) {
		if (!options) {
			options = {};
		}
		eval(id + "Update = function() {AjaxUpdateContainer.update(id, options) }");
	}
}

var AUL = {
	update: function(id, options, elementID, queryParams) {
		
		// Just some logging. For fun.
		console.log( "===== Clicked AjaxUpdateLink =====")
		console.log( "id: " + id );
		console.log( "options: " + options );
		console.log( "elementID: " + elementID );
		console.log( "queryParams: " + queryParams );
		
		// This is the updateContainer we're going to target
		var updateContainer = document.getElementById(id);
		
		if( !updateContainer ) {
			alert( 'No AjaxUpdateContainer on the page with id ' + id );
		}

		var actionUrl = updateContainer.getAttribute('data-updateUrl');

		// We cleverly replace the elementID on the UC to the clicked link's element ID
		actionUrl = actionUrl.replace(/[^\/]+$/, elementID);
		actionUrl = actionUrl + '?_u=' + id;
		invokeUpdate( id, actionUrl );
	}
}
*/