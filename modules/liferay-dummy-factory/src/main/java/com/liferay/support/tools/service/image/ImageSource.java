package com.liferay.support.tools.service.image;

import java.util.List;

@FunctionalInterface
public interface ImageSource {

	public List<String> supply(ImageRequest request);

}
