import java.util.*;
import java.io.BufferedReader;
import java.io.Reader;
import java.io.FileReader;
import java.io.IOException;
//import com.ibm.text.CollationData.*;

/**
* DemoCollation is a working version of UTR#10 Unicode Collation Algorithm,
* as described on http://www.unicode.org/unicode/reports/tr10/
* @author Mark Davis

It is not optimized, although it does use some techniques that are required for
a real optimization, such as squeezing all the weights into 32 bits.<p>

Invariants relied upon by the algorithm:

UCA Data:
1. While it contains secondaries greater than 0xFF, 
these can be folded down by subtracting 0xC0--without collision--to be less than 0xFF
2. Tertiary values are less than 0x80
3. Contracting characters must be "completed": if "abcd" is a contracting character, 
then "abc" is also.
4. Variables (marked with *), have a distinct, closed range of primaries. 
That is, there are no variable CEs X, Z and non-ignorable CE Y such that X[1] <= Y[1] <= Z[1]
5. It needs to be fixed when reading: only non-zero weights (levels 1-3) are really variable!

#4 saves a bit in each CE.

Limits
1. There is a limit on the number of expanding characters. If N is the number of expanding
characters, then their total lengths must be less than 65536-N. This should never pose a
problem in practice.
2. If any of the weight limits are reached (FFFF for primary, FF for secondary, tertiary),
expanding characters can be used to achieve the right results, as discussed in UTR#10.

Remarks:
Neither the old 14651 nor the old UCA algorithms for backwards really worked.
This is because of shared
characters between scripts with different directions, like French with Arabic or Greek.
*/

final public class DemoCollation {
    public static final String copyright = 
      "Copyright (C) 2000, IBM Corp. and others. All Rights Reserved.";

    /**
     * Records the codeversion
     */
    private static final String codeVersion = "7";

    // base directory will change depending on the installation
    private static final String BASE_DIR = "c:\\Documents and Settings\\Davis\\My Documents\\";
    
    /** Enum for alternate handling */
    public static final byte SHIFTED = 0, ZEROED = 1, NON_IGNORABLE = 2;
    
// =============================================================
// Test Settings
// =============================================================
    static final boolean DEBUG = false;
    static final boolean SHOW_CE = false;
    static final boolean CHECK_UNIQUE = false;
    static final boolean CHECK_UNIQUE_EXPANSIONS = false; // only effective if CHECK_UNIQUE
    static final boolean CHECK_UNIQUE_VARIABLES = false; // only effective if CHECK_UNIQUE
    static final boolean TEST_BACKWARDS = false;
    static final boolean RECORDING_DATA = false;
    static final boolean RECORDING_CHARS = true;
    
// =============================================================
// Main Methods
// =============================================================

    /**
     * Initializes the collation from a stream of rules in the normal formal.
     * If the source is null, uses the normal Unicode data files, which
     * need to be in BASE_DIR.
     */
    public DemoCollation(BufferedReader source) throws java.io.IOException {
        fullData = source == null;

        // clear some tables
        for (int i = 0; i < collationElements.length; ++i) {
            collationElements[i] = UNSUPPORTED;
        }
        // load the normalizer
        if (toD == null) {
            toD = new Normalizer(Normalizer.D, fullData);
        }
        
        // either get the full sources, or just a demo set
        if (fullData) {
            for (int i = 0; i < KEYS.length; ++i) {
                BufferedReader in = new BufferedReader(
                    new FileReader(KEYS[i]), BUFFER_SIZE);
                addCollationElements(in);
                in.close();
            }
        } else {
            addCollationElements(source);
        }
        cleanup();
    }
    
    /**
     * Constructs a sort key for a string of input Unicode characters. Uses
     * default values for alternate and decomposition.
     * @param sourceString string to make a sort key for.
     * @return Result is a String not of really of Unicodes, but of weights.
     * String is just a handy way of returning them in Java, since there are no
     * unsigned shorts.
     */
    public String getSortKey(String sourceString) {
        return getSortKey(sourceString, defaultAlternate, defaultDecomposition);
    }
    /**
     * Constructs a sort key for a string of input Unicode characters. Uses
     * default value decomposition.
     * @param sourceString string to make a sort key for.
     * @param alternate choice of different 4th level weight construction
     * @return Result is a String not of really of Unicodes, but of weights.
     * String is just a handy way of returning them in Java, since there are no
     * unsigned shorts.
     */

    public String getSortKey(String sourceString, byte alternate) {
        return getSortKey(sourceString, alternate, defaultDecomposition);
    }
    
