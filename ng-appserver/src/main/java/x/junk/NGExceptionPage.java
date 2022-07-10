package x.junk;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ng.appserver.NGApplication;
import ng.appserver.NGComponent;
import ng.appserver.NGContext;
import ng.appserver.NGForwardException;
import x.junk.NGExceptionPage.WOExceptionParser.WOParsedErrorLine;

/**
 * A nicer version of WOExceptionPage.
 *
 * When in development mode, it will show java code where exception occurred (highlighting the exact line)
 */

public class NGExceptionPage extends NGComponent {

	private static final Logger logger = LoggerFactory.getLogger( NGExceptionPage.class );

	private static final int NUMBER_OF_LINES_BEFORE_ERROR_LINE = 7;
	private static final int NUMBER_OF_LINES_AFTER_ERROR_LINE = 7;

	/**
	 * The exception we're reporting.
	 */
	private Throwable _exception;

	/**
	 * Line of source file currently being iterated over in the view.
	 */
	public String currentSourceLine;

	/**
	 * Current index of the source line iteration.
	 */
	public int currentSourceLineIndex;

	/**
	 * WO class that parses the stack trace for us.
	 */
	public WOExceptionParser exceptionParser;

	/**
	 * Line of the stack trace currently being iterated over.
	 */
	public WOParsedErrorLine currentErrorLine;

	/**
	 * A path modifier to put between bundle path and modules so that source code locations outside the
	 * regular path can be accommodated.
	 */
	private String pathModifier;

	public NGExceptionPage( NGContext aContext ) {
		super( aContext );
		pathModifier = "";
	}

	public LocalDateTime now() {
		return LocalDateTime.now();
	}

	/**
	 * Specifying a path fragment here will insert that between bundle path and source module path. Example
	 * modifier: "/../.." to go two directories up. May be needed for non-standard workspace setups.
	 *
	 * @param modifier modifier to insert, should start with a "/"
	 *
	 */
	public void setPathModifier( String modifier ) {
		pathModifier = modifier;
	}

	/**
	 * @return First line of the stack trace, essentially the causing line.
	 */
	public WOParsedErrorLine firstLineOfTrace() {
		List<WOParsedErrorLine> stackTrace = exceptionParser.stackTrace();

		if( stackTrace.isEmpty() ) {
			return null;
		}

		return stackTrace.get( 0 );
	}

	/**
	 * @return true if source should be shown.
	 */
	public boolean showSource() {
		return NGApplication.application().isDevelopmentMode() && sourceFileContainingError() != null && !sourceFileContainingError().toString().contains( ".jar/" );
	}

	/**
	 * @return The source file where the exception originated (from the last line of the stack trace).
	 */
	private Path sourceFileContainingError() {
		return null;
		/*
		final String nameOfThrowingClass = firstLineOfTrace().packageClassPath();
		final NSBundle bundle = bundleForClassName( nameOfThrowingClass );
		
		if( bundle == null ) {
			return null;
		}
		
		final String path;
		
		if( NSBundle.mainBundle().getClass().getName().contains( "NSMavenProjectBundle" ) ) { // FIXME: We should probably be referencing the real class once that exists again // Hugi 2021-05-21
			path = bundle.bundlePath() + pathModifier + "/src/main/java/" + nameOfThrowingClass.replace( ".", "/" ) + ".java";
		}
		else {
			path = bundle.bundlePath() + pathModifier + "/Sources/" + nameOfThrowingClass.replace( ".", "/" ) + ".java";
		}
		
		return Paths.get( path );
		*/
	}

	/**
	 * @return The source lines to view in the browser.
	 */
	public List<String> lines() {
		final List<String> lines;

		try {
			lines = Files.readAllLines( sourceFileContainingError() );
		}
		catch( IOException e ) {
			logger.error( "Attempt to read source code from '{}' failed", sourceFileContainingError(), e );
			return new ArrayList<>();
		}

		int indexOfFirstLineToShow = firstLineOfTrace().line() - NUMBER_OF_LINES_BEFORE_ERROR_LINE;
		int indexOfLastLineToShow = firstLineOfTrace().line() + NUMBER_OF_LINES_AFTER_ERROR_LINE;

		if( indexOfFirstLineToShow < 0 ) {
			indexOfFirstLineToShow = 0;
		}

		if( indexOfLastLineToShow > lines.size() ) {
			indexOfLastLineToShow = lines.size();
		}

		return lines.subList( indexOfFirstLineToShow, indexOfLastLineToShow );
	}

	/**
	 * @return Actual number of source file line being iterated over in the view.
	 */
	public int currentActualLineNumber() {
		return firstLineOfTrace().line() - NUMBER_OF_LINES_BEFORE_ERROR_LINE + currentSourceLineIndex + 1;
	}

	/**
	 * @return CSS class for the current line of the source file (to show odd/even lines and highlight the error line)
	 */
	public String sourceLineClass() {
		List<String> cssClasses = new ArrayList<>();
		cssClasses.add( "src-line" );

		if( currentSourceLineIndex % 2 == 0 ) {
			cssClasses.add( "even-line" );
		}
		else {
			cssClasses.add( "odd-line" );
		}

		if( isLineContainingError() ) {
			cssClasses.add( "error-line" );
		}

		return String.join( " ", cssClasses );
	}

