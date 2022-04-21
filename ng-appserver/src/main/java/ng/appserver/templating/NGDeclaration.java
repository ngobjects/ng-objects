package ng.appserver.templating;

import java.util.Enumeration;

import ng.appserver.NGApplication;
import ng.appserver.NGAssociation;
import ng.appserver.NGComponent;

public class NGDeclaration {
	String _name;
	String _type;
	_NSMutableDictionary<String, NGAssociation> _associations;

	public NGDeclaration( String aName, String aType, _NSDictionary<String, NGAssociation> theAssocations ) {
		this._name = aName;
		this._type = aType;
		this._associations = theAssocations.mutableClone();
		this._setDebuggingForAssociations();
	}

	private void _setDebuggingForAssociations() {
		NGAssociation aDebugAssoc = this._associations.remove( "WODebug" );
		if( aDebugAssoc != null ) {
			String aBindingName = null;
			Enumeration aKeyEnumerator = this._associations.keyEnumerator();

			while( true ) {
				NGAssociation anAssociation;
				do {
					if( !aKeyEnumerator.hasMoreElements() ) {
						return;
					}

					aBindingName = (String)aKeyEnumerator.nextElement();
					anAssociation = (NGAssociation)this._associations.objectForKey( aBindingName );
					anAssociation.setDebugEnabledForBinding( aBindingName, this._name, this._type );
				}
				while( aDebugAssoc.isValueConstant() && aDebugAssoc.valueInComponent( (NGComponent)null ) != null && NGApplication.application().isDebuggingEnabled() );

				anAssociation._setDebuggingEnabled( false );
			}
		}
	}

	public String name() {
		return this._name;
	}

	public String type() {
		return this._type;
	}

	public _NSDictionary<String, NGAssociation> associations() {
		return this._associations;
	}

	@Override
	public String toString() {
		return "<" + this.getClass().getName() + " name = " + this._name + " type = " + this._type + " associations "
				+ this._associations.toString() + " >";
	}

	public String stringRepresentation() {
		return this._name + ":" + this._type + " " + this.associations().toString() + "\n";
	}
}