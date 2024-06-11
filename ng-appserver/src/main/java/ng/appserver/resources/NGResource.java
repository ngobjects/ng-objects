package ng.appserver.resources;

import java.io.InputStream;

public record NGResource( String name, InputStream inputstream, String mimeType, Long length ) {}