package ng.control;

import ng.plugins.NGPlugin;
import ng.plugins.Routes;

public class NGControlPlugin implements NGPlugin {

	@Override
	public String namespace() {
		return "control";
	}

	@Override
	public Routes routes() {
		return Routes
				.create()
				.map( "/control", NGControlLogin.class );
	}

	//	// Should not take an application argument. Is invoked after the plugin routes and elements have been loaded
	//	// Or... We should perhaps have two methods - beforeLoad() and afterLoad(). even beforeLoadAll(), afterLoadAll() afterInitAll(), beforeInitAll() etc. Keep track of the lifecycle phases of applicaiton startup
	//	@Override
	//	public void load( NGApplication application ) {}
	//
	//	Invoked after the application has been run
	//	public void init( NGApplication application ) {}
	//
	//	@Override
	//	public Routes routes() {
	//		return Routes
	//				.map( "/home", request -> new NGResponse( "You're home", 200 ) )
	//				.map( "/home/user", NGUserDetailPage.class )
	//				.map( "/some/page", request -> {
	//					String someValue = request.formValueForKey( "someValue" );
	//					SomePage page = request.context().pageWithName( SomePage.class );
	//					page.someValue = someValue;
	//					return page;
	//				}
	//	}
}