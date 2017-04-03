package org.unicode.bidi;
/*
 * (C) Copyright ASMUS, Inc. 2013, All Rights Reserved
 *
 * Distributed under the Terms of Use in http://www.unicode.org/copyright.html.
 */

 import org.unicode.bidi.BidiPBAReference;

public abstract class BidiTestBracketMap {
	/**
	 * getPairedBracket - given one character of a bracket pair return the other
	 * returns the character passed in if not a bracket
	 * 
	 * @param ch
	 *            a (possible) bracket character
	 * @return the other part of a paired bracket pair, or self, if not paired
	 */
	public abstract char getPairedBracket(char ch);

	/**
	 * getPairedBracketValues - returns the original character for each  
	 * character in the string, except that both members of a set of paired
	 * bracket are replaced by the same character (closing br
	 * Example: [Test(s)] --> ]Test)s)]. Where bracket characters have canonical
	 * singleton decompositions, the decompositions are used. This mapping
	 * simplifies matching brackets. Note, the distinction between
	 * opening/closing is available from the bracket types.
	 * 
	 * @param str
	 * @return a canonical character for each member of a bracket pair, 
	 *         otherwise returns the input character 
	 */
	public abstract int[] getBracketValues(String str);

	/**
	 * getBracketType for a character "pbt.o" for opening, "pbt.c" for closing
	 * and "pbt.n" for none (either not a bracket or not supported by
	 * BP-algorithm)
	 * 
	 * @param ch
	 *            a (possible) bracket character
	 * @return 0 if not a bracket (none), 1 if opening, 2 if closing
	 */
	public abstract byte getBracketType(char ch);

	/**
	 * getBracketTypes for each character "pbt.o" for opening, "pbt.c" for
	 * closing and "pbt.n" for none (either not a bracket or not supported by
	 * BP-algorithm)
	 * 
	 * @param str
	 *           - containing (possible) bracket character
	 * @return array of paired bracket types for each character indicating
	 *         whether the character is opening, closing or none
	 */
	public abstract byte[] getBracketTypes(String str);

	/**
	 * map between singleton canonically equivalent brackets
	 * 
	 * @param chBracket
	 *            - character that may be bracket character with a singleton
	 *            canonical decomposition
	 * @return the singleton canonical decomposition mapping for bracket
	 *         characters only (identity otherwise)
	 */
	public abstract char mapCanon(char chBracket);

	/**
	 * Return the name of this mapping.
	 */
	public abstract String getName();

	/**
	 * Bracket map instance that maps ASCII bracket characters to bracket
	 * properties.
	 */
	public static final BidiTestBracketMap TEST_BRACKETS = new BidiASCIIBracketMap();

	private final static class BidiASCIIBracketMap extends DefaultBracketMap {
		BidiASCIIBracketMap() {
			super("ASCII");
		}

		/*
		 * In an actual implementation the Paired Bracket Type would be supplied
		 * by the Unicode Character Database.
		 */
		public final byte getBracketType(char ch) {
			// test implementation uses ( { and [
			switch (ch) {
			default:
				return BidiPBAReference.n;
			case '(':
			case '{':
			case '[':
			case '<':
				return BidiPBAReference.o;
			case ')':
			case '}':
			case ']':
			case '>':
				return BidiPBAReference.c;
			}
		}

		/*
		 * In an actual implementation the character that pairs with a given
		 * bracket character would be supplied by the Unicode Character
		 * Database.
		 */
		public final char getPairedBracket(char ch) {
			// test implementation uses ( { and [
			switch (ch) {
			default:
				// ideally throw an exception
				return ch; // self, if not a paired bracket, for now
			case '(':
				return ')';
			case '{':
				return '}';
			case '[':
				return ']';
			case ')':
				return '(';
			case '}':
				return '{';
			case ']':
				return '[';
			}
		}

		/*
		 * this is an ASCII hack simulating canonical mapping of < with [ style
		 * brackets in a real implementation this would take care of canonically
		 * mapping the bracket pairs at 2379, 237A and 3008,3009
		 */
		public final char mapCanon(char chBracket) {
			switch (chBracket) {
			default:
				return chBracket;
			case '>':
				return ']';
			case '<':
				return '[';
			}

		}
	}

	public static class DefaultBracketMap extends BidiTestBracketMap {
		private final String name;

		/**
		 * Return the name of this mapping.
		 */
		public String getName() {
			return name;
		}

		protected DefaultBracketMap(String name) {
			this.name = name;
		}

		/**
		 * Noop implementation of getPairedBracket
		 */
		public char getPairedBracket(char ch) {
			return ch;
		}

		/**
		 * Default implementation of getPBValues calling getPairedBracket
		 */
		public int[] getBracketValues(String str) {
			int[] pbValues = new int[str.length()];
			for (int ich = 0; ich < str.length(); ich++) {
				if (getBracketType(str.charAt(ich)) == BidiPBAReference.o)
					pbValues[ich] = mapCanon(getPairedBracket(str.charAt(ich)));
				else
					pbValues[ich] = mapCanon(str.charAt(ich));
			}
			return pbValues;
		}

		/**
		 * Noop implementation of getBracketType
		 */
		public byte getBracketType(char ch) {
			return BidiPBAReference.n; // not a bracket
		}

		/*
		 * Default implementation calling getBracketType on each character in
		 * the string
		 */
		public final byte[] getBracketTypes(String str) {
			byte[] pbTypes = new byte[str.length()];
			for (int ich = 0; ich < str.length(); ich++) {
				pbTypes[ich] = getBracketType(str.charAt(ich));
			}
			return pbTypes;
		}

		/**
		 * Noop implementation of mapCanon
		 */
		public char mapCanon(char chBracket) {
			return chBracket;
		}
	}

	protected BidiTestBracketMap() {
	};
}
