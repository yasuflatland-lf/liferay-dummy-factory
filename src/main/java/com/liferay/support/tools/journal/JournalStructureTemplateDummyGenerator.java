package com.liferay.support.tools.journal;

import com.liferay.dynamic.data.mapping.model.DDMStructure;
import com.liferay.dynamic.data.mapping.service.DDMStructureLocalService;
import com.liferay.dynamic.data.mapping.service.DDMStructureService;
import com.liferay.dynamic.data.mapping.service.DDMTemplateLocalService;
import com.liferay.journal.model.JournalArticle;
import com.liferay.journal.service.JournalArticleLocalService;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.support.tools.utils.ProgressManager;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.portlet.ActionRequest;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Structure / Template selected Journal Articles Generator
 * 
 * @author Yasuyuki Takeo
 *
 */
@Component(immediate = true, service = JournalStructureTemplateDummyGenerator.class)
public class JournalStructureTemplateDummyGenerator extends JournalStructureBaseDummyGenerator {

	@Override
	protected JournalContext getContext(ActionRequest request) throws PortalException {
		
		return new JournalContext(request);
	}

	@Override
	protected void exec(ActionRequest request, JournalContext paramContext) throws Exception {

		// Structure Key
		DDMStructure ddmStructure = 
			_DDMStructureLocalService.getStructure(
				paramContext.getDdmStructureId()
			);
		
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
						groupId,
						ddmStructure, 
						paramContext.getLocales()
					);

				Map<Locale, String> titleMap = new ConcurrentHashMap<Locale, String>();
				titleMap.put(
					paramContext.getDefaultLocale(), 
					title.toString()
				);

				try {
					// Create article
					JournalArticle createdArticle = _journalArticleLocalService.addArticle(
						null,
						paramContext.getServiceContext().getUserId(), 	// userId,
						groupId, 										// groupId,
						paramContext.getFolderId(),						// folderId
						titleMap, 										// titleMap
						paramContext.getDescriptionMap(),				// descriptionMap
						content, 										// content
						ddmStructure.getStructureId(),					// ddmStructureId,
						templateKey, 									// ddmTemplateKey,
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
	private DDMStructureLocalService _DDMStructureLocalService;

	@Reference
	private DDMTemplateLocalService _DDMTemplateLocalService;

	@Reference
	private DDMStructureService _ddmStructureService;

}
