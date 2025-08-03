package ng.plugins;

import ng.appserver.NGApplication;
import ng.appserver.NGComponentRequestHandler;
import ng.appserver.NGResourceRequestHandler;
import ng.appserver.NGResourceRequestHandlerDynamic;
import ng.appserver.directactions.NGDirectActionRequestHandler;
import ng.appserver.templating.elements.NGActionURL;
import ng.appserver.templating.elements.NGBrowser;
import ng.appserver.templating.elements.NGComponentContent;
import ng.appserver.templating.elements.NGConditional;
import ng.appserver.templating.elements.NGFileUpload;
import ng.appserver.templating.elements.NGForm;
import ng.appserver.templating.elements.NGGenericContainer;
import ng.appserver.templating.elements.NGGenericElement;
import ng.appserver.templating.elements.NGHyperlink;
import ng.appserver.templating.elements.NGImage;
import ng.appserver.templating.elements.NGJavaScript;
import ng.appserver.templating.elements.NGPopUpButton;
import ng.appserver.templating.elements.NGRepetition;
import ng.appserver.templating.elements.NGResourceURL;
import ng.appserver.templating.elements.NGString;
import ng.appserver.templating.elements.NGStylesheet;
import ng.appserver.templating.elements.NGSubmitButton;
import ng.appserver.templating.elements.NGSwitchComponent;
import ng.appserver.templating.elements.NGText;
import ng.appserver.templating.elements.NGTextField;
import ng.appserver.templating.elements.ajax.AjaxObserveField;
import ng.appserver.templating.elements.ajax.AjaxSubmitButton;
import ng.appserver.templating.elements.ajax.AjaxUpdateContainer;
import ng.appserver.templating.elements.ajax.AjaxUpdateLink;

public class NGCorePlugin implements NGPlugin {

	@Override
	public String namespace() {
		return "ng";
	}

	@Override
	public Elements elements() {
		return Elements
				.create()
				.elementClass( AjaxObserveField.class, "aof" )
				.elementClass( AjaxSubmitButton.class, "asb" )
				.elementClass( AjaxUpdateContainer.class, "auc" )
				.elementClass( AjaxUpdateLink.class, "aul" )
				.elementClass( NGActionURL.class, "actionURL" )
				.elementClass( NGBrowser.class, "browser" )
				.elementClass( NGComponentContent.class, "content" )
				.elementClass( NGConditional.class, "if" )
				.elementClass( NGFileUpload.class, "fileUpload" )
				.elementClass( NGForm.class, "form" )
				.elementClass( NGGenericContainer.class, "container" )
				.elementClass( NGGenericElement.class, "element" )
				.elementClass( NGHyperlink.class, "link" )
				.elementClass( NGImage.class, "img" )
				.elementClass( NGJavaScript.class, "script" )
				.elementClass( NGPopUpButton.class, "popUpButton" ) // CHECKME: We might want to consider just naming this "popup"
				.elementClass( NGRepetition.class, "repetition" )
				.elementClass( NGResourceURL.class, "resourceURL" )
				.elementClass( NGString.class, "str" )
				.elementClass( NGStylesheet.class, "stylesheet" )
				.elementClass( NGSubmitButton.class, "submit" )
				.elementClass( NGSwitchComponent.class, "switch" )
				.elementClass( NGText.class, "text" )
				.elementClass( NGTextField.class, "textfield" );
	}

	@Override
	public Routes routes() {
		return Routes
				.create()
				.map( "/", request -> NGApplication.application().defaultResponse( request ) )
				.map( NGComponentRequestHandler.DEFAULT_PATH + "*", new NGComponentRequestHandler() )
				.map( NGResourceRequestHandler.DEFAULT_PATH + "*", new NGResourceRequestHandler() )
				.map( NGResourceRequestHandlerDynamic.DEFAULT_PATH + "*", new NGResourceRequestHandlerDynamic() )
				.map( NGDirectActionRequestHandler.DEFAULT_PATH + "*", new NGDirectActionRequestHandler() )
				.map( "/ng/sessionCookieReset", request -> NGApplication.application().resetSessionCookie() );
	}
}