    /**
     * Constructs a sort key for a string of input Unicode characters.
     * @param sourceString string to make a sort key for.
     * @param alternate choice of different 4th level weight construction
     * @param decomposition true for UCA, false where the text is guaranteed to be
     * normalization form C with no combining marks of class 0.
     * @return Result is a String not of really of Unicodes, but of weights.
     * String is just a handy way of returning them in Java, since there are no
     * unsigned shorts.
     */
    public String getSortKey(String sourceString, byte alternate, boolean decomposition) {
        decompositionBuffer.setLength(0);
        if (decomposition) {
            toD.normalize(sourceString, decompositionBuffer);
        } else {
            decompositionBuffer.append(sourceString);
        }
        storedDecomposition = decomposition;    // record the setting for other methods
        index = 0;                              // position in source string
        int fixQuaternatiesPosition;

        // Weight strings - not chars, weights.
        primaries.setLength(0);             // clear out
        secondaries.setLength(0);           // clear out
        tertiaries.setLength(0);            // clear out
        quaternaries.setLength(0);          // clear out
        if (SHOW_CE) debugList.setLength(0); // clear out
        
        rearrangeBuffer = EMPTY;            // clear the rearrange buffer (thai)
        
        char weight4 = '\u0000'; // DEFAULT FOR NON_IGNORABLE

        // process CEs, building weight strings
        while (true) {
            //fixQuaternatiesPosition = quaternaries.length();
            int ce = getCE();
            if (ce == TERMINATOR) break;
            
            switch (alternate) {
              case ZEROED:
                if (isVariable(ce)) {
                    ce = 0;
                }
                break;
              case SHIFTED:
                if (ce == 0) {
                    weight4 = 0;
                } else if (isVariable(ce)) { // variables
                    weight4 = getPrimary(ce);
                    ce = 0;
                } else { // above variables
                    weight4 = '\uFFFF';
                }
                break;
              // case NON_IGNORABLE: // doesn't ever change!
            }
            if (SHOW_CE) {
                if (debugList.length() != 0) debugList.append("/");
                debugList.append(ceToString(ce));
            }
            
            // add weights
            char w = getPrimary(ce);
            if (DEBUG) System.out.println("CE: " + hex(ce));
            if (w != 0) primaries.append(w);
            
            w = getSecondary(ce);
            if (w != 0) {
                if (!useBackwards) {
                    secondaries.append(w);
                } else {
                    secondaries.insert(0, w);
                }
            }
            
            w = getTertiary(ce);
            if (w != 0) tertiaries.append(w);
   
            if (weight4 != 0) quaternaries.append(weight4);
        }
        
        // Produce weight strings
        // For simplicity, we use the strength setting here.
        // To optimize, we wouldn't actually generate the weights in the first place.
        
        StringBuffer result = primaries;
        if (strength >= 2) {
            result.append('\u0000');    // separator
            result.append(secondaries);
            if (strength >= 3) {
                result.append('\u0000');    // separator
                result.append(tertiaries);
                if (strength >= 4) {
                    result.append('\u0000');    // separator
                    result.append(quaternaries);
                    //appendInCodePointOrder(decompositionBuffer, result);
                }
            }
        }
        return result.toString();
    }
    
    /**
     * Turns backwards (e.g. for French) on globally for all secondaries
     */
    public void setBackwards(boolean backwards) {
        useBackwards = backwards;
    }

    /**
     * Retrieves value applied by set.
     */
    public boolean isBackwards() {
        return useBackwards;
    }

    /**
     * Causes variables (those with *) to be set to all zero weights (level 1-3).
     */
    public void setDecompositionState(boolean state) {
        defaultDecomposition = state;
    }

    /**
     * Retrieves value applied by set.
     */
    public boolean isDecomposed() {
        return defaultDecomposition;
    }

    /**
     * Causes variables (those with *) to be set to all zero weights (level 1-3).
     */
    public void setAlternate(byte status) {
        defaultAlternate = status;
    }

    /**
     * Retrieves value applied by set.
     */
    public byte getAlternate() {
        return defaultAlternate;
    }

    /**
     * Sets the maximum strength level to be included in the string. 
     * E.g. with 3, only weights of 1, 2, and 3 are included: level 4 weights are discarded.
     */
    public void setStrength(int inStrength) {
        strength = inStrength;
    }

    /**
     * Retrieves value applied by set.
     */
    public int getStrength() {
        return strength;
    }
    
    /**
     * Retrieves version
     */
    public String getCodeVersion() {
        return codeVersion;
    }

    /**
     * Retrieves version
     */
    public String getDataVersion() {
        return dataVersion;
    }

    /**
     * Appends UTF-16 string
     * with the values swapped around so that they compare in
     * code-point order.
     * @param source Normal UTF-16 (Java) string
     * @return sort key (as string)
     * @author Markus Scherer (cast into Java by MD)
     */
    public static void appendInCodePointOrder(StringBuffer source, StringBuffer target) {
        for (int i = 0; i < source.length(); ++i) {
            int ch = source.charAt(i);
            target.append((char)(ch + utf16CodePointOrder[ch>>11]));
        }
    }
    
// =============================================================
// Utility methods
// =============================================================

    /**
     * Produces a human-readable string for a sort key.
     * The 0000 separator is replaced by a '|'
     */
    static public String toString(String sortKey) {
        StringBuffer result = new StringBuffer();
        boolean needSep = false;
        result.append("[");
        for (int i = 0; i < sortKey.length(); ++i) {
            char ch = sortKey.charAt(i);
            if (ch == 0) {
                result.append("|");
                //needSep = false;
            } else {
                if (needSep) result.append(" ");
                result.append(hex(ch));
                needSep = true;
            }
        }
        result.append("]");
        return result.toString();
    }
    
