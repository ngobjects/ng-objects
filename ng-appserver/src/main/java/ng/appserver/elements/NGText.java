package ng.appserver.elements;

import java.util.Map;

import ng.appserver.NGAssociation;
import ng.appserver.NGElement;

/**
 * FIXME: This should be a text area. implement
 */

public class NGText extends NGString {

	public NGText( String name, Map<String, NGAssociation> associations, NGElement template ) {
		super( name, associations, template );
	}
}