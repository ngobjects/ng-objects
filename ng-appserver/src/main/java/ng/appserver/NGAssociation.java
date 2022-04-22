package ng.appserver;

public abstract class NGAssociation {

	public Object valueInComponent( NGComponent aComponent ) {
		return null;
	}

	public void setValue( Object aValue, NGComponent aComponent ) {}

	/**
	 * FIXME: Only added while we try out templating
	 */
	public void setDebugEnabledForBinding( String aBindingName, String _name, String _type ) {}

	/**
	 * FIXME: Only added while we try out templating
	 */
	public void _setDebuggingEnabled( boolean b ) {}
}