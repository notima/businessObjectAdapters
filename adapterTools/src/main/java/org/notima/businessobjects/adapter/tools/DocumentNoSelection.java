package org.notima.businessobjects.adapter.tools;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A selection of document numbers (invoice numbers, order numbers etc) parsed from
 * a specification like <code>1,4-5,6,9,15-20</code>.
 * <p>
 * Items are separated by commas. An item is either a single document number or a
 * range <code>from-to</code>. Document numbers containing commas, hyphens or spaces
 * can be single quoted, ie <code>'F-100'-'F-120','A,1'</code>. A single quote within
 * a quoted number is written as two single quotes.
 * <p>
 * Both ends of a range must end with digits and have the same prefix before the digits,
 * ie <code>100-120</code> or <code>'F-0100'-'F-0120'</code>. Leading zeros are kept
 * when the range is expanded.
 */
public class DocumentNoSelection {

	private static final int MAX_DIGITS = 18;

	private static class Item {
		private String from;
		private String to;		// null for a single document number
		private String prefix;
		private long fromNo;
		private long toNo;
		private int width;		// Width to zero pad to, 0 if no padding
	}

	private String spec;
	private List<Item> items = new ArrayList<Item>();

	private DocumentNoSelection(String spec) {
		this.spec = spec;
	}

	/**
	 * Parses a selection specification.
	 *
	 * @param spec	The specification, ie <code>1,4-5,6,9,15-20</code>
	 * @return		The parsed selection.
	 * @throws IllegalArgumentException	If the specification is empty or malformed.
	 */
	public static DocumentNoSelection parse(String spec) {
		if (spec==null || spec.trim().isEmpty()) {
			throw new IllegalArgumentException("Empty document number selection");
		}
		DocumentNoSelection sel = new DocumentNoSelection(spec);
		new Parser(spec, sel).parse();
		return sel;
	}

	/**
	 * @param documentNo	The document number to check.
	 * @return	True if the document number is part of this selection.
	 */
	public boolean contains(String documentNo) {
		if (documentNo==null) return false;
		for (Item item : items) {
			if (item.to==null) {
				if (item.from.equals(documentNo)) return true;
			} else {
				if (!documentNo.startsWith(item.prefix)) continue;
				String digits = documentNo.substring(item.prefix.length());
				if (!isDigits(digits) || digits.length()>MAX_DIGITS) continue;
				if (item.width>0 && digits.length()!=item.width) continue;
				long no = Long.parseLong(digits);
				if (no>=item.fromNo && no<=item.toNo) return true;
			}
		}
		return false;
	}

	/**
	 * Expands the selection to a list of document numbers, in the order specified.
	 * Duplicates are removed.
	 *
	 * @param maxCount	The maximum number of document numbers to expand to.
	 * @return	The document numbers.
	 * @throws IllegalArgumentException	If the selection contains more than maxCount document numbers.
	 */
	public List<String> expand(int maxCount) {
		List<String> result = new ArrayList<String>();
		Set<String> seen = new HashSet<String>();
		for (Item item : items) {
			if (item.to==null) {
				addIfNew(item.from, result, seen, maxCount);
			} else {
				for (long no = item.fromNo; no<=item.toNo; no++) {
					String digits = Long.toString(no);
					while (digits.length()<item.width) {
						digits = "0" + digits;
					}
					addIfNew(item.prefix + digits, result, seen, maxCount);
				}
			}
		}
		return result;
	}

	private void addIfNew(String documentNo, List<String> result, Set<String> seen, int maxCount) {
		if (seen.add(documentNo)) {
			if (result.size()>=maxCount) {
				throw new IllegalArgumentException("Selection " + spec + " contains more than " + maxCount + " document numbers");
			}
			result.add(documentNo);
		}
	}

	@Override
	public String toString() {
		return spec;
	}

	private void addSingle(String documentNo) {
		Item item = new Item();
		item.from = documentNo;
		items.add(item);
	}

