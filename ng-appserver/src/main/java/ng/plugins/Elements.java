package ng.plugins;

import java.util.ArrayList;
import java.util.List;

import ng.appserver.templating.NGElement;
import ng.appserver.templating.NGElementManager.ElementAliases;
import ng.appserver.templating.NGElementManager.ElementByClass;
import ng.appserver.templating.NGElementManager.ElementProvider;
import ng.appserver.templating.NGElementManager.ElementsByPackage;

/**
 * API for constructing a list of ways to locate element classes
 */
public class Elements {

	/**
	 * List of methods we've listed to locate elements
	 */
	private List<ElementProvider> _elementProviders = new ArrayList<>();

	private Elements() {}

	public static Elements create() {
		return new Elements();
	}

	public Elements elementClass( Class<? extends NGElement> elementClass, String... tagNames ) {
		_elementProviders.add( new ElementByClass( null, elementClass, tagNames ) );
		return this;
	}

	public Elements elementPackage( String packageName ) {
		_elementProviders.add( new ElementsByPackage( null, packageName ) );
		return this;
	}

	public Elements elementAlias( String tagName, String... tagAliases ) {
		_elementProviders.add( new ElementAliases( tagName, tagAliases ) );
		return this;
	}

	public List<ElementProvider> elementProviders() {
		return _elementProviders;
	}
}