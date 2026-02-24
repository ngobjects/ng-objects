package ng.testapp;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

import ng.appserver.NGApplication;

/**
 * Playwright-based integration tests for the framework's client-side Ajax support.
 *
 * Boots the ng-testapp application on port 1200, then uses a headless browser
 * to exercise AjaxUpdateLink, AjaxUpdateContainer, AjaxObserveField and friends.
 */
public class TestAjax {

	private static final String BASE_URL = "http://localhost:1200";

	// Shared across all tests — expensive to create
	static Playwright playwright;
	static Browser browser;

	// Fresh per test — full isolation (cookies, storage, etc.)
	BrowserContext context;
	Page page;

	@BeforeAll
	static void startAppAndBrowser() {
		// Boot the ng-testapp server (blocks until Jetty is listening)
		NGApplication.runAndReturn( new String[0], Application.class );

		playwright = Playwright.create();
		browser = playwright.chromium().launch();
	}

	@AfterAll
	static void closeBrowser() {
		if( playwright != null ) {
			playwright.close();
		}
	}

	@BeforeEach
	void createPage() {
		context = browser.newContext();
		page = context.newPage();
	}

	@AfterEach
	void closePage() {
		context.close();
	}

	// --- Basic page load -----------------------------------------------------

	@Test
	void ajaxPageLoads() {
		page.navigate( BASE_URL + "/ajax" );
		assertThat( page ).hasTitle( "Welcome to ng" );
		assertThat( page.getByText( "Partial page updates (Ajax)" ) ).isVisible();
	}

	// --- AjaxUpdateLink + AjaxUpdateContainer --------------------------------

	@Test
	void ajaxUpdateLinkUpdatesContainer() {
		page.navigate( BASE_URL + "/ajax" );

		// Click the "Show current time" link
		page.getByText( "Show current time" ).click();

		// The container should now contain a time string
		assertThat( page.locator( "#timeUC" ) ).containsText( "The current time is" );
	}

	@Test
	void ajaxUpdateLinkUpdatesMultipleContainers() {
		page.navigate( BASE_URL + "/ajax" );

		// Click the "Update both" link
		page.getByText( "Update both" ).click();

		// Both containers should now contain time strings
		assertThat( page.locator( "#uc1" ) ).containsText( "The current time is" );
		assertThat( page.locator( "#uc2" ) ).containsText( "The current time is" );
	}

	// --- AjaxSubmitButton ----------------------------------------------------

	@Test
	void ajaxSubmitButtonSubmitsFormAndUpdatesContainer() {
		page.navigate( BASE_URL + "/ajax" );

		// NGTextField renders as a plain <input type="text"> without an id.
		// The "Say hello" button is in the same form, so locate the text field relative to it.
		final var submitButton = page.locator( "input[value='Say hello']" );
		submitButton.locator( "xpath=preceding-sibling::input[@type='text']" ).fill( "Playwright" );
		submitButton.click();

		// The container should greet us
		assertThat( page.locator( "#helloUC" ) ).containsText( "Hello Playwright" );
	}

	// --- AjaxObserveField (single-field mode) --------------------------------

	@Test
	void observeFieldSingleModeEchosInput() {
		page.navigate( BASE_URL + "/ajax" );

		// Type into the observed input field
		page.locator( "#observeInput" ).fill( "hello world" );

		// The echo container should update after debounce (default 300ms)
		assertThat( page.locator( "#observeEcho" ) ).containsText( "Echo: hello world" );
	}

	// --- AjaxObserveField (container mode) -----------------------------------

	@Test
	void observeFieldContainerModeObservesDescendants() {
		page.navigate( BASE_URL + "/ajax" );

		// Type into Field A
		page.locator( "#containerFieldA" ).fill( "alpha" );

		// The container echo should reflect Field A's value
		assertThat( page.locator( "#containerEcho" ) ).containsText( "A=alpha" );

		// Now type into Field B
		page.locator( "#containerFieldB" ).fill( "beta" );

		// The container echo should reflect both values
		assertThat( page.locator( "#containerEcho" ) ).containsText( "A=alpha, B=beta" );
	}
}
