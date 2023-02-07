package ng.control;

import java.util.List;

import ng.appserver.NGActionResults;
import ng.appserver.NGComponent;
import ng.appserver.NGContext;
import ng.appserver.NGSession;

public class NGOverview extends NGComponent {

	public NGSession currentSession;

	public NGOverview( NGContext context ) {
		super( context );
	}

	public List<NGSession> sessions() {
		return application().sessionStore().sessions();
	}

	public boolean isMySession() {
		return currentSession.equals( context().session() );
	}

	public NGActionResults terminate() {
		currentSession.terminate();
		return null;
	}
}