    /**
     * Produces a human-readable string for a collation element
     */
    static public String ceToString(int ce) {
        return "[" + hex(getPrimary(ce)) + "." 
          + hex(getSecondary(ce)) + "."
          + hex(getTertiary(ce)) + "]";
    }
    
    /**
     * Supplies a zero-padded hex representation of an integer (without 0x)
     */
    static public String hex(int i) {
        String result = Long.toString(i & 0xFFFFFFFFL, 16).toUpperCase();
        return "00000000".substring(result.length(),8) + result;
    }
    
    /**
     * Supplies a zero-padded hex representation of a Unicode character (without 0x, \\u)
     */
    static public String hex(char i) {
        String result = Integer.toString(i, 16).toUpperCase();
        return "0000".substring(result.length(),4) + result;
    }
    
    /**
     * Supplies a zero-padded hex representation of a Unicode String (without 0x, \\u)
     *@param sep can be used to give a sequence, e.g. hex("ab", ",") gives "0061,0062"
     */
    static public String hex(String s, String sep) {
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < s.length(); ++i) {
            if (i != 0) result.append(sep);
            result.append(hex(s.charAt(i)));
        }
        return result.toString();
    }
    
    /**
     * Supplies a zero-padded hex representation of a Unicode String (without 0x, \\u)
     *@param sep can be used to give a sequence, e.g. hex("ab", ",") gives "0061,0062"
     */
    static public String hex(StringBuffer s, String sep) {
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < s.length(); ++i) {
            if (i != 0) result.append(sep);
            result.append(hex(s.charAt(i)));
        }
        return result.toString();
    }
    
// =============================================================
// Privates
// =============================================================
    
    /**
     * Array used to reorder surrogates to top of 16-bit range, and others down.
     * Adds 2000 to D800..DFFF, making them F800..FFFF
     * Subtracts 800 from E000..FFFF, making them D800..F7FF
     */
    private static final int[] utf16CodePointOrder = {
        0, 0, 0, 0,                        // 00, 08, 10, 18
        0, 0, 0, 0,                        // 20, 28, 30, 38
        0, 0, 0, 0,                        // 40, 48, 50, 58
        0, 0, 0, 0,                        // 60, 68, 70, 78
        0, 0, 0, 0,                        // 80, 88, 90, 98
        0, 0, 0, 0,                        // A0, A8, B0, B8
        0, 0, 0, 0x2000,                   // C0, C8, D0, D8
        -0x800, -0x800, -0x800, -0x800     // E0, E8, F0, F8
    };

    /**
     * NFD required
     */
    private static Normalizer toD;

    /**
     * Records the dataversion
     */
    private String dataVersion = "?";

    /**
     * Turns backwards (e.g. for French) on globally for all secondaries
     */
    private boolean useBackwards = false;
    
    /**
     * Choice of how to handle variables (those with *)
     */
    private byte defaultAlternate = SHIFTED;
    
    /**
     * For testing
     */
    private boolean defaultDecomposition = true;
    
    /**
     * Sets the maximum strength level to be included in the string. 
     * E.g. with 3, only weights of 1, 2, and 3 are included: level 4 weights are discarded.
     */
    private int strength = 4;
    
    /**
     * Position in decompositionBuffer used when constructing sort key
     */
    private int index;

    /**
     * Version of the Unicode Standard to use
     */
    private static final String VERSION = ""; // "-2.1.9d7";
    
    /**
     * List of files to use for constructing the CE data, used by build()
     */
    private static final String[] KEYS = {
        //"D:\\UnicodeData\\testkeys.txt",
        BASE_DIR + "UnicodeData\\Collation\\basekeys" + VERSION + ".txt",
        BASE_DIR + "UnicodeData\\Collation\\compkeys" + VERSION + ".txt",
        BASE_DIR + "UnicodeData\\Collation\\ctrckeys" + VERSION + ".txt",
    };
 
    /**
     * File buffer size, used to make reads faster.
     */
    private static final int BUFFER_SIZE = 64*1024;
    
