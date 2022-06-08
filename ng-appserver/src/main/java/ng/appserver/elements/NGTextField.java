package ng.appserver.elements;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ng.appserver.NGAssociation;
import ng.appserver.NGContext;
import ng.appserver.NGDynamicElement;
import ng.appserver.NGElement;
import ng.appserver.NGResponse;

public class NGTextField extends NGDynamicElement {

	private static final Logger logger = LoggerFactory.getLogger( NGTextField.class );

	/**
	 * 'name' attribute of the text field. If not specified, will be populated using the elementID
	 */
	private NGAssociation _nameAssociation;

	public NGTextField( String name, Map<String, NGAssociation> associations, NGElement template ) {
		super( name, associations, template );
		_nameAssociation = associations.get( "name" );
	}

	@Override
	public void appendToResponse( final NGResponse response, final NGContext context ) {

		if( !context.isInForm() ) {
			// FIXME: This warning should be suppressable. IT's really just
			logger.warn( "Warning, you're rendering an NGTextField outside of a form" );
		}

		String name;

		if( _nameAssociation != null ) {
			name = (String)_nameAssociation.valueInComponent( context.component() );
		}
		else {
			name = nameFromCurrentElementId( context );
		}

		// FIXME: Using String.format for convenience. We probably want to change that later for performance reasons // Hugi 2022-06-05
		final String tagString = String.format( "<input type=\"text\" name=\"%s\" />", name );
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