	/**
	 * @return true if the current line being iterated over is the line containining the error.
	 */
	private boolean isLineContainingError() {
		return currentSourceLineIndex == NUMBER_OF_LINES_BEFORE_ERROR_LINE - 1;
	}

	public Throwable exception() {
		return _exception;
	}

	public void setException( Throwable value ) {
		exceptionParser = new WOExceptionParser( value );
		_exception = value;
	}

	/**
	 * @return bundle of the class currently being iterated over in the UI (if any)
	 */
	public /*NSBundle*/ Object currentBundle() {
		return null;
		//		return bundleForClassName( currentErrorLine.packageClassPath() );
	}

	/**
	 * @return The bundle containing the (fully qualified) named class. Null if class is not found or not contained in a bundle.
	 */
	private static Object /* NSBundle */ bundleForClassName( String fullyQualifiedClassName ) {
		return null;
		//		Class<?> clazz;
		//
		//		try {
		//			clazz = Class.forName( fullyQualifiedClassName );
		//		}
		//		catch( ClassNotFoundException e ) {
		//			return null;
		//		}
		//
		//		return NSBundle.bundleForClass( clazz );
	}

	/**
	 * @return The CSS class of the current row in the stack trace table.
	 */
	public String currentRowClass() {
		/*
		if( NSBundle.mainBundle().equals( currentBundle() ) ) {
			return "success";
		}
		*/

		return null;
	}

	public static class WOExceptionParser {

		private List<WOParsedErrorLine> _stackTrace;
		private Throwable _exception;
		private String _message;
		private String _typeException;

		public WOExceptionParser( Throwable exception ) {
			_stackTrace = new ArrayList<>();
			_exception = NGForwardException._originalThrowable( exception );
			_message = _exception.getMessage();
			_typeException = _exception.getClass().getName();
			_parseException();
		}

		private List _ignoredPackages() {
			return Collections.emptyList();
			/*
			NSBundle bundle;
			String path, content;
			Map dic = null;
			List<NSBundle> allBundles = new ArrayList<>( NSBundle.frameworkBundles() );
			List<String> ignored = new ArrayList<>();

			for( Enumeration enumerator = Collections.enumeration( allBundles ); enumerator.hasMoreElements(); ) {
				bundle = (NSBundle)enumerator.nextElement();
				path = WOApplication.application().resourceManager().pathForResourceNamed( "WOIgnoredPackage.plist", bundle.name(), null );
				if( path != null ) {
					content = _stringFromFileSafely( path );
					if( content != null ) {
						dic = (Map)NSPropertyListSerialization.propertyListFromString( content );
						if( dic != null && dic.containsKey( "ignoredPackages" ) ) {
							@SuppressWarnings("unchecked")
							List<String> tmpArray = (List<String>)dic.get( "ignoredPackages" );
							if( tmpArray != null && tmpArray.size() > 0 ) {
								ignored.addAll( tmpArray );
							}
						}
					}
				}
			}

			return ignored;
			*/
		}

		private void _verifyPackageForLine( WOParsedErrorLine line, List packages ) {
			Enumeration enumerator;
			String ignoredPackageName, linePackageName;
			linePackageName = line.packageName();
			enumerator = Collections.enumeration( packages );

			while( enumerator.hasMoreElements() ) {
				ignoredPackageName = (String)enumerator.nextElement();
				if( linePackageName.startsWith( ignoredPackageName ) ) {
					line.setIgnorePackage( true );
					break;
				}
			}
		}

		private void _parseException() {
			StringWriter sWriter = new StringWriter();
			PrintWriter pWriter = new PrintWriter( sWriter, false );
			String string;
			List lines;
			List ignoredPackage;
			WOParsedErrorLine aLine;
			String line;

			int i, size;
			try {
				_exception.printStackTrace( pWriter );
				pWriter.close();
				sWriter.close(); // Added the try/catch as this throws in JDK 1.2
									// aB.
				string = sWriter.toString();
				i = _exception.toString().length(); // We skip the name of the
													// exception and the message for
													// our parse
				if( string.length() > i + 2 ) { // certain errors don't contain a
												// stack trace
					string = string.substring( i + 2 ); // Skip the exception type and
														// message
					lines = Arrays.asList( string.split( "\n" ) );
					ignoredPackage = _ignoredPackages();
					size = lines.size();
					_stackTrace = new ArrayList( size );
					for( i = 0; i < size; i++ ) {
						line = ((String)lines.get( i )).trim();
						if( line.startsWith( "at " ) ) {
							// If we don't have an open parenthesis it means that we
							// have probably reach the latest stack trace.
							aLine = new WOParsedErrorLine( line );
							_verifyPackageForLine( aLine, ignoredPackage );
							_stackTrace.add( aLine );
						}
					}
				}
			}
			catch( Throwable e ) {
				logger.error( "WOExceptionParser - exception collecting backtrace data " + e + " - Empty backtrace." );
				logger.error( "", e );
			}
			if( _stackTrace == null ) {
				_stackTrace = new ArrayList();
			}
		}

