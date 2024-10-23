package ng.control;

import java.util.Objects;

import ng.appserver.NGActionResults;
import ng.appserver.NGComponent;
import ng.appserver.NGContext;

public class NGControlLogin extends NGComponent {

	public String password;
	private String _errorMessage;

	public NGControlLogin( NGContext context ) {
		super( context );
	}

	/**
	 * @return Perform a "login" (in a very wide sense of that word)
	 *
	 * CHECKME: We should really involve some authentication here... // Hugi 2024-10-23
	 */
	public NGActionResults login() {

		if( !Objects.equals( environmentPassword(), password ) ) {
			_errorMessage = "Wrong password";
			return null;
		}

		return pageWithName( NGControlOverview.class );
	}

	/**
	 * @return Error message shown to the user
	 */
	public String errorMessage() {

		if( !environmentPasswordIsSet() ) {
			return "To access this page, you must set the property 'ng.control.password'";
		}

		return _errorMessage;
	}

	/**
	 * @return The password required for access to the environment
	 */
	public String environmentPassword() {
		return application().properties().get( "ng.control.password" );
	}

	/**
	 * @return true if the environment password is set
	 */
	public boolean environmentPasswordIsSet() {
		return environmentPassword() != null;
	}
}