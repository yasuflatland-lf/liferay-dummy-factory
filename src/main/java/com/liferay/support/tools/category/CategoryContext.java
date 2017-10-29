package com.liferay.support.tools.category;

import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.support.tools.common.ParamContext;

import javax.portlet.ActionRequest;

public class CategoryContext extends ParamContext {

	private long numberOfCategories = 0;
	private String baseCategoryName = "";
	private long groupId = 0;
	private long vocabularyId;
	private long numberOfVocabulary = 0;
	private String baseVocabularyName = "";
	long parentCategoryId = 0;

	public CategoryContext(ActionRequest actionRequest) {
		//Fetch data
		numberOfCategories = ParamUtil.getLong(actionRequest, "numberOfCategories",0);
		baseCategoryName = ParamUtil.getString(actionRequest, "baseCategoryName","");
		groupId = ParamUtil.getLong(actionRequest, "group",0);
		vocabularyId = ParamUtil.getLong(actionRequest, "vocabularyId",0);
		numberOfVocabulary = ParamUtil.getLong(actionRequest, "numberOfVocabulary",0);
		baseVocabularyName = ParamUtil.getString(actionRequest, "baseVocabularyName","");
		parentCategoryId = ParamUtil.getLong(actionRequest, "parentCategoryId",0);
	}

	public long getNumberOfCategories() {
		return numberOfCategories;
	}

	public void setNumberOfCategories(long numberOfCategories) {
		this.numberOfCategories = numberOfCategories;
	}

	public String getBaseCategoryName() {
		return baseCategoryName;
	}

	public void setBaseCategoryName(String baseCategoryName) {
		this.baseCategoryName = baseCategoryName;
	}

	public long getGroupId() {
		return groupId;
	}

	public void setGroupId(long groupId) {
		this.groupId = groupId;
	}

	public long getVocabularyId() {
		return vocabularyId;
	}

	public void setVocabularyId(long vocabularyId) {
		this.vocabularyId = vocabularyId;
	}

	public long getNumberOfVocabulary() {
		return numberOfVocabulary;
	}

	public void setNumberOfVocabulary(long numberOfVocabulary) {
		this.numberOfVocabulary = numberOfVocabulary;
	}

	public String getBaseVocabularyName() {
		return baseVocabularyName;
	}

	public void setBaseVocabularyName(String baseVocabularyName) {
		this.baseVocabularyName = baseVocabularyName;
	}

	public long getParentCategoryId() {
		return parentCategoryId;
	}

	public void setParentCategoryId(long parentCategoryId) {
		this.parentCategoryId = parentCategoryId;
	}
}