		public List<WOParsedErrorLine> stackTrace() {
			return _stackTrace;
		}

		public String typeException() {
			return _typeException;
		}

		public String message() {
			return _message;
		}

		/**
		 * Return a string from the contents of a file, returning null instead of any possible exception.
		 */
		private static String _stringFromFileSafely( String path ) {
			File f = new File( path );

			if( !f.exists() ) {
				return null;
			}

			FileInputStream fis = null;
			byte[] data = null;

			int bytesRead = 0;

			try {
				int size = (int)f.length();
				fis = new FileInputStream( f );
				data = new byte[size];

				while( bytesRead < size ) {
					bytesRead += fis.read( data, bytesRead, size - bytesRead );
				}

			}
			catch( java.io.IOException e ) {
				return null;
			}
			finally {
				if( fis != null ) {
					try {
						fis.close();
					}
					catch( java.io.IOException e ) {
						logger.debug( "Exception while closing file input stream: " + e.getMessage() );
						logger.debug( "", e );
					}
				}
			}

			if( bytesRead == 0 ) {
				return null;
			}
			return new String( data );
		}

		/**
		 * WOParsedErrorLine is the class that will parse an exception line. After
		 * parsing a line (see format in the constructor comment), each instance
		 * will be able to get information about the line, class, method where the
		 * error occurs.
		 *
		 * Evolution : should rewrite the parsing stuff... And verify the real
		 * format of java exception... Be careful, apparently it could happen that
		 * the latest ")" on a line is not present. This is why in the parsing stuff
		 * I try to get the index of this closing parenthesis.
		 */
		public static class WOParsedErrorLine {
			private String _packageName;
			private String _className;
			private String _methodName;
			private String _fileName;
			private int _line;
			private boolean _ignorePackage; // if true, then it will not be
											// possible to display an hyperlink

			public WOParsedErrorLine( String line ) {
				// line should have the format of an exception, which is normally
				// (below the index value)
				// at my.package.name.MyClass.myMethod(FileName.java:lineNumber)
				// ^ ^ ^ ^
				// atIndex I classIndex lineIndex
				// methodIndex
				int atIndex, methodIndex, classIndex, lineIndex, index;
				String string;
				atIndex = line.indexOf( "at " ) + 3;
				classIndex = line.indexOf( '(' ) + 1;
				methodIndex = line.lastIndexOf( '.', classIndex - 2 ) + 1;
				lineIndex = line.lastIndexOf( ':' );
				if( lineIndex < 0 ) { // We could potentially do not have the info if
										// we use a JIT
					_line = -1;
					_fileName = null;
				}
				else {
					lineIndex++;
					// Parse the line number
					index = line.indexOf( ')', lineIndex );
					if( index < 0 ) {
						index = line.length();
					}

					string = line.substring( lineIndex, index ); // Remove the last
																	// ")"

					try {
						_line = Integer.parseInt( string ); // Parse the fileName
						_fileName = line.substring( classIndex, lineIndex - 1 );
					}
					catch( NumberFormatException ex ) {
						_line = -1;
						_fileName = null;
					}
				}
				_methodName = line.substring( methodIndex, classIndex - 1 );
				_packageName = line.substring( atIndex, methodIndex - 1 );
				index = _packageName.lastIndexOf( '.' );
				if( index >= 0 ) {
					_className = _packageName.substring( index + 1 );
					_packageName = _packageName.substring( 0, index );
				}
				else {
					_className = _packageName;
				}
				if( _line < 0 ) {
					// JIT Activated so we don't have the class name... we can guess
					// it by using the package info\
					_fileName = _className + ".java";
				}
				_ignorePackage = false; // By default we handle all packages
			}

			public String packageName() {
				return _packageName;
			}

			public String className() {
				return _className;
			}

			public String packageClassPath() {
				if( _packageName.equals( _className ) ) {
					return _className;
				}
				return _packageName + "." + _className;
			}

			public String methodName() {
				return _methodName;
			}

			public boolean isDisable() {
				return _line < 0 || _ignorePackage;
			}

			private void setIgnorePackage( boolean yn ) {
				_ignorePackage = yn;
			}

			public String fileName() {
				return _fileName;
			}

			public String lineNumber() {
				if( _line >= 0 ) {
					return String.valueOf( _line );
				}

				return "NA";
			}

			public int line() {
				return _line;
			}

			@Override
			public String toString() {
				final String lineInfo = (_line >= 0) ? String.valueOf( _line ) : "No line info due to compiled code";
				final String fileInfo = (_line >= 0) ? _fileName : "Compiled code no file info";

				if( _packageName.equals( _className ) ) {
					return "class : " + _className + ": " + _methodName + " in file :" + fileInfo + " - line :" + lineInfo;
				}

				return "In package : " + _packageName + ", class : " + _className + " method : " + _methodName + " in file :" + fileInfo + " - line :" + lineInfo;
			}
		}
	}
}