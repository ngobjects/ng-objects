package er.extensions.bettertemplates;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NGHelperFunctionTagRegistry {

	public static Logger logger = LoggerFactory.getLogger( NGHelperFunctionTagRegistry.class );

	public static boolean allowInlineBindings() {
		return true;
	}

	public static Map<String, String> tagShortcutMap() {
		Map<String, String> m = new HashMap<>();
		m.put( "str", "NGString" );
		m.put( "img", "NGImage" );
		return m;
	}

	public static Map<String, NGTagProcessor> tagProcessorMap() {
		return Collections.emptyMap();
	}
}