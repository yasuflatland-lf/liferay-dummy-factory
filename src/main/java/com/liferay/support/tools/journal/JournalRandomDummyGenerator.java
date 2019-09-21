package com.liferay.support.tools.journal;

import com.liferay.journal.model.JournalArticle;
import com.liferay.journal.service.JournalArticleLocalService;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.support.tools.constants.LDFPortletKeys;
import com.liferay.support.tools.utils.ProgressManager;
import com.liferay.support.tools.utils.RandomizeContentGenerator;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.portlet.ActionRequest;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Random Journal Articles Generator
 * 
 * @author Yasuyuki Takeo
 *
 */
@Component(immediate = true, service = JournalRandomDummyGenerator.class)
public class JournalRandomDummyGenerator extends JournalStructureBaseDummyGenerator {

	@Override
	protected JournalContext getContext(ActionRequest request) throws PortalException {
		
		return new JournalContext(request);
	}

	@Override
	protected void exec(ActionRequest request, JournalContext paramContext) throws Exception {
		// Tracking progress start
		ProgressManager progressManager = new ProgressManager();
		progressManager.start(request);

		long progressCount = 0;
		long[] groupIds = paramContext.getGroupIds();
		
		for (long groupId : groupIds) {
			System.out.println(
					"Starting to create " + paramContext.getNumberOfArticles() + " articles for site id <"
					+ _groupLocalService.getGroup(groupId).getDescriptiveName() + ">");

			for (long i = 1; i <= paramContext.getNumberOfArticles(); i++) {
				// Update progress
				progressManager.trackProgress(
					progressCount, 
					paramContext.getNumberOfArticles()*groupIds.length
				);

				// Build title
				StringBundler title = new StringBundler(2);

				title.append(
					_randomizeContentGenerator.generateRandomTitleString(
						paramContext.getDefaultLocale().getLanguage(),
						paramContext.getTitleWords()
					)
				);

				// Add number more then one article
				if (1 < paramContext.getNumberOfArticles()) {
					title.append(i);
				}

				// Build contents
				String article = 
					_randomizeContentGenerator.generateRandomContents(
						paramContext.getDefaultLocale().getLanguage(),
						paramContext.getTotalParagraphs(), 
						paramContext.getRandomAmount(), 
						paramContext.getLinkLists()
					);						
				
				String content = 
					_journalUtils.buildFields(
						paramContext.getThemeDisplay().getCompanyGroupId(), 
						paramContext.getLocales(), 
						article
					);

				Map<Locale, String> titleMap = new ConcurrentHashMap<Locale, String>();
				titleMap.put(
					paramContext.getDefaultLocale(), 
					title.toString()
				);

				try {
					// Create article
					JournalArticle createdArticle = _journalArticleLocalService.addArticle(
						paramContext.getServiceContext().getUserId(), 	// userId,
						groupId, 										// groupId,
						paramContext.getFolderId(),						// folderId
						titleMap, 										// titleMap
						paramContext.getDescriptionMap(),				// descriptionMap
						content, 										// content
						LDFPortletKeys._DDM_STRUCTURE_KEY,				// ddmStructureKey,
						LDFPortletKeys._DDM_TEMPLATE_KEY, 				// ddmTemplateKey,
						paramContext.getServiceContext()				// serviceContext
					);
					
					// Update never expired and never reviewed
					updateArticle(createdArticle, paramContext);

				} catch (Throwable e) {
					// Finish progress
					progressManager.finish();
					throw e;
				}
				progressCount++;
			}
		}

		// Finish progress
		progressManager.finish();

		System.out.println("Finished creating " + paramContext.getNumberOfArticles() + " articles");
	}

	@Reference
	private JournalUtils _journalUtils;
	
	@Reference
	private JournalArticleLocalService _journalArticleLocalService;
	
	@Reference
	private GroupLocalService _groupLocalService;	
	
	@Reference
	private RandomizeContentGenerator _randomizeContentGenerator;	

}