// =============================================================
// Collation Element Memory Data Table Formats
// =============================================================
    /**
     * Utility, used to get the primary weight from a 32-bit CE
     * The primary is 16 bits, stored in b31..b16
     */
    private static char getPrimary(int ce) {
        return (char)(ce >>> 16);
    }

    /**
     * Utility, used to get the secondary weight from a 32-bit CE
     * The secondary is 8 bits, stored in b15..b8
     */
    private static char getSecondary(int ce) {
        return (char)((ce >>> 8) & 0xFF);
    }

    /**
     * Utility, used to get the tertiary weight from a 32-bit CE
     * The tertiary is 6 bits, stored in b6..b0
     */
    private static char getTertiary(int ce) {
        return (char)(ce & 0xFF);
    }

    /**
     * Utility, used to determine whether a CE is variable or not.
     */
     
    private boolean isVariable(int ce) {
        return (variableLowCE <= ce && ce <= variableHighCE);
    }
    
    /**
     * Utility, used to make a CE from the pieces. They must already
     * be in the right range of values.
     */
    private static int makeKey(int primary, int secondary, int tertiary) {
        return (primary << 16) | (secondary << 8) | tertiary;
    }

    /**
     * Temporary buffer used in getSortKey for the decomposed string
     */
    StringBuffer decompositionBuffer = new StringBuffer();
    
    /**
     * The collation element data is stored a couple of different structures.
     * First is collationElements, which generally contains the 32-bit CE corresponding
     * to the data. It is directly indexed by character code.<br>
     * For brevity in the implementation, we just use a flat array.
     * A real implementation would use a multi-stage table, as described in TUS Section 5.
     * table of simple collation elements, indexed by char.<br>
     * Exceptional cases: expanding, contracting, unsupported are handled as described below.
     */
    int[] collationElements = new int[65536];
    
    /**
     * A special bit combination in a CE is used to reserve exception cases. This has the effect
     * of removing 32 primary key values out of the 65536 possible.
     */
    static final int EXCEPTION_CE_MASK = 0xFFC00000;
    
    /**
     * Any unsupported characters (those not in the UCA data tables) 
     * are marked with a exception bit combination
     * so that they can be treated specially.<br>
     * There are at least 34 values, so that we can use a range for surrogates
     * However, we do add to the first weight if we have surrogate pairs!
     */
    static final int UNSUPPORTED = 0xFFC20101;
    
    /**
     * Used to composed Hangul and Han characters
     */
     
    static final int NEUTRAL_SECONDARY_TERTIARY = 0x2002;
       
    /**
     * Contracting characters are marked with a exception bit combination 
     * in the collationElement table.
     * This means that they are the first character of a contraction, and need
     * to be looked up (with following characters) in the contractingTable.<br>
     * This isn't a MASK since there is exactly one value.
     */
    static final int CONTRACTING = 0xFFC10000;

    /**
     * Expanding characters are marked with a exception bit combination
     * in the collationElement table.
     * This means that they map to more than one CE, which is looked up in
     * the expansionTable by index. See EXCEPTION_INDEX_MASK
     */
    static final int EXPANDING_MASK = 0xFFC00000; // marks expanding range start
    
    /**
     * This mask is used to get the index from an EXPANDING exception.
     * The contracting characters can also make use of this in a future optimization.
     */
    static final int EXCEPTION_INDEX_MASK = 0x0000FFFF;
 
    /**
     * We take advantage of the variables being in a closed range to save a bit per CE.
     * The low and high values are initially set to be at the opposite ends of the range,
     * as the table is built from the UCA data, they are narrowed in.
     * The first three values are used in building; the last two in testing.
    */
    int variableLow = '\uFFFF';
    int nonVariableLow = '\uFFFF';
    int variableHigh = '\u0000';
    
    int variableLowCE;  // used for testing against
    int variableHighCE; // used for testing against
    
    /**
     * Although a single character can expand into multiple CEs, we don't want to burden
     * the normal case with the storage. So, they get a special value in the collationElements
     * array. This value has a distinct primary weight, followed by an index into a separate
     * table called expandingTable. All of the CEs in that table, up to a TERMINATOR value
     * will be used for the expansion. The implementation is as a stack; this just makes it
     * easy to generate.
     */
    IntStack expandingTable = new IntStack(3600); // initial number is from compKeys
        
    /**
     * Used to terminate a list of CEs, as in expandingTable
     */
    static final int TERMINATOR = 0xFFFFFFFF;   // CE that marks end of string
         
    /**
     * For now, this is just a simple mapping of strings to collation elements.
     * The implementation depends on the contracting characters being "completed",
     * so that it can be efficiently determined when to stop looking.
     */
    Hashtable contractingTable = new Hashtable();
    
    /**
     *  Special char value that means failed or terminated
     */
    static final char NOT_A_CHAR = '\uFFFF';
    
    /**
     * Marks whether we are using the full data set, or an abbreviated version for
     * an applet.
     */
     
    private boolean fullData;
    
// =============================================================
// Temporaries used in getCE. 
// Made part of the object to avoid reallocating each time.
// =============================================================

    /**
     * Stack for expanding characters
     */
    private IntStack expandingStack = new IntStack(100);
    
    /**
     * Temporary buffers used in getSortKey to store weights
     * these are NOT strings of Unicode characters--they are
     * lists of weights. But this is a convenient way to store them,
     * since Java doesn't have unsigned shorts.
     */
    private StringBuffer primaries = new StringBuffer(100);
    private StringBuffer secondaries = new StringBuffer(100);
    private StringBuffer tertiaries = new StringBuffer(100);
    private StringBuffer quaternaries = new StringBuffer(100);
    
    /**
     * Temporary buffer used to collect progress data for debugging
     */
    StringBuffer debugList = new StringBuffer(100);
    
    /**
     * Temporary with requested decomposition
     */
    boolean storedDecomposition;
    int hangulHackBottom;
    int hangulHackTop;
    
    /**
     * Used for supporting Thai rearrangement
     */
    static final char EMPTY = '\uFFFF';
    char rearrangeBuffer = EMPTY;
    String rearrangeList = "";

