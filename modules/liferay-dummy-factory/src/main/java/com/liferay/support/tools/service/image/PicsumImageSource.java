package com.liferay.support.tools.service.image;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.osgi.service.component.annotations.Component;

@Component(service = ImageSource.class)
public class PicsumImageSource implements ImageSource {

	@Override
	public List<String> supply(ImageRequest request) {
		List<String> urls = new ArrayList<>(request.count());

		for (int i = 0; i < request.count(); i++) {
			String uuid = UUID.randomUUID().toString();

			urls.add(
				"https://picsum.photos/seed/" + uuid + "/" + request.width() +
					"/" + request.height());
		}

		return Collections.unmodifiableList(urls);
	}

}
