package ng.testapp.da;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.GsonBuilder;

import ng.appserver.NGActionResults;
import ng.appserver.NGRequest;
import ng.appserver.NGResponse;
import ng.appserver.directactions.NGDirectAction;

public class JSONAction extends NGDirectAction {

	public JSONAction( NGRequest request ) {
		super( request );
	}

	public NGActionResults jsonAction() {
		final List<Person> people = new ArrayList<>();
		people.add( new Person( "Hugi Þórðarson", "Hraunteigur 23" ) );
		people.add( new Person( "Ósk Gunnlaugsdóttir", "Þjórsárgata 6" ) );
		final var jsonString = new GsonBuilder().setPrettyPrinting().create().toJson( people );
		final var response = new NGResponse( jsonString, 200 );
		response.setHeader( "content-type", "application/json" );
		return response;
	}

	private record Person( String name, String address ) {}
}