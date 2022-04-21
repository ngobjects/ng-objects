package ng.appserver.templating;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.StringTokenizer;

import ng.appserver.NGApplication;
import ng.appserver.NGComponent;
import ng.appserver.NGComponentDefinition;
import ng.appserver.NGComponentReference;
import ng.appserver.NGDynamicElement;
import ng.appserver.NGElement;
import ng.appserver.elements.NGDynamicGroup;
import ng.appserver.elements.NGHTMLBareString;

public class NGHTMLWebObjectTag {
	private String _name;
	private NGHTMLWebObjectTag _parent;
	private List _children;

	private void extractName( String s ) throws NGHelperFunctionHTMLFormatException {

		StringTokenizer stringtokenizer = new StringTokenizer( s, "=" );
		if( stringtokenizer.countTokens() != 2 ) {
			throw new NGHelperFunctionHTMLFormatException( "<WOHTMLWebObjectTag cannot initialize WebObject tag " + s + "> . It has no NAME=... parameter" );
		}

		stringtokenizer.nextToken();
		String s1 = stringtokenizer.nextToken();

		int i = s1.indexOf( '"' );
		if( i != -1 ) {
			int j = s1.lastIndexOf( '"' );
			if( j > i ) {
				_name = s1.substring( i + 1, j );
			}
		}
		else {
			StringTokenizer stringtokenizer1 = new StringTokenizer( s1 );
			_name = stringtokenizer1.nextToken();
		}

		if( _name == null ) {
			throw new NGHelperFunctionHTMLFormatException( "<WOHTMLWebObjectTag cannot initialize WebObject tag " + s + "> . Failed parsing NAME parameter" );
		}
	}

	public NGHTMLWebObjectTag() {
		_name = null;
	}

	public NGHTMLWebObjectTag( String s, NGHTMLWebObjectTag wohtmlwebobjecttag ) throws NGHelperFunctionHTMLFormatException {
		_parent = wohtmlwebobjecttag;
		extractName( s );
	}

	public String name() {
		return _name;
	}

	public NGHTMLWebObjectTag parentTag() {
		return _parent;
	}

	public NGElement template() {
		List nsmutablearray = null;
		if( _children == null ) {
			return null;
		}
		Enumeration enumeration = Collections.enumeration( _children );
		if( enumeration != null ) {
			nsmutablearray = new ArrayList( _children.size() );
			StringBuilder stringbuffer = new StringBuilder( 128 );
			while( enumeration.hasMoreElements() ) {
				Object obj1 = enumeration.nextElement();
				if( obj1 instanceof String ) {
					stringbuffer.append( (String)obj1 );
				}
				else {
					if( stringbuffer.length() > 0 ) {
						NGHTMLBareString wohtmlbarestring1 = new NGHTMLBareString( stringbuffer.toString() );
						nsmutablearray.add( wohtmlbarestring1 );
						stringbuffer.setLength( 0 );
					}
					nsmutablearray.add( obj1 );
				}
			}
			if( stringbuffer.length() > 0 ) {
				NGHTMLBareString wohtmlbarestring = new NGHTMLBareString( stringbuffer.toString() );
				stringbuffer.setLength( 0 );
				nsmutablearray.add( wohtmlbarestring );
			}
		}
		NGElement obj = null;
		if( nsmutablearray != null && nsmutablearray.size() == 1 ) {
			Object obj2 = nsmutablearray.get( 0 );
			if( obj2 instanceof NGComponentReference ) {
				obj = new NGDynamicGroup( _name, null, (NGElement)obj2 );
			}
			else {
				obj = (NGElement)obj2;
			}
		}
		else {
			obj = new NGDynamicGroup( _name, null, nsmutablearray );
		}
		return obj;
	}

	public void addChildElement( Object obj ) {
		if( _children == null ) {
			_children = new ArrayList();
		}
		_children.add( obj );
	}

	public NGElement dynamicElement( _NSDictionary nsdictionary, List nsarray ) throws NGHelperFunctionDeclarationFormatException, ClassNotFoundException {
		String s = name();
		NGElement woelement = template();
		NGDeclaration wodeclaration = (NGDeclaration)nsdictionary.objectForKey( s );
		return _elementWithDeclaration( wodeclaration, s, woelement, nsarray );
	}

