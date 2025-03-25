package ng.appserver.properties;

/**
 * FIXME: Temp class for holding deprecated logic while we migrate our properties to a nice new model // Hugi 2025-03-25
 */

public class NGPropertiesDeprecated {

	private final NGProperties _p;

	public NGPropertiesDeprecated( final NGProperties properties ) {
		_p = properties;
	}

	/**
	 * Container for all the old properties from WO
	 */
	private enum WOProperties {
		// Properties in use
		WOOutputPath,
		WOApplicationName,
		WOMonitorEnabled,
		WOLifebeatEnabled,
		WOLifebeatInterval,
		WOLifebeatDestinationPort,
		WOHost,
		WOPort,

		// Properties currently NOT in use
		NSProjectSearchPath,
		WOAdaptor,
		WOAutoOpenClientApplication,
		WOAutoOpenInBrowser,
		WOCachingEnabled,
		WODebuggingEnabled,
		WOListenQueueSize,
		WONoPause,
		WOSessionTimeOut,
		WOWorkerThreadCount,
		WOWorkerThreadCountMax,
		WOWorkerThreadCountMin
	}

	public Integer propWOPort() {
		return _p.getInteger( WOProperties.WOPort.name() );
	}

	public String propWOHost() {
		return _p.get( WOProperties.WOHost.name() );
	}

	public Integer propWOLifebeatDestinationPort() {
		return _p.getInteger( WOProperties.WOLifebeatDestinationPort.name() );
	}

	public Integer propWOLifebeatIntervalInSeconds() {
		return _p.getInteger( WOProperties.WOLifebeatInterval.name() );
	}

	public boolean propWOLifebeatEnabled() {
		return "YES".equals( _p.get( WOProperties.WOLifebeatEnabled.name() ) );
	}

	public boolean propWOMonitorEnabled() {
		return "YES".equals( _p.get( WOProperties.WOMonitorEnabled.name() ) );
	}

	public String propWOApplicationName() {
		return _p.get( WOProperties.WOApplicationName.name() );
	}

	public String propWOOutputPath() {
		return _p.get( WOProperties.WOOutputPath.name() );
	}
}