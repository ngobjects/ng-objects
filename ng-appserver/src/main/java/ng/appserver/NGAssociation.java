package ng.appserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class NGAssociation {

	private static final Logger logger = LoggerFactory.getLogger( NGAssociation.class );

	/**
	 * This boolean is added since it's used by WOOgnl. Definitely want to remove this // FIXME Hugi 2021-11-13
	 */
	protected boolean _debugEnabled = true;

	public Object valueInComponent( NGComponent aComponent ) {
		return null;
	}

	public void setValue( Object aValue, NGComponent aComponent ) {
	}

	protected void _logPullValue( Object aValue, NGComponent aComponent ) {
		logger.error( "Not implemented" ); // FIXME
	}

	protected void _logPushValue( Object aValue, NGComponent aComponent ) {
		logger.error( "Not implemented" ); // FIXME
	}

	public void setDebugEnabledForBinding( String bindingName, String declarationName, String declarationType ) {
		logger.error( "Not implemented" ); // FIXME
	}

	public void _setDebuggingEnabled( boolean b ) {
		logger.error( "Not implemented" ); // FIXME
	}
}