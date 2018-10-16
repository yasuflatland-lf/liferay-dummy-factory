
package com.liferay.support.tools.user;

import com.liferay.support.tools.common.DummyFactory;
import com.liferay.support.tools.common.DummyGenerator;

import javax.portlet.ActionRequest;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * User Factory
 * 
 * @author Yasuyuki Takeo
 */
@Component(immediate = true, service = UserDummyFactory.class)
public class UserDummyFactory extends DummyFactory {

	@Override
	public DummyGenerator<UserContext> create(ActionRequest request) {
		return _userDefaultDummyGenerator;
	}

	@Reference
	UserDefaultDummyGenerator _userDefaultDummyGenerator;
}
