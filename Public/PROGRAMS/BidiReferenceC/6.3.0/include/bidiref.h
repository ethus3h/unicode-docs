/*
**      Unicode Bidirectional Algorithm
**      Reference Implementation
**      (c) Copyright 2013
**      Unicode, Inc. All rights reserved.
**      Unpublished rights reserved under U.S. copyright laws.
*/

/*
 * bidiref.h
 *
 * Public API for libbidir.
 */

/*
 * Change history:
 *
 * 2013-Jun-02 kenw   Created.
 * 2013-Jun-19 kenw   Added Trace15.
 * 2013-Jul-08 kenw   Public BR_MAXINPUTLEN.
 */

#ifndef __BIDIREF_LOADED
#define __BIDIREF_LOADED

#ifdef  __cplusplus
extern "C" {
#endif

typedef unsigned long U_Int_32;

/*
 * Set a limit to the longest input string the bidiref implementation
 * will handle. This helps bombproof the input handling against
 * various kinds of bad input data. Increase this value to enable
 * checking of longer strings.
 */

#define BR_MAXINPUTLEN 76

/*
 * Trace flags for control of debug output.
 *
 * These trace flags are bits set in the global traceFlags
 * unsigned 32-bit int.
 */

#define Trace0 (1)     /* On by default: print final test results */
#define Trace1 (1<<1)  /* Trace main algorithm function entry. */
#define Trace2 (1<<2)  /* Trace initialization code function entry. */
#define Trace3 (1<<3)  /* Trace stack handling in X1-X8 */
#define Trace4 (1<<4)  /* Additional debug output for X1-X8 */
#define Trace5 (1<<5)  /* Trace run and sequence handling in X10 */
#define Trace6 (1<<6)  /* Trace stack handling in N0 */
#define Trace7 (1<<7)  /* Additional debug output for N0 */
#define Trace8 (1<<8)  /* Trace reordering in L2 */
#define Trace9 (1<<9)  /* Additional debug output for P2/P3 */
#define Trace10 (1<<10)  /* Trace property file parsing. */

#define Trace11 (1<<11)  /* Display all intermediate UBA results */
#define Trace12 (1<<12)  /* Additional debug output for test parsing */

/*
 * Trace13 is off by default. When Trace13 is off, only FAIL
 * results for test cases as explicitly noted on a per test
 * basis. If Trace13 is turned on, then each PASS result will
 * also be explicitly noted.
 *
 * Trace13 is not recommended when running conformance testing
 * with BidiTest.txt, as that would produce hundreds of thousands
 * of lines of output, one for each passing test case.
 */

#define Trace13 (1<<13)  /* Explicitly list each PASS result for tests */

/*
 * Trace14 is off by default.
 *
 * When Trace14 is on, then any intermediate UBA results display
 * (Trace11) is omitted
 * when application of that rule is vacuous and has no effect on
 * the string being processed.
 *
 * Trace14 does not cause omission of additional debug output
 * related to other trace flags.
 */
#define Trace14 (1<<14)  /* Omit vacuous rule application from display */

/*
 * Trace15 is on by default.
 *
 * When Trace15 is on, errors in processing are logged to the
 * console.
 *
 * When Trace15 is off, no console error logging is done. 
 */
#define Trace15 (1<<15)  /* Turn on console error logging */

/*
 * TraceAll
 *
 * A convenience macro to turn on all trace flags.
 */
#define TraceAll (Trace0 | Trace1 | Trace2 | Trace3 | Trace4 | \
                  Trace5 | Trace6 | Trace7 | Trace8 | Trace9 | \
                  Trace10 | Trace11 | Trace12 | Trace13 | \
                  Trace14 | Trace15)

/*
 * Version defines for support of distinct versions
 * of UBA.
 *
 * These defines may be extended, if further distinctions
 * make sense. For now, just use UBA62 as the base
 * version, and define UBA63 as a distinct version for
 * the purposes of separating the details of the
 * algorithm which have changed for UBA 6.3.0.
 */

#define UBA62 (0)
#define UBA63 (1)

/*
 * Public Function Declarations
 */

extern void TraceOn ( U_Int_32 traceValue );
extern void TraceOff ( U_Int_32 traceValue );

extern int br_Init ( int version );

/*
 * br_ProcessOneTestCase
 *
 * Documentation TBD.
 */

extern int br_ProcessOneTestCase ( int testCaseNumber, U_Int_32 *text, int textLen, 
    int paragraphDirection, int expEmbeddingLevel, char *expLevels, char *expOrder );

/*
 * br_QueryOneTestCase
 *
 * Documentation TBD.
 *
 * Returns 1 if all goes well.
 * Error return values:
 *    -1  Error in formatting output parameters.
 *    -2  Initialization not completed before call.
 *    -3  Error in context construction.
 *    -4  Bad return from UBA processing
 *
 * br_QueryOneTestCase operates in quiet mode, and should produce no
 * console output. It forces all trace flags off.
 */

extern int br_QueryOneTestCase ( U_Int_32 *text, int textLen, 
    int paragraphDirection, int *embeddingLevel, char *levels, int levelsLen,
    char *order, int orderLen );

#ifdef  __cplusplus
}
#endif

#endif /* __BIDIREF_LOADED */

