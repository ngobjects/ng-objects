package ng.control;

import java.util.List;

import ng.appserver.NGApplication;
import ng.appserver.NGComponent;
import ng.appserver.NGContext;
import ng.appserver.NGSession;

public class NGOverview extends NGComponent {

	public NGSession currentSession;

	public NGOverview( NGContext context ) {
		super( context );
	}

	public List<NGSession> sessions() {
		return NGApplication.application().sessionStore().sessions();
	}
}