package ng.appserver.templating;

import ng.appserver.NGComponent;
import ng.appserver.NGKeyValueAssociation;

public class NGHelperFunctionKeyValueAssociation extends NGKeyValueAssociation {

	public NGHelperFunctionKeyValueAssociation( String s ) {
		super( s );
	}

	@Override
	public void setValue( Object obj, NGComponent wocomponent ) {

		//		if( WOHelperFunctionParser._debugSupport ) {
		//			WOHelperFunctionDebugUtilities.setDebugEnabled( this, wocomponent );
		//		}

		super.setValue( obj, wocomponent );
	}

	/*
	@Override
	protected String _debugDescription() {
		return keyPath();
	}
	*/
}
