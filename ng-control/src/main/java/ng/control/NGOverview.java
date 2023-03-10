package ng.control;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import ng.appserver.NGActionResults;
import ng.appserver.NGComponent;
import ng.appserver.NGContext;
import ng.appserver.NGSession;

public class NGOverview extends NGComponent {

	public NGSession currentSession;
	public Object currentPropertyKey;

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

	public List<String> systemPropertyKeys() {
		final List<String> list = new ArrayList( System.getProperties().keySet() );
		list.sort( Comparator.naturalOrder() );
		return list;
	}

	public Object currentSystemPropertyValue() {
		return System.getProperties().get( currentPropertyKey );
	}

	public List<String> propertyKeys() {
		final List<String> list = new ArrayList<>( application().properties().allKeys() );
		list.sort( Comparator.naturalOrder() );
		return list;
	}

	public String currentPropertyValue() {
		return application().properties().get( (String)currentPropertyKey );
	}
}