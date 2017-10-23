package com.liferay.support.tools.journal;

import com.liferay.dynamic.data.mapping.service.DDMStructureLocalService;
import com.liferay.dynamic.data.mapping.service.DDMTemplateLocalService;
import com.liferay.journal.service.JournalArticleLocalService;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.support.tools.common.DummyGenerator;
import com.liferay.support.tools.utils.ProgressManager;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.portlet.ActionRequest;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Structure / Template selected Journal Articles Generator
 * 
 * @author Yasuyuki Takeo
 *
 */
@Component(immediate = true, service = JournalStructureTemplateDummyGenerator.class)
public class JournalStructureTemplateDummyGenerator extends DummyGenerator<JournalContext> {

	@Override
	protected JournalContext getContext(ActionRequest request) throws PortalException {
		
		return new JournalContext(request);
	}

	@Override
	protected void exec(ActionRequest request, JournalContext paramContext) throws Exception {

		// Structure Key
		String structureKey =
			_DDMStructureLocalService.getStructure(
				paramContext.getDdmStructureId()
			).getStructureKey();
		
		// Template key
		String templateKey = 
				_DDMTemplateLocalService.getTemplate(
					paramContext.getDdmTemplateId()
				).getTemplateKey();
		
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
				title.append(paramContext.getBaseTitle());

				// Add number more then one article
				if (1 < paramContext.getNumberOfArticles()) {
					title.append(i);
				}

				// Build contents
				String content = 
					_journalUtils.buildFields(
						paramContext.getThemeDisplay().getCompanyGroupId(), 
						paramContext.getLocales(), 
						paramContext.getBaseArticle()
					);

				Map<Locale, String> titleMap = new ConcurrentHashMap<Locale, String>();
				titleMap.put(
					paramContext.getDefaultLocale(), 
					title.toString()
				);

				try {
					// Create article
					_journalArticleLocalService.addArticle(
						paramContext.getServiceContext().getUserId(), 	// userId,
						groupId, 										// groupId,
						paramContext.getFolderId(),						// folderId
						titleMap, 										// titleMap
						paramContext.getDescriptionMap(),				// descriptionMap
						content, 										// content
						structureKey,									// ddmStructureKey,
						templateKey, 									// ddmTemplateKey,
						paramContext.getServiceContext()				// serviceContext
					);
					
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
	private DDMStructureLocalService _DDMStructureLocalService;

	@Reference
	private DDMTemplateLocalService _DDMTemplateLocalService;	

}
