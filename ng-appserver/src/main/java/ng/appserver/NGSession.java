package ng.appserver;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Yes, we have sessions too!
 */

public class NGSession {

	/**
	 * FIXME: This is not the way we're going to store/cache contexts. Just for testing
	 */
	public List<NGContext> contexts = new ArrayList<>();

	/**
	 * A unique identifier for this session
	 */
	final String _sessionID;

	/**
	 * The birthdate of this session, as provided by System.currentTimeMillis()
	 */
	final long _birthDate;

	public NGSession() {
		this( UUID.randomUUID().toString() );
	}

	private NGSession( final String sessionID ) {
		this( sessionID, System.currentTimeMillis() );
	}

	private NGSession( final String sessionID, long birthDate ) {
		_sessionID = sessionID;
		_birthDate = birthDate;
	}

	/**
	 * FIXME: This is horrid and does not belong here // Hugi 2022-06-25
	 */
	public static NGSession createSession() {
		try {
			return NGApplication.application()._sessionClass().getConstructor().newInstance();
		}
		catch( InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e ) {
			throw new RuntimeException( e );
		}
	}
}