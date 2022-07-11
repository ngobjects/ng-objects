package x.junk;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ng.appserver.NGApplication;
import ng.appserver.NGComponent;
import ng.appserver.NGContext;

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
	 * Line of the stack trace currently being iterated over.
	 */
	public StackTraceElement currentErrorLine;

	public NGExceptionPage( NGContext aContext ) {
		super( aContext );
	}

	public String currentPackageName() {
		return packageNameFromClassName( currentErrorLine.getClassName() );
	}

	public List<StackTraceElement> stackTrace() {
		return Arrays.asList( exception().getStackTrace() );
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
		StackTraceElement[] stackTrace = _exception.getStackTrace();

		if( stackTrace.length == 0 ) {
			return null;
		}

		return stackTrace[0];
	}

	/**
	 * @return true if source should be shown.
	 */
	public boolean showSource() {
		return NGApplication.application().isDevelopmentMode() && sourceFileContainingError() != null && !sourceFileContainingError().toString().contains( ".jar/" );
	}

	/**
	 * @return The source file where the exception originated (from the last line of the stack trace).
	 *
	 * FIXME: We need to locate the correct working directory here
	 */
	private Path sourceFileContainingError() {
		final String nameOfThrowingClass = firstLineOfTrace().getFileName();
		String sourceFolder = "/Users/hugi/git/ng-objects/ng-core/src/main/java/";
		final String path = sourceFolder + packageNameFromClassName( firstLineOfTrace().getClassName() ).replace( ".", "/" ) + "/" + nameOfThrowingClass;
		return Paths.get( path );
	}

	private static String packageNameFromClassName( final String className ) {
		Objects.requireNonNull( className );
		return className.substring( 0, className.lastIndexOf( '.' ) );

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
		_exception = value;
	}

	/**
	 * @return bundle of the class currently being iterated over in the UI (if any)
	 */
	public Object currentBundle() {
		return null;
	}

	/**
	 * @return The CSS class of the current row in the stack trace table.
	 *
	 * FIXME: Identify bundles and return appropriate class
	 */
	public String currentRowClass() {
		return null;
	}
}