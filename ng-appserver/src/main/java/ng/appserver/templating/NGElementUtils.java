package ng.appserver.templating;

import ng.appserver.NGApplication;
import ng.appserver.elements.NGActionURL;
import ng.appserver.elements.NGBrowser;
import ng.appserver.elements.NGComponentContent;
import ng.appserver.elements.NGConditional;
import ng.appserver.elements.NGForm;
import ng.appserver.elements.NGGenericContainer;
import ng.appserver.elements.NGGenericElement;
import ng.appserver.elements.NGHyperlink;
import ng.appserver.elements.NGImage;
import ng.appserver.elements.NGJavaScript;
import ng.appserver.elements.NGPopUpButton;
import ng.appserver.elements.NGRepetition;
import ng.appserver.elements.NGResourceURL;
import ng.appserver.elements.NGString;
import ng.appserver.elements.NGStylesheet;
import ng.appserver.elements.NGSubmitButton;
import ng.appserver.elements.NGSwitchComponent;
import ng.appserver.elements.NGText;
import ng.appserver.elements.NGTextField;
import ng.appserver.elements.ajax.AjaxObserveField;
import ng.appserver.elements.ajax.AjaxSubmitButton;
import ng.appserver.elements.ajax.AjaxUpdateContainer;
import ng.appserver.elements.ajax.AjaxUpdateLink;

/**
 * An abomination of a class that serves as a repository for temporary hacky stuff
 */

@Deprecated
public class NGElementUtils {

	static {
		addClass( NGActionURL.class, "actionURL" );
		addClass( AjaxUpdateContainer.class, "auc" );
		addClass( AjaxUpdateLink.class, "aul" );
		addClass( AjaxObserveField.class, "aof" );
		addClass( AjaxSubmitButton.class, "asb" );
		addClass( NGBrowser.class, "browser" );
		addClass( NGComponentContent.class, "content" );
		addClass( NGConditional.class, "if" );
		addClass( NGForm.class, "form" );
		addClass( NGString.class, "str" );
		addClass( NGGenericContainer.class, "container" );
		addClass( NGGenericElement.class, "element" );
		addClass( NGImage.class, "img" );
		addClass( NGHyperlink.class, "link" );
		addClass( NGJavaScript.class, "script" );
		addClass( NGPopUpButton.class, "popUpButton" ); // CHECKME: We might want to consider just naming this "popup"
		addClass( NGRepetition.class, "repetition" );
		addClass( NGResourceURL.class, "resourceURL" );
		addClass( NGSubmitButton.class, "submit" );
		addClass( NGStylesheet.class, "stylesheet" );
		addClass( NGSwitchComponent.class, "switch" );
		addClass( NGText.class, "text" );
		addClass( NGTextField.class, "textfield" );
	}

	/**
	 * Add a class to make searchable by it's simpleName, full class name or any of the given shortcuts (for tags)
	 */
	@Deprecated
	public static void addClass( final Class<?> elementClass, final String... tagNames ) {
		NGApplication.application().elementManager().registerElementClass( NGElementManager.GLOBAL_UNNAMESPACED_NAMESPACE, (Class<? extends NGElement>)elementClass, tagNames );
	}

	@Deprecated
	public static void addPackage( final String packageName ) {
		NGApplication.application().elementManager().registerElementPackage( NGElementManager.GLOBAL_UNNAMESPACED_NAMESPACE, packageName );
	}

	/**
	 * @return true if the given object is "truthy" for conditionals
	 *
	 * The only conditions for returning false are:
	 * - Boolean false (box or primitive)
	 * - A number that's exactly zero
	 * - null
	 */
	public static boolean isTruthy( Object object ) {

		if( object == null ) {
			return false;
		}

		if( object instanceof Boolean b ) {
			return b;
		}

		if( object instanceof Number number ) {
			// Note that Number.doubleValue() might return Double.NaN which is... Troublesome. Trying to decide if NaN is true or false is almost a philosophical question.
			// I'm still leaning towards keeping our definition of "only exactly zero is false", meaning NaN is true, making this code currently fine.
			return number.doubleValue() != 0;
		}

		return true;
	}
}