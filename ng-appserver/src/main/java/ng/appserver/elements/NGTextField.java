package ng.appserver.elements;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ng.appserver.NGAssociation;
import ng.appserver.NGContext;
import ng.appserver.NGDynamicElement;
import ng.appserver.NGElement;
import ng.appserver.NGRequest;
import ng.appserver.NGResponse;

public class NGTextField extends NGDynamicElement {

	private static final Logger logger = LoggerFactory.getLogger( NGTextField.class );

	/**
	 * 'name' attribute of the text field. If not specified, will be populated using the elementID
	 */
	private NGAssociation _nameAssociation;

	/**
	 * The value for the field. This is a bidirectional binding that will also pass the value upstrem.
	 */
	private NGAssociation _valueAssociation;

	public NGTextField( String name, Map<String, NGAssociation> associations, NGElement template ) {
		super( null, null, null );
		_nameAssociation = associations.get( "name" );
		_valueAssociation = associations.get( "value" );
	}

	@Override
	public void takeValuesFromRequest( NGRequest request, NGContext context ) {
		// FIXME: we should only grab values from the request if it's containing form is being submitted. We probably need to pass that info along from NGForm (or perhaps NGSubmitButton?)
		// FIXME: In that case, we're going to want to push the field's value through the value binding

		final List<String> valuesFromRequest = request.formValues().get( nameFromCurrentElementId( context ) ); // FIXME: Account for multiple values/no value

		if( valuesFromRequest != null ) { // FIXME: Should formValues return an empty list or null if not present? // Hugi 2022-06-08
			String valueFromRequest = valuesFromRequest.get( 0 );
			logger.info( "We're going for it! We're setting that value to: " + valueFromRequest );
			_valueAssociation.setValue( valueFromRequest, context.component() );
		}
	}

	@Override
	public void appendToResponse( final NGResponse response, final NGContext context ) {

		String name;

		if( _nameAssociation != null ) {
			name = (String)_nameAssociation.valueInComponent( context.component() );
		}
		else {
			name = nameFromCurrentElementId( context );
		}

		String value = "";

		if( _valueAssociation != null ) { // FIXME: _valueAssociation should actually not be allowed to be null
			value = (String)_valueAssociation.valueInComponent( context.component() ); // FIXME: This value might need to be converted/formatted
		}

		// FIXME: Not sure if we should be setting the empty string here or making some other special accommodation for null
		if( value == null ) {
			value = "";
		}

		// FIXME: Using String.format for convenience. We probably want to change that later for performance reasons // Hugi 2022-06-05
		// FIXME: Omit empty tags
		final String tagString = String.format( "<input type=\"text\" name=\"%s\" value=\"%s\" />", name, value );
		response.appendContentString( tagString );
	}

	/**
	 * @return A unique name for this text field, based on the WOContext's elementId
	 *
	 * FIXME: Implement
	 */
	private String nameFromCurrentElementId( final NGContext context ) {
		return context.elementID().toString();
	}
}