package x.junk;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ng.appserver.NGComponent;
import ng.appserver.NGContext;

/**
 * Exception page displayed in development mode.
 *
 * Displays the java code where exception occurred (highlighting the line containing the error)
 */

public class NGExceptionPageDevelopment extends NGComponent {

	private static final Logger logger = LoggerFactory.getLogger( NGExceptionPageDevelopment.class );

	/**
	 * Number of source lines to show above the error line
	 */
	private static final int NUMBER_OF_LINES_BEFORE_ERROR_LINE = 7;

	/**
	 * Number of source lines to show below the error line
	 */
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
	 * Line of the stack trace currently being iterated over.
	 */
	public StackTraceElement currentStackTraceElement;

	public NGExceptionPageDevelopment( NGContext aContext ) {
		super( aContext );
	}

	/**
	 * @return Name of current package being iterated over in the stack trace
	 */
	public String currentPackageName() {
		return packageNameFromClassName( currentStackTraceElement.getClassName() );
	}

	/**
	 * @return The current date and time for display on the exception page. Useful for tracing when customers send you screenshots of exceptions.
	 */
	public LocalDateTime now() {
		return LocalDateTime.now();
	}

	/**
	 * @return First line of the stack trace, essentially the causing line.
	 */
	public StackTraceElement firstLineOfTrace() {
		StackTraceElement[] stackTrace = exception().getStackTrace();

		if( stackTrace.length == 0 ) {
			return null;
		}

		return stackTrace[0];
	}

	/**
	 * @return true if source should be shown.
	 */
	public boolean showSource() {

		// We only show the source file if we're in development mode (exposing code to production users is generally considered bad practice)
		if( !application().isDevelopmentMode() ) {
			return false;
		}

		if( !Files.exists( sourceFileContainingError() ) ) {
			// If the file doesn't exist, some of our earlier checks have failed (most probably the source is inside a jar)
			// This is kind of a catch-all, we should be figuring out what the error conditions are and handling those instead.
			return false;
		}

		return true;
	}

	/**
	 * @return The source file where the exception originated (from the last line of the stack trace).
	 */
	private Path sourceFileContainingError() {
		final String nameOfThrowingClass = firstLineOfTrace().getFileName();
		final String projectRoot = projectRootForClassName( firstLineOfTrace().getClassName() );
		final String sourceFolder = projectRoot + "src/main/java/";
		final String path = sourceFolder + packageNameFromClassName( firstLineOfTrace().getClassName() ).replace( ".", "/" ) + "/" + nameOfThrowingClass;
		return Paths.get( path );
	}

	/**
	 * An extremely hacky method to get to our project root to locate the source file.
	 * Should really be using some sort of bundle functionality to resolve this instead in the future.
	 */
	private static String projectRootForClassName( final String className ) {
		final String classFilename = className.replace( '.', '/' ) + ".class";
		final URL classURL = NGExceptionPageDevelopment.class.getClassLoader().getResource( classFilename );
		final String filename = classURL.getFile().toString();

		// If the source is inside a JAR file, we don't have a project root and currently don't display anything.
		// We might want to consider looking inside source jars?
		if( filename.contains( ".jar!/" ) ) {
			return null;
		}

		final int targetIndex = filename.indexOf( "target/" ); // We're assuming a maven project with the standard "target" name for the build folder

		if( targetIndex == -1 ) {
			return null;
		}

		return filename.substring( 0, targetIndex );
	}

	/**
	 * @return the package name from a fully qualified class name
	 */
	private static String packageNameFromClassName( final String className ) {
		Objects.requireNonNull( className );
		return className.substring( 0, className.lastIndexOf( '.' ) );
	}

	/**
	 * @return The source lines to view in the browser.
	 */
	public List<String> lines() {
		final List<String> lines;

		final Path sourceFileContainingError = sourceFileContainingError();

		if( !Files.exists( sourceFileContainingError ) ) {
			return Collections.emptyList();
		}

		try {
			lines = Files.readAllLines( sourceFileContainingError );
		}
		catch( IOException e ) {
			logger.error( "Attempt to read source code from '{}' failed", sourceFileContainingError, e );
			return new ArrayList<>();
		}

		int indexOfFirstLineToShow = firstLineOfTrace().getLineNumber() - NUMBER_OF_LINES_BEFORE_ERROR_LINE;
		int indexOfLastLineToShow = firstLineOfTrace().getLineNumber() + NUMBER_OF_LINES_AFTER_ERROR_LINE;

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
		return firstLineOfTrace().getLineNumber() - NUMBER_OF_LINES_BEFORE_ERROR_LINE + currentSourceLineIndex + 1;
	}

	/**
	 * @return CSS class for the current line of the source file (to show odd/even lines and highlight the error line)
	 */
	public String sourceLineClass() {
		final List<String> cssClasses = new ArrayList<>();
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

	/**
	 * FIXME: InvocationTargetException really just wraps the useful exception, so we show that
	 * We might want to offer a better way to "browse" the exception's hierarchy? // Hugi 2024-04-10
	 */
	public void setException( Throwable value ) {

		// FIXME: This should probably not be like this. We need to figure out proper display when it comes to exception wrapping // Hugi 2024-04-25
		while( value.getCause() != null ) {
			value = value.getCause();
		}

		// InvocationTargetException really just wraps the useful exception, so show that
		if( value instanceof InvocationTargetException ite ) {
			_exception = ite.getTargetException();
		}

		_exception = value;
	}

	/**
	 * @return The CSS class of the current row in the stack trace table.
	 */
	public String currentRowClass() {
		return null;
	}
}