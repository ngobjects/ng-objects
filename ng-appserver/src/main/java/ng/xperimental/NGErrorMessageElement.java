package ng.xperimental;

import ng.appserver.NGContext;
import ng.appserver.NGElement;
import ng.appserver.NGResponse;

public class NGErrorMessageElement implements NGElement {

	private final String _type;
	private final String _heading;
	private final String _message;

	public NGErrorMessageElement( final String type, final String heading, final String message ) {
		_type = type;
		_heading = heading;
		_message = message;
	}

	@Override
	public void appendToResponse( NGResponse response, NGContext context ) {
		response.appendContentString( """
				<span style="display: inline-block; margin: 10px; padding: 10px; color:white; background-color: rgba(255,100,100,0.5); border: 1px solid red; border-radius: 5px; box-shadow: 5px 5px 0px rgba(0,0,200,0.8); font-size: 12px;">
					<span style="font-size: 20px">%s</span> %s<br>
					<span style="font-size: 16px"><strong>%s</strong></span><br>
					%s
				</span>
				""".formatted( u( "DOG FACE" ), _type, _heading, _message ) );
	}

	/**
	 * @return The unicode character corresponding to the given character name
	 */
	private static String u( final String unicodeCharacterName ) {
		final int codePoint = Character.codePointOf( unicodeCharacterName );
		return Character.toString( codePoint ).toString();
	}
}