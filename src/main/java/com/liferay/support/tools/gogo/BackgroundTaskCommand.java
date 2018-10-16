package com.liferay.support.tools.gogo;

import com.liferay.portal.background.task.exception.NoSuchBackgroundTaskException;
import com.liferay.portal.kernel.backgroundtask.BackgroundTask;
import com.liferay.portal.kernel.backgroundtask.BackgroundTaskManagerUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.Validator;

import java.util.Arrays;
import java.util.List;

import org.apache.felix.service.command.Descriptor;
import org.osgi.service.component.annotations.Component;

/**
 * Background Task Commands
 * 
 * @author Yasuyuki Takeo
 */
@Component(
	property = {
		"osgi.command.scope=df",
		"osgi.command.function=killbg" 
	},
	service = Object.class
)
public class BackgroundTaskCommand {
	
	@Descriptor("Kill background process(s)")
	public void killbg(String... backgroundTaskIds){

		if(0 == backgroundTaskIds.length) {
			System.out.println("Please input valid background ID");
			return;
		}
		
		List<String> ids = Arrays.asList(backgroundTaskIds);
		
		for(String id : ids) {
			if(!Validator.isNumber(id)) {
				System.out.println("Invalid background ID : " + id + " Skip...");
				continue;
			}
			
			try {
				BackgroundTask bkt = BackgroundTaskManagerUtil.getBackgroundTask(Long.valueOf(id));
				BackgroundTaskManagerUtil.cleanUpBackgroundTask(
					bkt, bkt.getStatus());				
				
			} catch (NoSuchBackgroundTaskException e) {
				System.out.println("Background ID :" + id + " has been successfully deleted.");
			} catch (PortalException e) {
				
				System.out.println("Exception occured. Please see server console.");
				_log.error(e,e);
			}
		}
	}

	private static final Log _log =
		LogFactoryUtil.getLog(BackgroundTaskCommand.class);
}
