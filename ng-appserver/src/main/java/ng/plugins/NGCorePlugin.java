package ng.plugins;

import ng.appserver.NGApplication;
import ng.appserver.NGComponentRequestHandler;
import ng.appserver.NGResourceRequestHandler;
import ng.appserver.NGResourceRequestHandlerDynamic;
import ng.appserver.directactions.NGDirectActionRequestHandler;
import ng.appserver.templating.elements.NGActionURL;
import ng.appserver.templating.elements.NGBrowser;
import ng.appserver.templating.elements.NGCheckbox;
import ng.appserver.templating.elements.NGComponentContent;
import ng.appserver.templating.elements.NGConditional;
import ng.appserver.templating.elements.NGFileUpload;
import ng.appserver.templating.elements.NGForm;
import ng.appserver.templating.elements.NGGenericContainer;
import ng.appserver.templating.elements.NGGenericElement;
import ng.appserver.templating.elements.NGHyperlink;
import ng.appserver.templating.elements.NGImage;
import ng.appserver.templating.elements.NGJavaScript;
import ng.appserver.templating.elements.NGPasswordField;
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

	/**
	 * The framework's element classes. Their tag names are NOT declared here: they live in
	 * ng-appserver's {@code parsley-tag-aliases.properties}, which NGElementManager loads
	 * and the template editor reads, so both resolve tags identically.
	 */
	@Override
	public Elements elements() {
		return Elements
				.create()
				.elementClass( AjaxObserveField.class )
				.elementClass( AjaxSubmitButton.class )
				.elementClass( AjaxUpdateContainer.class )
				.elementClass( AjaxUpdateLink.class )
				.elementClass( NGActionURL.class )
				.elementClass( NGCheckbox.class )
				.elementClass( NGBrowser.class )
				.elementClass( NGComponentContent.class )
				.elementClass( NGConditional.class )
				.elementClass( NGFileUpload.class )
				.elementClass( NGForm.class )
				.elementClass( NGGenericContainer.class )
				.elementClass( NGGenericElement.class )
				.elementClass( NGHyperlink.class )
				.elementClass( NGImage.class )
				.elementClass( NGJavaScript.class )
				.elementClass( NGPasswordField.class )
				.elementClass( NGPopUpButton.class )
				.elementClass( NGRepetition.class )
				.elementClass( NGResourceURL.class )
				.elementClass( NGString.class )
				.elementClass( NGStylesheet.class )
				.elementClass( NGSubmitButton.class )
				.elementClass( NGSwitchComponent.class )
				.elementClass( NGText.class )
				.elementClass( NGTextField.class );
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