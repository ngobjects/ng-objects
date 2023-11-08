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
		
		
//		if( updateContainer ) {
//			alert('No AjaxUpdateContainer on the page with id ' + id);
//		}

		var actionUrl = updateContainer.getAttribute('data-updateUrl');

		// We cleverly replace the elementID on the UC to the clicked link's element ID
		actionUrl = actionUrl.replace(/[^\/]+$/, elementID);
		actionUrl = actionUrl + '?_u=' + id;
		invokeUpdate( id, actionUrl );
	}
}

function invokeUpdate( id, url ) {
	const xhttp = new XMLHttpRequest();
	xhttp.open("GET", url, true);
	xhttp.setRequestHeader( 'x-updatecontainerid', id )
	
	xhttp.onload = () => {
		var updateContainer = document.getElementById(id);
		updateContainer.innerHTML = xhttp.responseText;
    };
	
	xhttp.send();
}

function updateSmu( url ) {
	invokeUpdate( 'smu', url );
}