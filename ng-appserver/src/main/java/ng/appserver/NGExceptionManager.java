package ng.appserver;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Handles exceptions that occur during request execution.
 *
 * FIXME: This is API under development. Do some development // Hugi 2025-03-23
 * FIXME: Both handler functions should probably get request/context as parameters for additional information, along with the exception // Hugi 2025-03-23
 * FIXME: In the case of exception inheritance hierarchies, we really should be looking for the "most closely related" exception when looking up handlers/response generators, not just exact matches // Hugi 2025-03-23
 */

public class NGExceptionManager {

	/**
	 * List of handlers for different exception types
	 */
	private List<NGExceptionHandler<? super Throwable>> _handlers = new ArrayList<>();

	/**
	 * List of response generators for exception types
	 */
	private List<NGExceptionResponseGenerator<? super Throwable>> _responseGenerators = new ArrayList<>();

	/**
	 * FIXME: We're hardcoding a couple of exception types at the moment, this should be done in the application/the actual module that bears responsibility for the exceptions // Hugi 2025-03-23
	 */
	public NGExceptionManager( final NGApplication application ) {
		register(
				NGPageRestorationException.class,
				NGExceptionManager::doNothing,
				application::responseForPageRestorationException );

		register(
				NGSessionRestorationException.class,
				NGExceptionManager::doNothing,
				application::responseForSessionRestorationException );
	}

	public <E extends Throwable> void register(
			Class<E> exceptionClass,
			Consumer<E> handler,
			Function<E, NGActionResults> responseGeneratingFunction ) {
		registerHandler( exceptionClass, handler );
		registerResponseGenerator( exceptionClass, responseGeneratingFunction );
	}

	public void handleException( final Throwable throwable ) {

		boolean handled = false;

		for( final NGExceptionHandler<? super Throwable> handler : _handlers ) {
			if( handler.exceptionClass().isAssignableFrom( throwable.getClass() ) ) {
				handler.consumer().accept( throwable );
				handled = true;
			}
		}

		// If the exception didn't get any special treatment, let the application class handle it
		if( !handled ) {
			NGApplication.application().handleException( throwable );
		}
	}

	public NGActionResults responseForException( final Throwable throwable, final NGContext context ) {

		for( final NGExceptionResponseGenerator<? super Throwable> responseGenerator : _responseGenerators ) {
			if( responseGenerator.exceptionClass().isAssignableFrom( throwable.getClass() ) ) {
				return responseGenerator.function().apply( throwable );
			}
		}

		return NGApplication.application().responseForException( throwable, context );
	}

	public <E extends Throwable> void registerHandler( Class<E> exceptionClass, Consumer<E> consumer ) {
		_handlers.add( new NGExceptionHandler( exceptionClass, consumer ) );
	}

	public <E extends Throwable> void registerResponseGenerator( Class<E> exceptionClass, Function<E, NGActionResults> function ) {
		_responseGenerators.add( new NGExceptionResponseGenerator( exceptionClass, function ) );
	}

	private static void doNothing( Throwable e ) {}

	private record NGExceptionHandler<E extends Throwable>(
			Class<E> exceptionClass,
			Consumer<E> consumer ) {}

	private record NGExceptionResponseGenerator<E extends Throwable>(
			Class<E> exceptionClass,
			Function<E, NGActionResults> function ) {}
}