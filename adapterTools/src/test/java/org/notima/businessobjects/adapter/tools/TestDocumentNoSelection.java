package org.notima.businessobjects.adapter.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

class TestDocumentNoSelection {

	@Test
	void singlesAndRanges() {
		DocumentNoSelection sel = DocumentNoSelection.parse("1,4-5,6,9,15-20");
		assertEquals(Arrays.asList("1", "4", "5", "6", "9", "15", "16", "17", "18", "19", "20"), sel.expand(100));
		assertTrue(sel.contains("17"));
		assertFalse(sel.contains("7"));
		assertFalse(sel.contains("21"));
	}

	@Test
	void singleDocumentNo() {
		assertEquals(Arrays.asList("105723"), DocumentNoSelection.parse("105723").expand(10));
	}

	@Test
	void whitespaceAndDuplicates() {
		assertEquals(Arrays.asList("1", "2", "3"), DocumentNoSelection.parse(" 1 - 3 , 2 ").expand(10));
	}

	@Test
	void quotedDocumentNos() {
		DocumentNoSelection sel = DocumentNoSelection.parse("'F-0098'-'F-0101','A,1','It''s'");
		assertEquals(Arrays.asList("F-0098", "F-0099", "F-0100", "F-0101", "A,1", "It's"), sel.expand(10));
		assertTrue(sel.contains("F-0100"));
		assertFalse(sel.contains("F-100"));
		assertTrue(sel.contains("A,1"));
	}

	@Test
	void prefixWithoutPadding() {
		DocumentNoSelection sel = DocumentNoSelection.parse("INV9-INV11");
		assertEquals(Arrays.asList("INV9", "INV10", "INV11"), sel.expand(10));
	}

	@Test
	void malformed() {
		assertThrows(IllegalArgumentException.class, () -> DocumentNoSelection.parse(""));
		assertThrows(IllegalArgumentException.class, () -> DocumentNoSelection.parse("1,,2"));
		assertThrows(IllegalArgumentException.class, () -> DocumentNoSelection.parse("1-"));
		assertThrows(IllegalArgumentException.class, () -> DocumentNoSelection.parse("5-1"));
		assertThrows(IllegalArgumentException.class, () -> DocumentNoSelection.parse("A1-B5"));
		assertThrows(IllegalArgumentException.class, () -> DocumentNoSelection.parse("'F-1"));
		assertThrows(IllegalArgumentException.class, () -> DocumentNoSelection.parse("F-1"));
		assertThrows(IllegalArgumentException.class, () -> DocumentNoSelection.parse("1 2"));
	}

	@Test
	void expandLimit() {
		assertThrows(IllegalArgumentException.class, () -> DocumentNoSelection.parse("1-1000").expand(999));
		assertEquals(1000, DocumentNoSelection.parse("1-1000").expand(1000).size());
	}

}