// =============================================================
// getCE: Get the next Collation Element
// Main Routine
// =============================================================

    /**
     * Gets the next Collation Element from the decomposition buffer.
     * May take one or more characters.
     * Resets index to point at the next position to get characters from.
     *@param quaternary the collection of 4th level weights, synthesized from the
     * (normalized) character code.
     */
    private int getCE() {
        if (!expandingStack.isEmpty()) return expandingStack.pop();
        char ch;
        
        // Fetch next character. Handle rearrangement for Thai, etc.
        if (rearrangeBuffer != EMPTY) {
            ch = rearrangeBuffer;
            rearrangeBuffer = EMPTY;
        } else {
            if (index >= decompositionBuffer.length()) return TERMINATOR;
            ch = decompositionBuffer.charAt(index++); // get next
            if (rearrangeList.indexOf(ch) != -1 && index < decompositionBuffer.length()) {// if in list
                rearrangeBuffer = ch;   // store for later
                ch = decompositionBuffer.charAt(index++);   // never rearrange twice!!
            }
        }
        
        int ce = collationElements[ch];
        
        // Hangul tailoring hack
        //if (!storedDecomposition && hangulHackBottom <= ce && ce < hangulHackTop) return fixJamo(ch, ce);   // hard coded fix!!

        // if the CE is not exceptional (unsupported, contracting, expanding) we are done.
        if ((ce & EXCEPTION_CE_MASK) != EXCEPTION_CE_MASK) return ce;
        
        if (ce == UNSUPPORTED) {
            int bigChar = ch;
            
            // Special check for Han, Hangul
            if (0x3400 <= bigChar && bigChar <= 0xD7A3) {
                if (bigChar <= 0x4DB5 || 0xAC00 <= bigChar || (0x4E00 <= bigChar && bigChar <= 0x9FA5)) {
                    return (bigChar << 16) | NEUTRAL_SECONDARY_TERTIARY;
                }
            }
                        
            // special check for unsupported surrogate pair, 20 1/8 bits
            if (0xD800 <= bigChar && bigChar <= 0xDFFF) {
                // ignore unmatched surrogates (e.g. return zero)
                if (bigChar >= 0xDC00 || index >= decompositionBuffer.length()) return 0; // unmatched
                int ch2 = decompositionBuffer.charAt(index);
                if (ch2 < 0xDC00 || 0xDFFF < ch2) return 0;  // unmatched
                index++; // skip next char
                bigChar = 0x10000 + ((ch - 0xD800) << 10) + (ch2 - 0xDC00); // extract value
            }

            if ((bigChar & 0xFFFE) == 0xFFFE) { // illegal code value, ignore!!
                return 0;
            }
            
            // The result is 2 CEs. One is UNSUPPORTED + top bits, and the other
            // is a primary that is the next fifteen bits
            // This has the effect of putting all unsupported characters at the end,
            // in code order.
                    // add bottom 5 bits to UNSUPPORTED, and push rest
                    //return UNSUPPORTED + (bigChar & 0xFFFF0000);    // top bits added
            expandingStack.push((bigChar << 17) | 0x10000); // primary = bottom 15 bits plus turn bottom bit on.
            // secondary and tertiary are both zero
            return UNSUPPORTED + ((bigChar << 1) & 0xFFFF0000); // top 34 values plus UNSUPPORTED
        }
        if (ce == CONTRACTING) {
            // Contracting is probably the most interesting (read "tricky") part
            // of the algorithm.
            // First get longest substring that is in the contracting table.
            // For simplicity, we use a hash table for contracting.
            // There are much better optimizations, 
            // but they take a more complicated build algorithm than we want to show here.
            // NOTE: We are guaranteed that the character itself is in the contracting table because
            // of the build process.
            String probe = String.valueOf(ch);
            Object value = contractingTable.get(probe);
            
            // We loop, trying to add successive characters to the longest substring.
            while (index < decompositionBuffer.length()) {
                char ch2 = decompositionBuffer.charAt(index);
                
                // see whether the current string plus the next char are in
                // the contracting table.
                String newProbe = probe + ch2;
                Object newValue = contractingTable.get(newProbe);
                if (newValue == null) break;    // stop if not in table.
                
                // We succeeded--so update our new values, and set index
                // and quaternary to indicate that we swallowed another character.
                probe = newProbe;
                value = newValue;
                index++;
            }
            
            // Now, see if we can add any combining marks
            short lastCan = 0;
            for (int i = index; i < decompositionBuffer.length(); ++i) {
                // We only take certain characters. They have to be accents,
                // and they have to not be blocked.
                // Unlike above, if we don't find a match (and it was an accent!)
                // then we don't stop, we continue looping.
                char ch2 = decompositionBuffer.charAt(i);
                short can = toD.getCanonicalClass(ch2);
                if (can == 0) break;            // stop with any zero (non-accent)
                if (can == lastCan) continue;   // blocked if same class as last
                lastCan = can;                  // remember for next time
                
                // Now see if we can successfully add it onto our string
                // and find it in the contracting table.
                String newProbe = probe + ch2;
                Object newValue = contractingTable.get(newProbe);
                if (newValue == null) continue;

                // We succeeded--so update our new values, remove the char, and update
                // quaternary to indicate that we swallowed another character.
                probe = newProbe;
                value = newValue;
                decompositionBuffer.setCharAt(i,'\u0000');  // zero char
            }
            
            // we are all done, and can extract the CE from the last value set.
            ce = ((Integer)value).intValue();
            // if the CE is not exceptional (unsupported expanding) we are done.
            // BTW we will never have a contracting CE at this point.
            if ((ce & EXCEPTION_CE_MASK) != EXCEPTION_CE_MASK) return ce;
            // otherwise fall through to expansion
        }
        // expanding, so copy list of items onto stack
        int index = ce & EXCEPTION_INDEX_MASK; // get index
        // copy onto stack from index until reach TERMINATOR
        while (true) {
            ce = expandingTable.get(index++);
            if (ce == TERMINATOR) break;
            expandingStack.push(ce);
        }
        return expandingStack.pop(); // pop last (guaranteed to exist!)
    }
    
    /**
     * Constants for Han
     */
    static final int // constants
        SBase = 0xAC00, LBase = 0x1100, VBase = 0x1161, TBase = 0x11A7,
        LCount = 19, VCount = 21, TCount = 28,
        NCount = VCount * TCount,   // 588
        SCount = LCount * NCount,   // 11172
        LastInitial = LBase + LCount-1, // last initial jamo
        LastPrimary = SBase + (LCount-1) * VCount * TCount; // last corresponding primary
        
    /**
     * Fix for Hangul, since the tables are not set up right.
     * The fix for Hangul is to give different values to the combining initial 
     * Jamo to put them up into the AC00 range, as follows. Each one is put
     * after the first syllable it begins.
     */ 
    private int fixJamo(char ch, int jamoCe) {
        
        int result = jamoCe - hangulHackBottom + 0xAC000000; // put into right range
        if (DEBUG) System.out.println("Changing " + hex(ch) + " " + hex(jamoCe) + " => " + hex(result));
        return result;
        /*
        int newPrimary;
        int LIndex = jamo - LBase;
        if (LIndex < LCount) {
            newPrimary = SBase + (LIndex + 1) * VCount * TCount; // multiply to match syllables
        } else {
            newPrimary = LastPrimary + (jamo - LastInitial); // just shift up
        }
        return makeKey(newPrimary, 0x21, 0x2);  // make secondary difference!
        */
    }
    
