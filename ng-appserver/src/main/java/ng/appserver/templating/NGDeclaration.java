package ng.appserver.templating;

import java.util.Enumeration;

import ng.appserver.NGApplication;
import ng.appserver.NGAssociation;
import ng.appserver.NGComponent;

public class NGDeclaration {

	final String _name;
	final String _type;
	final _NSMutableDictionary<String, NGAssociation> _associations;

	public NGDeclaration( String aName, String aType, _NSDictionary<String, NGAssociation> theAssocations ) {
		_name = aName;
		_type = aType;
		_associations = theAssocations.mutableClone();
		_setDebuggingForAssociations();
	}

	public String name() {
		return _name;
	}

	public String type() {
		return _type;
	}

	public _NSDictionary<String, NGAssociation> associations() {
		return _associations;
	}

	@Override
	public String toString() {
		return "<" + getClass().getName() + " name = " + _name + " type = " + _type + " associations " + _associations.toString() + " >";
	}

	public String stringRepresentation() {
		return _name + ":" + _type + " " + associations().toString() + "\n";
	}

	private void _setDebuggingForAssociations() {
		NGAssociation aDebugAssoc = _associations.remove( "WODebug" );
		if( aDebugAssoc != null ) {
			String aBindingName = null;
			Enumeration aKeyEnumerator = _associations.keyEnumerator();

			while( true ) {
				NGAssociation anAssociation;
				do {
					if( !aKeyEnumerator.hasMoreElements() ) {
						return;
					}

					aBindingName = (String)aKeyEnumerator.nextElement();
					anAssociation = (NGAssociation)_associations.objectForKey( aBindingName );
					anAssociation.setDebugEnabledForBinding( aBindingName, _name, _type );
				}
				while( aDebugAssoc.isValueConstant() && aDebugAssoc.valueInComponent( (NGComponent)null ) != null && NGApplication.application().isDebuggingEnabled() );

				anAssociation._setDebuggingEnabled( false );
			}
		}
	}
}