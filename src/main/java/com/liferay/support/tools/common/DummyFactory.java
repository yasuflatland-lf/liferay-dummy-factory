package com.liferay.support.tools.common;

import javax.portlet.ActionRequest;

public abstract class DummyFactory {
	abstract public DummyGenerator<?> create(ActionRequest request);
}
