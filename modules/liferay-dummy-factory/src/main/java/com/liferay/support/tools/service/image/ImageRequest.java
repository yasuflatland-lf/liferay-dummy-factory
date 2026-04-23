package com.liferay.support.tools.service.image;

public record ImageRequest(int count, int width, int height) {

	public ImageRequest {
		if (count < 0) {
			throw new IllegalArgumentException(
				"count must be greater than or equal to 0");
		}

		if (width <= 0) {
			throw new IllegalArgumentException(
				"width must be greater than 0");
		}

		if (height <= 0) {
			throw new IllegalArgumentException(
				"height must be greater than 0");
		}
	}

	public static ImageRequest of(int count) {
		return new ImageRequest(count, 800, 600);
	}

}
