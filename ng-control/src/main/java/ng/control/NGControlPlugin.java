package ng.control;

import ng.appserver.NGApplication;
import ng.plugins.NGPlugin;

public class NGControlPlugin extends NGPlugin {

	@Override
	public void load() {
		// FIXME: Don't want to use the global application. Application needs to be passed as an argument at theis point, or we need the plugin to function as a route provider  // Hugi 2023-07-28
		NGApplication.application().routeTable().mapComponent( "/control", NGOverview.class );
	}
}