package ng.appserver;

public abstract class NGElement {

	public void takeValuesFromRequest( NGRequest request, NGContext context ) {}
	
	public NGActionResults invokeAction( NGRequest request, NGContext context ) {
		return null;
	}
	
	public void appendToResponse( NGResponse response, NGContext context ) {}
}