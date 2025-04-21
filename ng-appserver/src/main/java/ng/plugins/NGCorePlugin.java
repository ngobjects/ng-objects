package ng.plugins;

import ng.appserver.NGApplication;
import ng.appserver.NGComponentRequestHandler;
import ng.appserver.NGResourceRequestHandler;
import ng.appserver.NGResourceRequestHandlerDynamic;
import ng.appserver.directactions.NGDirectActionRequestHandler;
import ng.appserver.elements.NGActionURL;
import ng.appserver.elements.NGBrowser;
import ng.appserver.elements.NGComponentContent;
import ng.appserver.elements.NGConditional;
import ng.appserver.elements.NGFileUpload;
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
import ng.appserver.wointegration.WOMPRequestHandler;

public class NGCorePlugin implements NGPlugin {

	@Override
	public String namespace() {
		return "ng";
	}

	@Override
	public Elements elements() {
		return Elements
				.create()
				.elementClass( NGActionURL.class, "actionURL" )
				.elementClass( AjaxUpdateContainer.class, "auc" )
				.elementClass( AjaxUpdateLink.class, "aul" )
				.elementClass( AjaxObserveField.class, "aof" )
				.elementClass( AjaxSubmitButton.class, "asb" )
				.elementClass( NGBrowser.class, "browser" )
				.elementClass( NGComponentContent.class, "content" )
				.elementClass( NGConditional.class, "if" )
				.elementClass( NGFileUpload.class, "fileUpload" )
				.elementClass( NGForm.class, "form" )
				.elementClass( NGString.class, "str" )
				.elementClass( NGGenericContainer.class, "container" )
				.elementClass( NGGenericElement.class, "element" )
				.elementClass( NGImage.class, "img" )
				.elementClass( NGHyperlink.class, "link" )
				.elementClass( NGJavaScript.class, "script" )
				.elementClass( NGPopUpButton.class, "popUpButton" ) // CHECKME: We might want to consider just naming this "popup"
				.elementClass( NGRepetition.class, "repetition" )
				.elementClass( NGResourceURL.class, "resourceURL" )
				.elementClass( NGSubmitButton.class, "submit" )
				.elementClass( NGStylesheet.class, "stylesheet" )
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
				.map( WOMPRequestHandler.DEFAULT_PATH, new WOMPRequestHandler() )
				.map( "/ng/sessionCookieReset", request -> NGApplication.application().resetSessionCookie() );
	}
}