package ng.control;

import ng.appserver.NGApplication;
import ng.plugins.NGPlugin;

public class NGControlPlugin extends NGPlugin {

	@Override
	public void load( NGApplication application ) {
		application.routeTable().mapComponent( "/control", NGControlLogin.class );
		application.elementManager().registerElementPackage( "ng.control" );
	}
}