	private void addRange(String from, String to) {
		int fromDigitStart = digitSuffixStart(from);
		int toDigitStart = digitSuffixStart(to);
		if (fromDigitStart==from.length() || toDigitStart==to.length()) {
			throw new IllegalArgumentException("Range " + from + "-" + to + " must end with digits");
		}
		String prefix = from.substring(0, fromDigitStart);
		if (!prefix.equals(to.substring(0, toDigitStart))) {
			throw new IllegalArgumentException("Range " + from + "-" + to + " must have the same prefix on both ends");
		}
		String fromDigits = from.substring(fromDigitStart);
		String toDigits = to.substring(toDigitStart);
		if (fromDigits.length()>MAX_DIGITS || toDigits.length()>MAX_DIGITS) {
			throw new IllegalArgumentException("Range " + from + "-" + to + " has too many digits");
		}
		Item item = new Item();
		item.from = from;
		item.to = to;
		item.prefix = prefix;
		item.fromNo = Long.parseLong(fromDigits);
		item.toNo = Long.parseLong(toDigits);
		if (item.fromNo>item.toNo) {
			throw new IllegalArgumentException("Range " + from + "-" + to + " starts after it ends");
		}
		// Zero padded, ie 0100-0120
		if (fromDigits.length()>1 && fromDigits.startsWith("0")) {
			if (fromDigits.length()!=toDigits.length()) {
				throw new IllegalArgumentException("Range " + from + "-" + to + " is zero padded but the ends have different lengths");
			}
			item.width = fromDigits.length();
		}
		items.add(item);
	}

	private static int digitSuffixStart(String s) {
		int i = s.length();
		while (i>0 && Character.isDigit(s.charAt(i-1))) {
			i--;
		}
		return i;
	}

	private static boolean isDigits(String s) {
		return !s.isEmpty() && digitSuffixStart(s)==0;
	}

	/**
	 * Parses the specification one character at a time.
	 */
	private static class Parser {

		private String spec;
		private DocumentNoSelection sel;
		private int pos = 0;

		Parser(String spec, DocumentNoSelection sel) {
			this.spec = spec;
			this.sel = sel;
		}

		void parse() {
			while (true) {
				skipWhitespace();
				String from = readToken();
				skipWhitespace();
				if (pos<spec.length() && spec.charAt(pos)=='-') {
					pos++;
					skipWhitespace();
					String to = readToken();
					sel.addRange(from, to);
					skipWhitespace();
				} else {
					sel.addSingle(from);
				}
				if (pos>=spec.length()) return;
				if (spec.charAt(pos)!=',') {
					throw error("Expected ','");
				}
				pos++;
			}
		}

		private String readToken() {
			if (pos<spec.length() && spec.charAt(pos)=='\'') {
				return readQuoted();
			}
			int start = pos;
			while (pos<spec.length() && ",-' \t".indexOf(spec.charAt(pos))<0) {
				pos++;
			}
			if (start==pos) {
				throw error("Expected a document number");
			}
			return spec.substring(start, pos);
		}

		private String readQuoted() {
			int start = pos;
			pos++;	// Opening quote
			StringBuilder buf = new StringBuilder();
			while (pos<spec.length()) {
				char c = spec.charAt(pos++);
				if (c=='\'') {
					if (pos<spec.length() && spec.charAt(pos)=='\'') {
						buf.append('\'');
						pos++;
					} else {
						if (buf.length()==0) {
							pos = start;
							throw error("Empty quoted document number");
						}
						return buf.toString();
					}
				} else {
					buf.append(c);
				}
			}
			pos = start;
			throw error("Unterminated quote");
		}

		private void skipWhitespace() {
			while (pos<spec.length() && Character.isWhitespace(spec.charAt(pos))) {
				pos++;
			}
		}

		private IllegalArgumentException error(String msg) {
			return new IllegalArgumentException(msg + " at position " + (pos+1) + " in " + spec);
		}

	}

}
