package ng.control;

import ng.appserver.NGApplication;
import ng.plugins.Elements;
import ng.plugins.NGPlugin;

public class NGControlPlugin implements NGPlugin {

	// Should not take an application argument. Is invoked after the plugin routes and elements have been loaded
	// Or... We should perhaps have two methods - beforeLoad() and afterLoad(). even beforeLoadAll(), afterLoadAll() afterInitAll(), beforeInitAll() etc. Keep track of the lifecycle phases of applicaiton startup
	@Override
	public void load( NGApplication application ) {
		application.routeTable().map( "/control", NGControlLogin.class );
	}

	// Invoked after the application has been run
	public void init( NGApplication application ) {

	}

	@Override
	public String namespace() {
		return "control";
	}

	@Override
	public Elements elements() {
		return Elements
				.create()
				.elementPackage( "ng.control" );
	}

	//	@Override
	//	public Routes routes() {
	//		return Routes
	//				.map( "/home", context -> new NGResponse( "You're home", 200 ) )
	//				.mapComponent( "/home/user", NGUserDetailPage.class );
	//	}
	//	@Override
	//	public Routes routes() {
	//		return Routes
	//				.map( "/home", context -> new NGResponse( "You're home", 200 ) )
	//				.mapComponent( "/home/user", NGUserDetailPage.class );
	//	}
	//
	//	/**
	//	 * @return A class describing how
	//	 */
	//	@Override
	//	public Elements elements() {
	//		return Elements
	//				.register()
	//				.fromClass( NGElement.class, "someTag", "someOtherTag" ) // signature( Class<? extends NGElement> elementClass, String ... tagNames )
	//				.fromPackage( "my.element.package" ) // signature( String packageName )
	//				.elementAlias( "NGPopUpButton", "popUpButton", "popup" ); // signature ( String tagName, String... tagAliases )
	//	}
}