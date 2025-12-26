package ng.appserver.templating.associations;

import ng.appserver.templating.NGComponent;

/**
 * A binding association allows a component to access it's bindings direcly and push/pull values, by prefixing the binding name with "^".
 *
 *  Example:
 *
 *  <wo:str value="$^someValue" />
 *
 *  This would directly get the value of the component's "someValue" binding
 */

public class NGBindingAssociation extends NGAssociation {

	private final String _bindingName;

	public NGBindingAssociation( final String binding ) {
		_bindingName = binding;
	}

	@Override
	public Object valueInComponent( final NGComponent component ) {
		return component.valueForBinding( _bindingName );
	}

	@Override
	public void setValue( Object value, NGComponent component ) {
		component.setValueForBinding( value, _bindingName );
	}
}