package ng.appserver.templating.assications;

import ng.appserver.NGComponent;
import ng.appserver.templating.NGElementUtils;

public abstract class NGAssociation {

	/**
	 * @return The value of this association resolved against the given component
	 */
	public Object valueInComponent( NGComponent aComponent ) {
		return null;
	}

	/**
	 * Set the value of this association in the given component
	 */
	public void setValue( Object aValue, NGComponent aComponent ) {}

	/**
	 * @return The value of this binding interpreted as a boolean in a "truthy" way (as specified by isTruthy())
	 */
	public boolean booleanValueInComponent( NGComponent component ) {
		final Object value = valueInComponent( component );
		return NGElementUtils.isTruthy( value );
	}
}