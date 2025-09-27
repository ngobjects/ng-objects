package ng.testapp.components;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import ng.appserver.NGActionResults;
import ng.appserver.NGContext;
import ng.appserver.templating.NGComponent;

public class TAGuestbookPage extends NGComponent {

	public record GuestBookEntry( String name, LocalDate date ) {}

	public String myName;

	public List<GuestBookEntry> entries = new ArrayList<>();
	public GuestBookEntry currentEntry;

	public String errorMessage;

	public TAGuestbookPage( NGContext context ) {
		super( context );
	}

	public NGActionResults addName() {

		if( myName == null || myName.isBlank() ) {
			errorMessage = "Please enter a name!";
		}
		else {
			entries.add( new GuestBookEntry( myName, LocalDate.now() ) );
			myName = null;
			errorMessage = null;
		}

		return null;
	}
}