// =============================================================
// Building Collation Element Tables
// =============================================================

    /**
     * Value for returning int as well as function return,
     * since Java doesn't have output parameters
     */
    private int[] position = new int[1]; 
    
    /**
     * For recording statistics
     */
    private int count1 = 0, count2 = 0, count3 = 0, max2 = 0, max3 = 0;
    private int oldKey1 = -1, oldKey2 = -1, oldKey3 = -1;
    Hashtable multiTable = new Hashtable();
    BitSet found = new BitSet();
    
    public Hashtable getContracting() {
        return multiTable;
    }
    
    /**
     * Adds the collation elements from a file (or other stream) in the UCA format.
     * Values will override any previous mappings.
     */
    private void addCollationElements(BufferedReader in) throws java.io.IOException {
        IntStack tempStack = new IntStack(100); // used for reversal
        StringBuffer multiChars = new StringBuffer(); // used for contracting chars
        String inputLine = "";
        while (true) try {
            inputLine = in.readLine();
            if (inputLine == null) break;       // means file is done
            String line = cleanLine(inputLine); // remove comments, extra whitespace
            if (line.length() == 0) continue;   // skip empty lines

            position[0] = 0;                    // start at front of line
            if (line.startsWith("@version")) {
                dataVersion = line.substring("@version".length()+1).trim();
                continue;
            }
            
            if (line.startsWith("@rearrange")) {
                line = line.substring("@rearrange".length()+1).trim();
                while (position[0] < line.length()) {
                    rearrangeList += getChar(line, position);
                }
                continue;
            }
            
            // collect characters
            char value = getChar(line, position);
            char value2 = getChar(line, position);
            multiChars.setLength(0);            // clear buffer
            if (value2 != NOT_A_CHAR) {
                multiChars.append(value);       // append until we get terminator
                multiChars.append(value2);
                while (true) {
                    value2 = getChar(line, position);
                    if (value2 == NOT_A_CHAR) break;
                    multiChars.append(value2);
                }
            }
            if (RECORDING_CHARS) {
                if (multiChars.length() > 1) {
                    multiTable.put(multiChars.toString(), "");
                }
                found.set(value);
                for (int i = 1; i < multiChars.length(); ++i) {
                    found.set(multiChars.charAt(i));
                }
            }
            if (!fullData && RECORDING_DATA) {
                if (value == 0 || value == '\t' || value == '\n' || value == '\r'
                  || (0x20 <= value && value <= 0x7F)
                  || (0x80 <= value && value <= 0xFF)
                  || (0x300 <= value && value <= 0x3FF)
                  ) {
                    System.out.println("    + \"" + inputLine + "\\n\"");
                }
            }
            
            // collect CEs
            int ce = getCEFromLine(value, line, position);
            int ce2 = getCEFromLine(value, line, position);
            if (CHECK_UNIQUE && (ce2 == TERMINATOR || CHECK_UNIQUE_EXPANSIONS)) {
                if (!CHECK_UNIQUE_VARIABLES) {
                    checkUnique(value, ce, 0, inputLine); // only need to check first value
                } else {
                    int key1 = ce >>> 16;
                    if (isVariable(ce)) {
                        checkUnique(value, 0, key1, inputLine); // only need to check first value
                    }
                }
            }
            if (ce2 != TERMINATOR) { // have expanding character!
                // put list into the expanding table
                // use a temporary stack to get them in reverse order
                tempStack.push(ce);
                tempStack.push(ce2);
                // set collationElement to exception value, plus index
                ce = EXPANDING_MASK | expandingTable.getTop();
                while (true) {
                    ce2 = getCEFromLine(value, line, position);
                    if (ce2 == TERMINATOR) break;
                    tempStack.push(ce2);
                } 
                // push onto expanding table, now in reverse order
                while (!tempStack.isEmpty()) expandingTable.push(tempStack.pop());
                expandingTable.push(TERMINATOR);
            }
            
            // assign CE(s) to char(s)
            if (multiChars.length() > 0) {
                contractingTable.put(multiChars.toString(), new Integer(ce));
                if (collationElements[value] == UNSUPPORTED) {
                    collationElements[value] = CONTRACTING; // mark special
                } else if (collationElements[value] != CONTRACTING) {
                    // move old value to contracting table!
                    contractingTable.put(String.valueOf(value), new Integer(collationElements[value]));
                    collationElements[value] = CONTRACTING; // signal we must look up in table
                }
            } else if (collationElements[value] == CONTRACTING) {
                // must add old value to contracting table!
                contractingTable.put(String.valueOf(value), new Integer(ce));
            } else {
                collationElements[value] = ce; // normal
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Malformed line: " + inputLine + "\n " 
              + e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    /**
     * Checks the internal tables corresponding to the UCA data.
     */
    private void cleanup() {
        
        // at this point, we have to guarantee that the contractingTable is CLOSED
        // e.g. if a substring of length n is in the table, then the first n-1 characters
        // are also!!
        
        Hashtable missingStrings = new Hashtable();
        
        Enumeration enum = contractingTable.keys();
        while (enum.hasMoreElements()) {
            String sequence = (String)enum.nextElement();
            for (int i = sequence.length()-1; i > 0; --i) {
                String shorter = sequence.substring(0,i);
                Object probe = contractingTable.get(shorter);
                if (probe == null) {
                    missingStrings.put(shorter,"");
                    collationElements[sequence.charAt(0)] = UNSUPPORTED; // nuke all bad values
                }
            }
        }
        
        if (missingStrings.size() != 0) {
            String errorMessage = "";
            enum = missingStrings.keys();
            while (enum.hasMoreElements()) {
                String missing = (String)enum.nextElement();
                if (errorMessage.length() != 0) errorMessage += ", ";
                errorMessage += "\"" + missing + "\"";
            }
            throw new IllegalArgumentException("Contracting table not closed! Missing " + errorMessage);
        }
        
        //fixlater;
        variableLowCE = variableLow << 16;
        variableHighCE = (variableHigh << 16) | 0xFFFF; // turn on bottom bits
        
        hangulHackBottom = collationElements[0x1100] & 0xFFFF0000; // remove secondaries & tertiaries
        hangulHackTop = collationElements[0x11F9] | 0xFFFF; // bump up secondaries and tertiaries
        if (DEBUG) System.out.println("Hangul Hack: " + hex(hangulHackBottom) + ", " + hex(hangulHackTop));
        
        // show some statistics
        if (DEBUG) System.out.println("count1: " + count1);
        if (DEBUG) System.out.println("count2: " + max2);
        if (DEBUG) System.out.println("count3: " + max3);
        
        if (DEBUG) System.out.println("MIN1/MAX1: " + hex(MIN1) + "/" + hex(MAX1));
        if (DEBUG) System.out.println("MIN2/MAX2: " + hex(MIN2) + "/" + hex(MAX2));
        if (DEBUG) System.out.println("MIN3/MAX3: " + hex(MIN3) + "/" + hex(MAX3));
        
        if (DEBUG) System.out.println("Var Min/Max: " + hex(variableLow) + "/" + hex(variableHigh));
        if (DEBUG) System.out.println("Non-Var Min: " + hex(nonVariableLow));
        
        if (DEBUG) System.out.println("renumberedVariable: " + renumberedVariable);
    }
    
    /**
     * Remove comments, extra whitespace
     */
    private String cleanLine(String line) {
        int commentPosition = line.indexOf('#');
        if (commentPosition >= 0) line = line.substring(0,commentPosition);
        commentPosition = line.indexOf('%');
        if (commentPosition >= 0) line = line.substring(0,commentPosition);
        return line.trim();
    }
    
    /**
     * Get a char from a line, of form: (<space> | <comma>)* <hex>*
     *@param position on input, the place to start at. 
     * On output, updated to point to the next place to search.
     *@return the character, or NOT_A_CHAR when done
     */
    private char getChar(String line, int[] position) {
        int start = position[0];
        char ch;
        while (true) { // trim whitespace
            if (start >= line.length()) return NOT_A_CHAR;
            ch = line.charAt(start);
            if (ch != ' ' && ch != ',') break;
            start++;
        }
        // from above, we have at least one char
        if ((ch >= '0' && ch <= '9') || (ch >= 'A' && ch <= 'F')) {
            position[0] = start + 4;
            return (char)Integer.parseInt(line.substring(start,start+4),16);
        }
        return NOT_A_CHAR; 
    }
    
    /**
     * Gets a CE from a UCA format line
     *@param value the first character for the line. Just used for statistics.
     *@param line a string of form "[.0000.0000.0000.0000]..."
     *@param position on input, the place to start at. 
     * On output, updated to point to the next place to search.
     */
    
    private int getCEFromLine(char value, String line, int[] position) {
        int start = line.indexOf('[', position[0]);
        if (start == -1) return TERMINATOR;
        boolean variable = line.charAt(start+1) == '*';
        int key1 = Integer.parseInt(line.substring(start+2,start+6),16);
        if (key1 == 0 && variable) {
            System.out.println("Zero L1s cannot be variable!!: " + line);
            variable = false; // FIX DATA FILE
        }
        int key2 = Integer.parseInt(line.substring(start+7,start+11),16);
        if (key2 > 0xFF) {
            key2 = key2 - 0xC0; // this works because of the way the data is set up.
            if (key2 > 0xFF) {
                throw new IllegalArgumentException("Weight2 doesn't fit: " + hex(key2) + "," + line);
            }
        }
        int key3 = Integer.parseInt(line.substring(start+12,start+16),16);
        if (key3 > 0xFF) {
            throw new IllegalArgumentException("Weight3 doesn't fit: " + hex(key3) + "," + line);
        }
        // adjust variable bounds, if needed
        if (variable) {
            if (key1 < variableLow) variableLow = key1;
            if (key1 > variableHigh) variableHigh = key1;
        } else {
            if (key1 != 0 && key1 < nonVariableLow) nonVariableLow = key1;
        }
            
        // statistics
        count1++;
        if (key1 != oldKey1) {
            oldKey1 = key1;
            if (count2 > max2) max2 = count2;
            if (count3 > max3) max3 = count3;
            count2 = count3 = 1;
        } else {
            count2++;
            if (key2 != oldKey2) {
                oldKey2 = key2;
                if (count3 > max3) max3 = count3;
                count3 = 1;
            } else {
                count3++;
            }
        }
        position[0] = start + 17;
        /*
        if (VARIABLE && variable) {
            key1 = key2 = key3 = 0;
            if (CHECK_UNIQUE) {
                if (key1 != lastUniqueVariable) renumberedVariable++;
                result = renumberedVariable;     // push primary down
                lastUniqueVariable = key1;
                key3 = key1;
                key1 = key2 = 0;
            }
        }
        */
        // gather some statistics
        if (key1 != 0 && key1 < MIN1) MIN1 = (char)key1;
        if (key2 != 0 && key2 < MIN2) MIN2 = (char)key2;
        if (key3 != 0 && key3 < MIN3) MIN3 = (char)key3;
        if (key1 > MAX1) MAX1 = (char)key1;
        if (key2 > MAX2) MAX2 = (char)key2;
        if (key3 > MAX3) MAX3 = (char)key3;
        return makeKey(key1, key2, key3);
    }
    
    /**
     * Just for statistics
     */
    int lastUniqueVariable = 0;
    int renumberedVariable = 50;
    char MIN1 = '\uFFFF'; // start large; will be reset as table is built
    char MIN2 = '\uFFFF'; // start large; will be reset as table is built
    char MIN3 = '\uFFFF'; // start large; will be reset as table is built
    char MAX1 = '\u0000'; // start small; will be reset as table is built
    char MAX2 = '\u0000'; // start small; will be reset as table is built
    char MAX3 = '\u0000'; // start small; will be reset as table is built
    
    /**
     * Used for checking data file integrity
     */
    private Hashtable uniqueTable = new Hashtable();
    
    /**
     * Used for checking data file integrity
     */
    private void checkUnique(char value, int result, int fourth, String line) {
        if (toD.hasDecomposition(value)) return; // don't check decomposables.
        Object ceObj = new Long(((long)result << 16) | fourth);
        Object probe = uniqueTable.get(ceObj);
        if (probe != null) {
            System.out.println("CE(" + hex(value) 
              + ")=CE(" + hex(((Character)probe).charValue()) + "); " + line);
              
        } else {
            uniqueTable.put(ceObj, new Character(value));
        }
    }
}

// =============================================================
// Simple stack mechanism, with push, pop and access
// =============================================================

final class IntStack {
    private int[] values;
    private int top = 0;
    
    public IntStack(int initialSize) {
        values = new int[initialSize];
    }
    
    public void push(int value) {
        if (top >= values.length) { // must grow?
            int[] temp = new int[values.length*2];
            System.arraycopy(values,0,temp,0,values.length);
            values = temp;
        }
        values[top++] = value;
    }
    
    public int pop() {
        if (top > 0) return values[--top];
        throw new IllegalArgumentException("Stack underflow");
    }
    
    public int get(int index) {
        if (0 <= index && index < top) return values[index];
        throw new IllegalArgumentException("Stack index out of bounds");
    }
    
    public int getTop() {
        return top;
    }
    
    public boolean isEmpty() {
        return top == 0;
    }
}