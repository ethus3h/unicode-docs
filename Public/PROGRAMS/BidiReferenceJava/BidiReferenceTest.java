package org.unicode.bidi;

/*
 * (C) Copyright IBM Corp. 1999, All Rights Reserved
 * (C) Copyright ASMUS, Inc. 2013, All Rights Reserved
 * 
 * Distributed under the Terms of Use in http://www.unicode.org/copyright.html. *
 * version 1.0
 */

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;

/**
 * A simple command-line interface to the BidiReference class.
 * <p>
 * This prompts the user for an ASCII string, runs the reference
 * algorithm on the string, and displays the results to the terminal.
 * An empty return to the prompt exits the program.
 * <p>
 * ASCII characters are preassigned various bidi direction types.
 * These types can be displayed by the user for reference by
 * typing <code>-display</code> at the prompt.  More help can be
 * obtained by typing <code>-help</code> at the prompt.
 * <p>
 * Updated to allow testing of paired bracket algorithm extension for Unicode 6.3
 */
public class BidiReferenceTest {
    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
    PrintWriter writer = new PrintWriter(new BufferedOutputStream(System.out));
    BidiReferenceTestCharmap charmap = BidiReferenceTestCharmap.TEST_ARABIC;
    byte baseDirection = BidiReference.implicitEmbeddingLevel;

    // for running bidi PBA algorithm in test mode
    byte doPBATest = 0;
    
    /**
     * Run the interactive test.
     */
    public static void main(String args[]) {
        new BidiReferenceTest().run();
    }

    void run() {
        printHelp();

        while (true) {
            writer.print("> ");
            writer.flush();
            String input;
            try {
                input = reader.readLine();
            }
            catch (final Exception e) {
                writer.println(e);
                continue;
            }

            if (input.length() == 0) {
                writer.println("Bye!");
                writer.flush();
                return;
            }

            if (input.charAt(0) == '-') { // command
                int limit = input.indexOf(' ');
                if (limit == -1) {
                    limit = input.length();
                }
                final String cmd = input.substring(0, limit);
                if (cmd.equals("-display")) {
                    charmap.dumpInfo(writer);
                } else if (cmd.equals("-english")) {
                    charmap = BidiReferenceTestCharmap.TEST_ENGLISH;
                    charmap.dumpInfo(writer);
                } else if (cmd.equals("-hebrew")) {
                    charmap = BidiReferenceTestCharmap.TEST_HEBREW;
                    charmap.dumpInfo(writer);
                } else if (cmd.equals("-arabic")) {
                    charmap = BidiReferenceTestCharmap.TEST_ARABIC;
                    charmap.dumpInfo(writer);
                } else if (cmd.equals("-mixed")) {
                    charmap = BidiReferenceTestCharmap.TEST_MIXED;
                    charmap.dumpInfo(writer);
                } else if (cmd.equals("-baseLTR")) {
                    baseDirection = 0;
                } else if (cmd.equals("-baseRTL")) {
                    baseDirection = 1;
                } else if (cmd.equals("-baseDefault")) {
                    baseDirection = BidiReference.implicitEmbeddingLevel;
                } else if (cmd.equals("-pba")) {
                    charmap = BidiReferenceTestCharmap.TEST_PBA;
                    charmap.dumpInfo(writer);
                 	doPBATest = 1;
                } else {
                    printHelp();
                }
            } else {
                runSample(input);
            }
        }
    }

    /**
     * Display instructions to the user.
     */
    void printHelp() {
        writer.println("Bidi Reference Interactive Test");
        writer.println();
        writer.println("To exit the program, hit return or enter at the prompt without typing any text");
        writer.println("To run the bidi algorithm, just enter some text (without a leading '-')");
        writer.println();
        writer.println("To see the current mapping of characters to Bidi types, enter '-display'");
        writer.println("To switch the mapping to english, enter '-english'");
        writer.println("To switch the mapping to hebrew for upper case, enter '-hebrew'");
        writer.println("To switch the mapping to arabic for upper case and numbers, enter '-arabic'");
        writer.println("To switch the mapping to mixed hebrew and arabic for upper case and numbers, enter '-mixed'");
        writer.println("To switch the mapping to map parens to ON and select PBA test, enter -pba");
        writer.println();
        writer.println("To force an LTR base direction, enter '-baseLTR'");
        writer.println("To force an RTL base direction, enter '-baseRTL'");
        writer.println("To compute the default base direction using the algorithm, enter '-baseDefault'");
        writer.println();
        writer.println("To display this help message, enter '-help'");
    }

    /**
     * Run the BidiReference algorithm over the string using the current character to direction code mapping.
     */
    void runSample(String str) {
   
        try {
            final byte[] codes = charmap.getCodes(str);
            BidiTestBracketMap map = BidiTestBracketMap.TEST_BRACKETS;
            byte[] pbTypes = map.getBracketTypes(str);
            int[]pbValues = map.getBracketValues(str);
  
            final BidiReference bidi = new BidiReference(codes, pbTypes, pbValues, baseDirection);
            final int[] reorder = bidi.getReordering(new int[] { codes.length });

            writer.println("base level: " + bidi.getBaseLevel() + (baseDirection != BidiReference.implicitEmbeddingLevel ? " (forced)" : ""));

            // output original text
            for (int i = 0; i < str.length(); ++i) {
                displayChar(str.charAt(i));
            }
            writer.println();
            
            if (doPBATest ==1)
            {
            	// report on paired bracket algorithm
	            writer.println();
	            writer.println("bracket pairs at:\n" + bidi.pba.getPairPositionsString()); /*bidi.pba.pairPositions.toString()*/
	            writer.println("(last isolated run sequence processed, in relative offsets)");
	            writer.println();
	            writer.print("resolved directional types: "); 
	            charmap.dumpCodes(writer, bidi.getResultTypes());
            }
            

            // output visually ordered text
            for (int i = 0; i < str.length(); ++i) {
                displayChar(str.charAt(reorder[i]));
            }
            writer.println();
        }
        catch (final Exception e) {
            writer.println(e);
        }
        writer.println();
    }

    void displayChar(char c) {
        if (c < '\u0010') {
            writer.print("0x0" + Integer.toHexString(c));
        } else if (c < '\u0020' || c >= '\u007f') {
            writer.print("0x" + Integer.toHexString(c));
        } else {
            writer.print(c);
        }
    }
}

