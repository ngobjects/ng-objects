package ng.testapp.components;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

import ng.appserver.NGContext;
import ng.appserver.templating.NGComponent;

public class TAAjaxPage extends NGComponent {

	public String myName;

	public TAAjaxPage( NGContext context ) {
		super( context );
	}

	public String currentTime() {
		final String dateTime = LocalDateTime.now().format( DateTimeFormatter.ofPattern( "HH:mm:ss 'on' MMMM d YYYY" ) );
		final String encouragingMessage = encouragingMessages().get( new Random().nextInt( 0, encouragingMessages().size() - 1 ) );
		return "The current time is %s. %s".formatted( dateTime, encouragingMessage );
	}

	private List<String> encouragingMessages() {
		return List.of( "Your eyes look amazing!",
				"Your clicks rock my world!",
				"We just click!",
				"Your sheer click-ability makes me swoon!" );
	}
}