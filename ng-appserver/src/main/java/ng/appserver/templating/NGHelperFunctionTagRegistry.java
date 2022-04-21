package ng.appserver.templating;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import ng.appserver.elements.NGHyperlink;
import ng.appserver.elements.NGImage;
import ng.appserver.elements.NGString;
import ng.appserver.elements.NGStylesheet;

public class NGHelperFunctionTagRegistry {

	public static Map<String, String> tagShortcutMap() {
		Map<String, String> m = new HashMap<>();
		m.put( "str", NGString.class.getSimpleName() );
		m.put( "img", NGImage.class.getSimpleName() );
		m.put( "link", NGHyperlink.class.getSimpleName() );
		m.put( "stylesheet", NGStylesheet.class.getSimpleName() );
		return m;
	}

	public static Map<String, NGTagProcessor> tagProcessorMap() {
		return Collections.emptyMap();
	}
}