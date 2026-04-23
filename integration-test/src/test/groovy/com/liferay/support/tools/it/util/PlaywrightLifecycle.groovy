package com.liferay.support.tools.it.util

import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserContext
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright

class PlaywrightLifecycle implements Closeable {

	final Playwright playwright
	final Browser browser
	BrowserContext context
	Page page

	PlaywrightLifecycle() {
		playwright = Playwright.create()
		browser = playwright.chromium().launch(
			new BrowserType.LaunchOptions().setHeadless(true)
		)
	}

	Page newPage() {
		context?.close()

		context = browser.newContext(
			new Browser.NewContextOptions().setViewportSize(1280, 720)
		)
		page = context.newPage()

		return page
	}

	@Override
	void close() {
		context?.close()
		browser.close()
		playwright.close()
	}

}
