package er.extensions.bettertemplates;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NGHelperFunctionTagRegistry {
	public static Logger log = LoggerFactory.getLogger( NGHelperFunctionTagRegistry.class );

	private static NSMutableDictionary _tagShortcutMap = new NSMutableDictionary();
	private static NSMutableDictionary _tagProcessorMap = new NSMutableDictionary();
	private static boolean _allowInlineBindings = true;

	public static NSDictionary tagShortcutMap() {
		return _tagShortcutMap;
	}

	public static NSDictionary tagProcessorMap() {
		return _tagProcessorMap;
	}

	public static void registerTagShortcut( String fullElementType, String shortcutElementType ) {
		_tagShortcutMap.setObjectForKey( fullElementType, shortcutElementType );
	}

	public static void registerTagProcessorForElementType( NGTagProcessor tagProcessor, String elementType ) {
		_tagProcessorMap.setObjectForKey( tagProcessor, elementType );
	}

	public static void setAllowInlineBindings( boolean allowInlineBindings ) {
		_allowInlineBindings = allowInlineBindings;
	}

	public static boolean allowInlineBindings() {
		return _allowInlineBindings;
	}

	static {
		// 		FIXME: Disabled on switch to slf4j // Hugi 2022-01-05
		//		WOHelperFunctionTagRegistry.log.setLevel(Level.WARN);

		NGHelperFunctionTagRegistry.registerTagShortcut( "ERXLocalizedString", "localized" ); // not in 5.4

		NGHelperFunctionTagRegistry.registerTagShortcut( "ERXElse", "else" );
		NGHelperFunctionTagRegistry.registerTagShortcut( "ERXWOConditional", "if" );
		NGHelperFunctionTagRegistry.registerTagShortcut( "ERXWOConditional", "conditional" );
		NGHelperFunctionTagRegistry.registerTagShortcut( "ERXWOConditional", "condition" ); // not in 5.4
		NGHelperFunctionTagRegistry.registerTagShortcut( "WORepetition", "foreach" );
		NGHelperFunctionTagRegistry.registerTagShortcut( "WORepetition", "repeat" );
		NGHelperFunctionTagRegistry.registerTagShortcut( "WORepetition", "repetition" );
		NGHelperFunctionTagRegistry.registerTagShortcut( "WORepetition", "loop" ); // not in 5.4
		NGHelperFunctionTagRegistry.registerTagShortcut( "WOComponentContent", "content" );
		NGHelperFunctionTagRegistry.registerTagShortcut( "WOComponentContent", "componentContent" );
		NGHelperFunctionTagRegistry.registerTagShortcut( "NGString", "str" ); // not in 5.4
		NGHelperFunctionTagRegistry.registerTagShortcut( "WOString", "string" );
		NGHelperFunctionTagRegistry.registerTagShortcut( "WOSwitchComponent", "switchComponent" );
		NGHelperFunctionTagRegistry.registerTagShortcut( "WOSwitchComponent", "switch" );
		NGHelperFunctionTagRegistry.registerTagShortcut( "WOXMLNode", "XMLNode" );
		NGHelperFunctionTagRegistry.registerTagShortcut( "WONestedList", "nestedList" );
		NGHelperFunctionTagRegistry.registerTagShortcut( "WOParam", "param" );
		NGHelperFunctionTagRegistry.registerTagShortcut( "WOApplet", "applet" );
		NGHelperFunctionTagRegistry.registerTagShortcut( "WOQuickTime", "quickTime" );
		NGHelperFunctionTagRegistry.registerTagShortcut( "WOHTMLCommentString", "commentString" );
		NGHelperFunctionTagRegistry.registerTagShortcut( "WOHTMLCommentString", "comment" );
		NGHelperFunctionTagRegistry.registerTagShortcut( "WONoContentElement", "noContentElement" );
		NGHelperFunctionTagRegistry.registerTagShortcut( "WONoContentElement", "noContent" );
		NGHelperFunctionTagRegistry.registerTagShortcut( "WOBody", "body" );
		NGHelperFunctionTagRegistry.registerTagShortcut( "WOEmbeddedObject", "embeddedObject" );
		NGHelperFunctionTagRegistry.registerTagShortcut( "WOEmbeddedObject", "embedded" );
		NGHelperFunctionTagRegistry.registerTagShortcut( "WOFrame", "frame" );
		NGHelperFunctionTagRegistry.registerTagShortcut( "WOImage", "image" );
		NGHelperFunctionTagRegistry.registerTagShortcut( "WOImage", "img" ); // not in 5.4
		NGHelperFunctionTagRegistry.registerTagShortcut( "WOForm", "form" );
		NGHelperFunctionTagRegistry.registerTagShortcut( "WOJavaScript", "javaScript" );
		NGHelperFunctionTagRegistry.registerTagShortcut( "WOVBScript", "VBScript" );
		NGHelperFunctionTagRegistry.registerTagShortcut( "WOResourceURL", "resourceURL" );
		NGHelperFunctionTagRegistry.registerTagShortcut( "WOGenericElement", "genericElement" );
		NGHelperFunctionTagRegistry.registerTagShortcut( "WOGenericElement", "element" );
		NGHelperFunctionTagRegistry.registerTagShortcut( "WOGenericContainer", "genericContainer" );
		NGHelperFunctionTagRegistry.registerTagShortcut( "WOGenericContainer", "container" );
		NGHelperFunctionTagRegistry.registerTagShortcut( "WOActiveImage", "activeImage" );
		NGHelperFunctionTagRegistry.registerTagShortcut( "WOCheckBox", "checkBox" );
		NGHelperFunctionTagRegistry.registerTagShortcut( "WOCheckBox", "checkbox" ); // not in 5.4 (5.4 is case insensitive)
		NGHelperFunctionTagRegistry.registerTagShortcut( "WOFileUpload", "fileUpload" );
		NGHelperFunctionTagRegistry.registerTagShortcut( "WOFileUpload", "upload" );
		NGHelperFunctionTagRegistry.registerTagShortcut( "WOHiddenField", "hiddenField" );
		NGHelperFunctionTagRegistry.registerTagShortcut( "WOHiddenField", "hidden" ); // not in 5.4
		NGHelperFunctionTagRegistry.registerTagShortcut( "WOImageButton", "imageButton" );
		NGHelperFunctionTagRegistry.registerTagShortcut( "WOInputList", "inputList" );
		NGHelperFunctionTagRegistry.registerTagShortcut( "WOBrowser", "browser" );
		NGHelperFunctionTagRegistry.registerTagShortcut( "WOCheckBoxList", "checkBoxList" );
		NGHelperFunctionTagRegistry.registerTagShortcut( "WOPopUpButton", "popUpButton" );
		NGHelperFunctionTagRegistry.registerTagShortcut( "WOPopUpButton", "select" ); // not in 5.4
		NGHelperFunctionTagRegistry.registerTagShortcut( "WORadioButtonList", "radioButtonList" );
		NGHelperFunctionTagRegistry.registerTagShortcut( "WOPasswordField", "passwordField" );
		NGHelperFunctionTagRegistry.registerTagShortcut( "WOPasswordField", "password" );
		NGHelperFunctionTagRegistry.registerTagShortcut( "WORadioButton", "radioButton" );
		NGHelperFunctionTagRegistry.registerTagShortcut( "WORadioButton", "radio" );
		NGHelperFunctionTagRegistry.registerTagShortcut( "WOResetButton", "resetButton" );
		NGHelperFunctionTagRegistry.registerTagShortcut( "WOResetButton", "reset" );
		NGHelperFunctionTagRegistry.registerTagShortcut( "WOSubmitButton", "submitButton" );
		NGHelperFunctionTagRegistry.registerTagShortcut( "WOSubmitButton", "submit" );
		NGHelperFunctionTagRegistry.registerTagShortcut( "WOText", "text" );
		NGHelperFunctionTagRegistry.registerTagShortcut( "WOTextField", "textField" );
		NGHelperFunctionTagRegistry.registerTagShortcut( "WOTextField", "textfield" ); // not in 5.4 (5.4 is case insensitive)
		NGHelperFunctionTagRegistry.registerTagShortcut( "WOSearchField", "search" );
		NGHelperFunctionTagRegistry.registerTagShortcut( "WOSearchField", "searchfield" );
		NGHelperFunctionTagRegistry.registerTagShortcut( "WOHyperlink", "hyperlink" );
		NGHelperFunctionTagRegistry.registerTagShortcut( "WOHyperlink", "link" );
		NGHelperFunctionTagRegistry.registerTagShortcut( "WOActionURL", "actionURL" );

		//		NGHelperFunctionTagRegistry.registerTagProcessorForElementType( new NotTagProcessor(), "not" ); // FIXME: Look into this later
	}

}
