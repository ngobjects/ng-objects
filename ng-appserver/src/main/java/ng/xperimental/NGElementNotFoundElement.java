package ng.xperimental;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Objects;

import ng.appserver.NGActionResults;
import ng.appserver.NGApplication;
import ng.appserver.NGContext;
import ng.appserver.NGElement;
import ng.appserver.NGRequest;
import ng.appserver.NGResponse;

public class NGElementNotFoundElement implements NGElement {

	private String _type;

	public NGElementNotFoundElement( final String type ) {
		_type = type;
	}

	@Override
	public void appendToResponse( NGResponse response, NGContext context ) {
		String s = """
				<a href="%s" style="display: inline-block; padding: 10px; margin: 10px; border: 2px solid rgba(50,50,200,0.6); box-shadow: 4px 4px 1px red; background-color: rgba(0,0,200,0.5); border-radius: 4px; text-decoration: none; color: white">
					Can't find an element/component '<strong>%s</strong>'. Would you like to create it?
				</a>
				""".formatted( context.componentActionURL() + "?elementName=" + _type, _type );

		response.appendContentString( s );
	};

	@Override
	public NGActionResults invokeAction( NGRequest request, NGContext context ) {

		if( context.currentElementIsSender() ) {
			System.out.println( "Let's create that component!" );

			final String packageName = NGApplication.application().getClass().getPackageName() + ".elements";
			final String elementName = request.formValueForKey( "elementName" );
			final String elementString = """
					package %s;

					import java.util.Map;

					import ng.appserver.NGAssociation;
					import ng.appserver.NGContext;
					import ng.appserver.NGDynamicElement;
					import ng.appserver.NGElement;
					import ng.appserver.NGResponse;

					/**
					 * Your new element
					 */

					public class %s extends NGDynamicElement {

						public %s( String name, Map<String, NGAssociation> associations, NGElement contentTemplate ) {
							super( null, null, null );
						}

						@Override
						public void appendToResponse( final NGResponse response, final NGContext context ) {
							response.appendContentString( "Congratulations on your new element!" );
						}
					}
										""".formatted( packageName, elementName, elementName, elementName );

			final Path root = Paths.get( "." ).normalize().toAbsolutePath();
			final Path dirPath = Paths.get( root.toString(), "src", "main", "java", "smu", "elements" ); // FIXME: Split the package name elements into corresponding directories
			final Path filePath = Paths.get( dirPath.toString(), elementName + ".java" ); // FIXME: Split the package name elements into corresponding directories

			System.out.println( "Creating new dynamic element at %s".formatted( filePath ) );

			try {
				Files.createDirectories( dirPath );
				Files.write( filePath, elementString.getBytes(), StandardOpenOption.CREATE_NEW );
				touch( Paths.get( root.toString(), ".project" ) );
			}
			catch( IOException e ) {
				throw new UncheckedIOException( e );
			}
		}

		return NGElement.super.invokeAction( request, context );
	}

	public static void touch( final Path path ) throws IOException {
		Objects.requireNonNull( path, "path is null" );
		try {
			Files.createFile( path );
		}
		catch( FileAlreadyExistsException e ) {
			Files.setLastModifiedTime( path, FileTime.from( Instant.now() ) );
		}
	}
};