package com.liferay.support.tools.utils;

import com.github.javafaker.Faker;
import com.liferay.support.tools.constants.LDFPortletKeys;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * 
 * Randomize Content Generator
 * 
 * @author Yasuyuki Takeo
 *
 */
@Component(immediate = true,service = RandomizeContentGenerator.class)
public class RandomizeContentGenerator {
	/**
	 * Generate Link rows
	 * 
	 * @param linkLists link list string from a textarea
	 * @return Link rows
	 */
	public List<String> generateLinks(String linkLists) {
		String[] rows = linkLists.split(LDFPortletKeys.EOL);
		return Arrays.asList(rows);
	}
	
	/**
	 * Generate Random index
	 * 
	 * @param totalParagraphs Total index size
	 * @param randomAmount List size of return List
	 * @return Randomized list index
	 */
	public List<Integer> generateRandomIndex(int totalParagraphs, int randomAmount ) {
		if(totalParagraphs <= 0 || randomAmount <= 0) {
			return new ArrayList<Integer>();
		}
		
		List<Integer> integers =
		    IntStream.range(0, totalParagraphs)
		        .boxed()         
		        .collect(Collectors.toList());

		Collections.shuffle(integers);
		
		return integers.stream().limit(randomAmount).collect(Collectors.toList());
	}
	
	/**
	 * Image link generator
	 * 
	 * @param src
	 * @return
	 */
	public String generateImageLink(String src) {
		return "<img src=\"".concat(src).concat("\" />");
	}
	
	/**
	 * Insert Random Links
	 * 
	 * @param paragraphes Paragrphes list
	 * @param links URL link list
	 * @param rndIndex
	 * @return Randomly links inserted contents string
	 */
	public String insertRandomLinksInContents(List<String> paragraphes, List<String> links, List<Integer> rndIndex) {
		
		StringBuilder sb = new StringBuilder();
		int idx = 0;
		
		for(String paragraphe : paragraphes) {
			sb.append(paragraphe);
			sb.append(LDFPortletKeys.EOL);
			
			if( idx < rndIndex.size() && 
				rndIndex.get(idx) < links.size() &&
				rndIndex.contains(rndIndex.get(idx))) {
				
				sb.append(generateImageLink(links.get(rndIndex.get(idx))));
				sb.append(LDFPortletKeys.EOL);
				++idx;
			}
		}

		return sb.toString();
	}

	/**
	 * Generate Random Paragraphes
	 * 
	 * @param locale Locale for generating contents
	 * @param totalParagraphs Amount of paragraphes to be included.
	 * @return Generated random paragraphs
	 */
	public List<String> generateRandomParagraphes(String locale, int totalParagraphs) {
		Faker faker = _commonUtil.createFaker(locale);
		return faker.lorem().paragraphs(totalParagraphs);
	}

	/**
	 * Generate Random Contents
	 * 
	 * @param locale
	 * @param totalParagraphs
	 * @param randomAmount
	 * @param linkLists
	 * @return
	 */
	public String generateRandomContents(String locale, int totalParagraphs, int randomAmount,String linkLists) {
		// Paragraphes
		List<String> paragraphes = generateRandomParagraphes(locale, totalParagraphs);
		
		// Random index
		List<Integer> rndIndex = generateRandomIndex( totalParagraphs,  randomAmount);
		
		// Image links
		List<String> links = generateLinks(linkLists);
		
		return insertRandomLinksInContents(paragraphes, links, rndIndex);
	}
	
	@Reference
	private CommonUtil _commonUtil;

}
