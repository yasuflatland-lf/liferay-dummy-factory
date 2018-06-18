package com.liferay.support.tools.blogs;

import com.liferay.blogs.kernel.model.BlogsEntry;
import com.liferay.blogs.kernel.service.BlogsEntryLocalService;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.ServiceContextFactory;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.support.tools.common.DummyGenerator;
import com.liferay.support.tools.utils.ProgressManager;

import java.util.Date;

import javax.portlet.ActionRequest;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(immediate = true, service = BlogsDefaultDummyGenerator.class)
public class BlogsDefaultDummyGenerator extends DummyGenerator<BlogsContext> {

	@Override
	protected BlogsContext getContext(ActionRequest request) throws Exception {
		return new BlogsContext(request);
	}

	@Override
	protected void exec(ActionRequest request, BlogsContext paramContext)
		throws Exception {
		//Tracking progress start
		ProgressManager progressManager = new ProgressManager();
		progressManager.start(request);
		
		System.out.println("Starting to create " + paramContext.getNumberOfPosts() + " blog posts");
		
		ServiceContext serviceContext = ServiceContextFactory.getInstance(
				BlogsEntry.class.getName(), request);
		
		serviceContext.setScopeGroupId(paramContext.getGroupId());
		
		for (long i = 1; i <= paramContext.getNumberOfPosts(); i++) {
			//Update progress
			progressManager.trackProgress(i, paramContext.getNumberOfPosts());
			
			//Create blogs title
			StringBundler title = new StringBundler(2);
			title.append(paramContext.getBaseTitle());
			
			//Add number more then one post
			if(1 < paramContext.getNumberOfPosts()) {
				title.append(i);
			}
			
			try {
				
				if(_log.isDebugEnabled()) {
					_log.debug("-----");
				}
				
				_blogsEntryLocalService.addEntry(
						paramContext.getUserId(), 
						title.toString(), 
						StringPool.BLANK, 
						StringPool.BLANK, 
						paramContext.getContents(),
						new Date(), 
						paramContext.isAllowPingbacks(), 
						paramContext.isAllowTrackbacks(), 
						paramContext.getAllowTrackbacks(), 
						StringPool.BLANK, 
						null, 
						null,
						serviceContext);
			
			} catch (Exception e) {
				//Finish progress
				progressManager.finish();	
				throw e;
			}	
		}
		
		//Finish progress
		progressManager.finish();	
		
		System.out.println("Finished creating " + paramContext.getNumberOfPosts() + " blog posts");
		
		
	}

	@Reference
	private BlogsEntryLocalService _blogsEntryLocalService;
	
	private static final Log _log = LogFactoryUtil.getLog(BlogsDefaultDummyGenerator.class);	
	
}