package com.liferay.support.tools.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class WorkflowReferenceParserTest {

	@Test
	void parseReadsStepSegments() {
		WorkflowReference workflowReference = new WorkflowReferenceParser().parse(
			"steps.createSite.items[0].groupId");

		assertEquals(WorkflowReferenceTarget.STEP_RESULT, workflowReference.target());
		assertEquals("createSite", workflowReference.stepId());
		assertEquals(3, workflowReference.segments().size());
		assertInstanceOf(
			WorkflowReferencePropertySegment.class,
			workflowReference.segments().get(0));
		assertInstanceOf(
			WorkflowReferenceIndexSegment.class,
			workflowReference.segments().get(1));
		assertInstanceOf(
			WorkflowReferencePropertySegment.class,
			workflowReference.segments().get(2));
	}

	@Test
	void parseRejectsUnsupportedTargets() {
		assertThrows(
			IllegalArgumentException.class,
			() -> new WorkflowReferenceParser().parse("result.step1"));
	}

	@Test
	void parseRejectsMalformedIndexes() {
		assertThrows(
			IllegalArgumentException.class,
			() -> new WorkflowReferenceParser().parse("steps.step1.items[abc]"));
	}

	@Test
	void parseRejectsInputRootIndex() {
		assertThrows(
			IllegalArgumentException.class,
			() -> new WorkflowReferenceParser().parse("input[0]"));
	}

	@Test
	void parseRejectsStepRootIndex() {
		assertThrows(
			IllegalArgumentException.class,
			() -> new WorkflowReferenceParser().parse("steps.createSite[0]"));
	}

}
