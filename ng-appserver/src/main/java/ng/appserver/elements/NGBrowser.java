package ng.appserver.elements;

import java.util.Map;

import ng.appserver.NGAssociation;
import ng.appserver.NGElement;

/**
 * FIXME: This should be a select element for multiple selection. Implement. Also, not sure about this name... // Hugi 2023-09-25
 */

public class NGBrowser extends NGPopUpButton {

	public NGBrowser( String name, Map<String, NGAssociation> associations, NGElement template ) {
		super( name, associations, template );
	}
}