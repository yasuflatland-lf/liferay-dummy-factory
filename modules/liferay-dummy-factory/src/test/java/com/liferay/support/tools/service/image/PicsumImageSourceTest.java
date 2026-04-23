package com.liferay.support.tools.service.image;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PicsumImageSourceTest {

	@Test
	public void supply_returns_empty_list_when_count_is_zero() {
		List<String> urls = _source.supply(ImageRequest.of(0));

		assertNotNull(urls);
		assertTrue(urls.isEmpty());
	}

	@Test
	public void supply_returns_n_urls_when_count_is_n() {
		List<String> urls = _source.supply(ImageRequest.of(5));

		assertEquals(5, urls.size());
	}

	@Test
	public void supply_urls_start_with_picsum_seed_prefix() {
		List<String> urls = _source.supply(ImageRequest.of(4));

		for (String url : urls) {
			assertTrue(
				url.startsWith("https://picsum.photos/seed/"),
				"URL did not start with expected prefix: " + url);
		}
	}

	@Test
	public void supply_urls_embed_width_and_height() {
		List<String> urls = _source.supply(new ImageRequest(3, 640, 480));

		assertEquals(3, urls.size());

		for (String url : urls) {
			assertTrue(
				url.endsWith("/640/480"),
				"URL did not end with /640/480: " + url);
		}
	}

	@Test
	public void supply_urls_are_unique_across_a_single_call() {
		List<String> urls = _source.supply(ImageRequest.of(10));

		Set<String> unique = new HashSet<>(urls);

		assertEquals(10, unique.size());
	}

	@Test
	public void supply_returns_unmodifiable_list() {
		List<String> urls = _source.supply(ImageRequest.of(2));

		assertThrows(
			UnsupportedOperationException.class,
			() -> urls.add("https://example.com"));
	}

	@Test
	public void image_request_rejects_negative_count() {
		assertThrows(
			IllegalArgumentException.class,
			() -> new ImageRequest(-1, 800, 600));
	}

	@Test
	public void image_request_rejects_non_positive_width() {
		assertThrows(
			IllegalArgumentException.class,
			() -> new ImageRequest(1, 0, 600));
	}

	@Test
	public void image_request_rejects_non_positive_height() {
		assertThrows(
			IllegalArgumentException.class,
			() -> new ImageRequest(1, 800, 0));
	}

	@Test
	public void image_request_of_defaults_to_800_by_600() {
		ImageRequest request = ImageRequest.of(3);

		assertEquals(3, request.count());
		assertEquals(800, request.width());
		assertEquals(600, request.height());
	}

	private final PicsumImageSource _source = new PicsumImageSource();

}
