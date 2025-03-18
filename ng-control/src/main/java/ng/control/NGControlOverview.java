package ng.control;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import ng.appserver.NGActionResults;
import ng.appserver.NGContext;
import ng.appserver.NGPageCache.NGPageCacheEntry;
import ng.appserver.NGSession;
import ng.appserver.properties.NGProperties.PropertiesSource;
import ng.appserver.templating.NGComponent;

public class NGControlOverview extends NGComponent {

	public NGPageCacheEntry currentCacheEntry;
	public NGPageCacheEntry currentChildEntry;
	public NGSession currentSession;
	public Object currentPropertyKey;

	public PropertiesSource currentPropertySource;

	public NGControlOverview( NGContext context ) {
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