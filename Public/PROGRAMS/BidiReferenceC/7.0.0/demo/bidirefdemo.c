/*
**      Unicode Bidirectional Algorithm
**      Reference Implementation Demo Program
**      (c) Copyright 2013
**      Unicode, Inc. All rights reserved.
**      Unpublished rights reserved under U.S. copyright laws.
*/

/*
 * bidirefdemo.c
 *
 * Unicode Bidirectional Algorithm (UBA)
 * Reference Implementation Demo Program
 *
 * This is a simple demo program illustrating the use
 * of the public API defined in bidiref.h to run
 * individual, externally-defined bidi test cases.
 */

/*
 * Change history:
 *
 * 2013-Jun-02 kenw   Created.
 * 2013-Jun-20 kenw   Updated error handling.
 */

#include <string.h>
#include <stdio.h>

#include "bidiref.h"


/***********************************************************/

/*
 * Define a few macros for characters with particular
 * bidi classes, to make the definition of example
 * strings easier to interpret.
 */

#define C_L1  0x0061
#define C_R1  0x05D0
#define C_AL1 0x0627
#define C_EN1 0x0030
#define C_AN1 0x0600
#define C_ET1 0x0024
#define C_ES1 0x002D
#define C_CS1 0x002C 
#define C_WS1 0x0020
#define C_ON1 0x0022
#define C_ON2 0x002E
#define C_ON3 0x0028
#define C_ON4 0x0029
#define C_ON5 0x005B
#define C_ON6 0x005D
#define C_BN1 0x200B
#define C_NSM 0x0300
#define C_LRE 0x202A
#define C_RLE 0x202B
#define C_PDF 0x202C
#define C_LRI 0x2066
#define C_RLI 0x2067
#define C_FSI 0x2068
#define C_PDI 0x2069
#define C_B1  0x000A

/*
 * Define an example test case for the demo.
 */

/*
 * This is the input string for the example, expressed as a UTF-32 vector.
 *
 * For this short demo, this value is just defined here statically. Of
 * course the input string can come from anywhere, depending on what
 * you are using the bidiref API to do.
 */

#define TESTCASENUM 1
#define EXAMPLELEN 13

static U_Int_32 example[EXAMPLELEN] = { C_R1, C_RLI, C_L1, C_LRI, C_L1, C_RLE, C_L1,
                                C_PDF, C_L1, C_PDI, C_L1, C_PDI, C_L1 };

/*
 * These are the expected results of this test case. These use the
 * same format defined in BidiCharacterTest.txt. They are passed
 * in with the input string, when calling br_ProcessOneTestCase.
 */
static int exampleEmbeddingLevel = 1;
static char *exampleLevels = "1 1 4 4 4 x 6 x 4 4 4 1 2";
static char *exampleOrder  = "12 11 2 3 4 6 8 9 10 1 0";

/*
 * Define an output buffer length. This value is just set to 40 for
 * the demo, but when using br_QueryOneTestCase, make sure that
 * the output buffers are long enough to hold the expected results
 * for the level and order strings, based on how long the input
 * strings are.
 */
#define BUFLEN 40

/***********************************************************/

main ( int argc, char *argv[] )
{
int rc;
int myEmbeddingLevel;
char myLevels[BUFLEN];
char myOrder[BUFLEN];

    /*
     * Do whatever with program arguments, if desired.
     */

    /*
     * Set a specific trace flag. This one will show
     * trace information about property table loading.
     * It isn't required, of course.
     */

    TraceOn ( Trace10 );

    /*
     * Call br_Init first to set the UBA version and load
     * the required character property tables. It is only
     * necessary to call this once, but it should be called
     * before any invocation of br_ProcessOneTestCase or
     * br_QueryOneTestCase.
     */

    rc = br_Init ( UBA63 );
    if ( rc != 1 )
    {
        printf ( "Error in initialization.\n" );           
        return ( rc );
    }

    /*
     * Set a combination of trace flags. These particular flags
     * together will provide a trace listing of the entries to main
     * rule invocations, and also show intermediate results
     * of their application to the input string.
     * The trace flags aren't required, of course, but show helpful
     * output to understand the application of the UBA rules.
     */

    TraceOn ( Trace1 | Trace11 );

    /*
     * Now invoke br_ProcessOneTestCase with the input string and
     * expected results. Using 2 for the paragraphDirection requests
     * auto-LTR for the paragraph direction, the default for UBA.
     */

    rc = br_ProcessOneTestCase ( TESTCASENUM, example, EXAMPLELEN, 2, 
        exampleEmbeddingLevel, exampleLevels, exampleOrder );

    if ( rc == 1 )
    {
        printf ( "\nMy example test case #1 passed!\n" );
    }
    else
    {
        printf ( "Oops! Something went wrong. I better look at the trace output.\n" );
    }

    /*
     * Print a display delimiter next, to more easily see where the
     * following query example starts.
     */

    printf ( "\n\n=======================================================\n\n");

    /*
     * Now invoke br_QueryOneTestCase with the same input string.
     * This runs UBA without showing any display output. Instead,
     * the results of running UBA on the input string are placed
     * in the output parameters, for the calling code to check.
     */

    rc = br_QueryOneTestCase ( example, EXAMPLELEN, 2, &myEmbeddingLevel,
        myLevels, BUFLEN, myOrder, BUFLEN );

    if ( rc == 1 )
    {
        printf ( "My example query worked.\n" );
        /*
         * Now the output parameters can be checked against external
         * criteria. In this case, for demo purposes, we can just check
         * that they match the statically defined example values.
         */
        if ( myEmbeddingLevel != exampleEmbeddingLevel )
        {
            printf ( "Unexpected embedding level %d\n", myEmbeddingLevel );
        }
        if ( strcmp ( myLevels, exampleLevels ) != 0 )
        {
            printf ( "Unexpected levels: [%s]\n", myLevels );
        }
        if ( strcmp ( myOrder, exampleOrder ) != 0 )
        {
            printf ( "Unexpected order:  [%s]\n", myOrder );
        }
    }
    else
    {
        printf ( "Oops! Something went wrong. The error return code was %d.\n", rc );
    }

    /*
     * No cleanup is required for the bidiref API. Just return.
     */

    return (1);
}

