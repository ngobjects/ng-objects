package ng.appserver.templating.elements;

import java.util.Map;

import ng.appserver.templating.NGElement;
import ng.appserver.templating.assications.NGAssociation;

/**
 * FIXME: The implementation for multiple selection should be moved here from NGPopUpButton // Hugi 2024-08-14
 * FIXME: Both this class and NGPopUpButton should probably be renamed to something like NGSelect/NGSelectMultiple to better reflect the HTML element they represents // Hugi 2024-08-14
 */

public class NGBrowser extends NGPopUpButton {

	public NGBrowser( String name, Map<String, NGAssociation> associations, NGElement template ) {
		super( name, associations, template );
	}
}