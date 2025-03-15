package ng.appserver.templating;

/**
 * Thrown in the case of bad binding configuration (missing required bindings or an unsupported binding combination)
 */

public class NGBindingConfigurationException extends RuntimeException {

	public NGBindingConfigurationException( String message ) {
		super( message );
	}
}