	private static NGElement _componentReferenceWithClassNameDeclarationAndTemplate( String s, NGDeclaration wodeclaration, NGElement woelement, List nsarray ) throws ClassNotFoundException {
		NGComponentReference wocomponentreference = null;
		NGComponentDefinition wocomponentdefinition = NGApplication.application()._componentDefinition( s, nsarray );
		if( wocomponentdefinition != null ) {
			_NSDictionary nsdictionary = wodeclaration.associations();
			wocomponentreference = wocomponentdefinition.componentReferenceWithAssociations( nsdictionary, woelement );
		}
		else {
			throw new ClassNotFoundException( "Cannot find class or component named \'" + s + "\" in runtime or in a loadable bundle" );
		}
		return wocomponentreference;
	}

	private static NGElement _elementWithClass( Class c, NGDeclaration declaration, NGElement element ) {
		return NGApplication.application().dynamicElementWithName( c.getName(), declaration.associations(), element, null );
	}

	private static NGElement _elementWithDeclaration( NGDeclaration wodeclaration, String s, NGElement woelement, List nsarray ) throws ClassNotFoundException, NGHelperFunctionDeclarationFormatException {
		NGElement woelement1 = null;

		if( wodeclaration != null ) {
			String s1 = wodeclaration.type();
			if( s1 != null ) {
				if( _NSLog.debugLoggingAllowedForLevelAndGroups( 3, 8388608L ) ) {
					_NSLog.debug.appendln( "<WOHTMLWebObjectTag> will look for " + s1 + " in the java runtime." );
				}
				Class class1 = _NGUtilities.classWithName( s1 );
				if( class1 == null ) {
					if( _NSLog.debugLoggingAllowedForLevelAndGroups( 3, 8388608L ) ) {
						_NSLog.debug.appendln( "<WOHTMLWebObjectTag> will look for com.webobjects.appserver._private." + s1 + " ." );
					}
					class1 = _NGUtilities.lookForClassInAllBundles( s1 );
					if( class1 == null ) {
						_NSLog.err.appendln( "WOBundle.lookForClassInAllBundles(" + s1 + ") failed!" );
					}
					else

					if( !(NGDynamicElement.class).isAssignableFrom( class1 ) ) {
						class1 = null;
					}
				}

				if( class1 != null ) {
					if( _NSLog.debugLoggingAllowedForLevelAndGroups( 3, 8388608L ) ) {
						_NSLog.debug.appendln( "<WOHTMLWebObjectTag> Will initialize object of class " + s1 );
					}
					if( (NGComponent.class).isAssignableFrom( class1 ) ) {
						if( _NSLog.debugLoggingAllowedForLevelAndGroups( 3, 8388608L ) ) {
							_NSLog.debug.appendln( "<WOHTMLWebObjectTag> will look for " + s1 + " in the Compiled Components." );
						}
						woelement1 = _componentReferenceWithClassNameDeclarationAndTemplate( s1, wodeclaration, woelement, nsarray );
					}
					else {
						woelement1 = _elementWithClass( class1, wodeclaration, woelement );
					}
				}
				else {
					if( _NSLog.debugLoggingAllowedForLevelAndGroups( 3, 8388608L ) ) {
						_NSLog.debug.appendln( "<WOHTMLWebObjectTag> will look for " + s1 + " in the Frameworks." );
					}
					woelement1 = _componentReferenceWithClassNameDeclarationAndTemplate( s1, wodeclaration, woelement, nsarray );
				}
			}
			else {
				throw new NGHelperFunctionDeclarationFormatException( "<WOHTMLWebObjectTag> declaration object for dynamic element (or component) named " + s + "has no class name." );
			}
		}
		else {
			throw new NGHelperFunctionDeclarationFormatException( "<WOHTMLTemplateParser> no declaration for dynamic element (or component) named " + s );
		}

		// FIXME: I'm not sure what this is supposed to do. Work that out!
		// NGGenerationSupport.insertInElementsTableWithName( woelement1, s, wodeclaration.associations() );

		return woelement1;
	}
}