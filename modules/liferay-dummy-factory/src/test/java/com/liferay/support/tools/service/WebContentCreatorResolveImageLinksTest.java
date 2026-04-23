package com.liferay.support.tools.service;

import com.liferay.support.tools.constants.LDFPortletKeys;
import com.liferay.support.tools.service.image.ImageSource;
import com.liferay.support.tools.utils.RandomizeContentGenerator;

import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WebContentCreatorResolveImageLinksTest {

	@Test
	public void returns_empty_string_when_linkLists_null_and_randomAmount_zero() {
		String result = WebContentCreator.resolveImageLinks(
			_generator, _deterministicFake, null, 0);

		assertEquals("", result);
	}

	@Test
	public void returns_linkLists_unchanged_when_randomAmount_zero() {
		String linkLists = "https://user/a" + LDFPortletKeys.EOL +
			"https://user/b";

		String result = WebContentCreator.resolveImageLinks(
			_generator, _deterministicFake, linkLists, 0);

		assertEquals(linkLists, result);
	}

	@Test
	public void returns_linkLists_unchanged_when_user_list_already_sufficient() {
		String linkLists = String.join(
			LDFPortletKeys.EOL,
			"https://user/a", "https://user/b", "https://user/c",
			"https://user/d", "https://user/e");

		String result = WebContentCreator.resolveImageLinks(
			_generator, _deterministicFake, linkLists, 3);

		assertEquals(linkLists, result);
	}

	@Test
	public void supplements_when_user_list_empty_and_randomAmount_positive() {
		String result = WebContentCreator.resolveImageLinks(
			_generator, _deterministicFake, null, 3);

		String[] lines = result.split(LDFPortletKeys.EOL);

		assertEquals(3, lines.length);
		assertEquals("https://fake/0", lines[0]);
		assertEquals("https://fake/1", lines[1]);
		assertEquals("https://fake/2", lines[2]);
	}

	@Test
	public void supplements_when_user_list_blank_string() {
		String result = WebContentCreator.resolveImageLinks(
			_generator, _deterministicFake, "", 2);

		String[] lines = result.split(LDFPortletKeys.EOL);

		assertEquals(2, lines.length);
		assertEquals("https://fake/0", lines[0]);
		assertEquals("https://fake/1", lines[1]);
	}

	@Test
	public void supplements_shortfall_when_user_list_shorter_than_randomAmount() {
		String linkLists = "https://user/a";

		String result = WebContentCreator.resolveImageLinks(
			_generator, _deterministicFake, linkLists, 3);

		String[] lines = result.split(LDFPortletKeys.EOL);

		assertEquals(3, lines.length);

		List<String> lineList = List.of(lines);

		assertTrue(lineList.contains("https://user/a"));

		long fakeCount = lineList.stream(
		).filter(
			line -> line.startsWith("https://fake/")
		).count();

		assertEquals(2, fakeCount);
	}

	@Test
	public void merged_result_splits_by_EOL_delimiter() {
		String linkLists = "https://user/a" + LDFPortletKeys.EOL +
			"https://user/b";

		String result = WebContentCreator.resolveImageLinks(
			_generator, _deterministicFake, linkLists, 5);

		String[] lines = result.split(LDFPortletKeys.EOL);

		assertEquals(5, lines.length);

		for (String line : lines) {
			assertFalse(line.isEmpty());
		}
	}

	@Test
	public void does_not_call_imageSource_when_user_list_sufficient() {
		ImageSource throwingSource = request -> {
			throw new AssertionError(
				"imageSource should not be called when user list is sufficient");
		};

		String linkLists = String.join(
			LDFPortletKeys.EOL,
			"https://user/a", "https://user/b", "https://user/c");

		String result = WebContentCreator.resolveImageLinks(
			_generator, throwingSource, linkLists, 3);

		assertEquals(linkLists, result);
	}

	private final RandomizeContentGenerator _generator =
		new RandomizeContentGenerator();

	private final ImageSource _deterministicFake =
		request -> IntStream.range(
			0, request.count()
		).mapToObj(
			i -> "https://fake/" + i
		).toList();

}
