package com.liferay.support.tools.utils;

import com.liferay.petra.string.StringPool;
import com.liferay.wiki.engine.WikiEngine;
import com.liferay.wiki.engine.WikiEngineRenderer;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(immediate = true, service = WikiCommons.class)
public class WikiCommons {
	
	/**
	 * Get Wiki format list
	 *
	 * @param locale
	 * @return available Wiki format list
	 */
	public Map<String, String> getFormats(Locale locale) {
		Collection<String> formats = _wikiEngineRenderer.getFormats();
		Map<String,String> fmt = new LinkedHashMap<>();
		
		for(String format : formats) {
			fmt.put(format,getFormatLabel(_wikiEngineRenderer, format, locale));
		}
		
		return fmt;
	}
	
	@Reference(unbind = "-")
	protected void setWikiEngineRenderer(
		WikiEngineRenderer wikiEngineRenderer) {

		_wikiEngineRenderer = wikiEngineRenderer;
	}

	public String getFormatLabel(
		WikiEngineRenderer wikiEngineRenderer, String format, Locale locale) {

		WikiEngine wikiEngine = wikiEngineRenderer.fetchWikiEngine(format);

		if (wikiEngine != null) {
			return wikiEngine.getFormatLabel(locale);
		}

		return StringPool.BLANK;
	}
	
	private WikiEngineRenderer _wikiEngineRenderer;

}
