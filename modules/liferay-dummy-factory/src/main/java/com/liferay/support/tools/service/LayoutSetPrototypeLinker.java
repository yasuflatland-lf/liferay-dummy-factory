package com.liferay.support.tools.service;

import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.User;
import com.liferay.sites.kernel.util.Sites;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = LayoutSetPrototypeLinker.class)
public class LayoutSetPrototypeLinker {

	public void linkUserPersonalSite(
			User user, long publicLayoutSetPrototypeId,
			long privateLayoutSetPrototypeId)
		throws Exception {

		linkSite(
			user.getGroup(), publicLayoutSetPrototypeId,
			privateLayoutSetPrototypeId);
	}

	public void linkSite(
			Group group, long publicLayoutSetPrototypeId,
			long privateLayoutSetPrototypeId)
		throws Exception {

		if ((publicLayoutSetPrototypeId == 0) &&
			(privateLayoutSetPrototypeId == 0)) {

			return;
		}

		boolean publicEnabled = publicLayoutSetPrototypeId != 0;
		boolean privateEnabled = privateLayoutSetPrototypeId != 0;

		_sites.updateLayoutSetPrototypesLinks(
			group, publicLayoutSetPrototypeId, privateLayoutSetPrototypeId,
			publicEnabled, privateEnabled);
	}

	@Reference
	private Sites _sites;

}
