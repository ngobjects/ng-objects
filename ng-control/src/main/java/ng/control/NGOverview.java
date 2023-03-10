package ng.control;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

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

	public Map<Object, Object> properties() {
		return System.getProperties();
	}

	public List<String> propertyKeys() {
		final List<String> list = new ArrayList( System.getProperties().keySet() );
		list.sort( Comparator.naturalOrder() );
		return list;
	}

	public Object currentPropertyValue() {
		return properties().get( currentPropertyKey );
	}
}