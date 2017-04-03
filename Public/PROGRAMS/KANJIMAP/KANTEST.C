/*
Copyright (C) 1994-1995 Taligent, Inc. All rights reserved.

This code is copyrighted. Under the copyright laws, this code may not
be copied, in whole or part, without prior written consent of Taligent. 

Taligent grants the right to use or reprint this code as long as this
ENTIRE copyright notice is reproduced in the code or reproduction.
The code is provided AS-IS, AND TALIGENT DISCLAIMS ALL WARRANTIES,
EITHER EXPRESS OR IMPLIED, INCLUDING, BUT NOT LIMITED TO IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.  IN
NO EVENT WILL TALIGENT BE LIABLE FOR ANY DAMAGES WHATSOEVER (INCLUDING,
WITHOUT LIMITATION, DAMAGES FOR LOSS OF BUSINESS PROFITS, BUSINESS
INTERRUPTION, LOSS OF BUSINESS INFORMATION, OR OTHER PECUNIARY
LOSS) ARISING OUT OF THE USE OR INABILITY TO USE THIS CODE, EVEN
IF TALIGENT HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
BECAUSE SOME STATES DO NOT ALLOW THE EXCLUSION OR LIMITATION OF
LIABILITY FOR CONSEQUENTIAL OR INCIDENTAL DAMAGES, THE ABOVE
LIMITATION MAY NOT APPLY TO YOU.

RESTRICTED RIGHTS LEGEND: Use, duplication, or disclosure by the
government is subject to restrictions as set forth in subparagraph
(c)(l)(ii) of the Rights in Technical Data and Computer Software
clause at DFARS 252.227-7013 and FAR 52.227-19.

This code may be protected by one or more U.S. and International
Patents.

TRADEMARKS: Taligent and the Taligent Design Mark are registered
trademarks of Taligent, Inc.
*/
/**********************************************************************/
/* Test file for KanjiMapping */
/* by Mark E. Davis */
/**********************************************************************/
#ifndef __KANJIMAPPING__
	#include "KANJIMAP.H"
#endif
/**********************************************************************/
/* Test Routines */
/**********************************************************************/

#define realData 1

char16 * TestUnicodeToCjisTable;
char16 * TestCjisToUnicodeTable;
char16 * TestPackedUnicodeToCjisIndex;
char16 * TestPackedUnicodeToCjisTable;
char16 testIndexCount, testPackedCount;

void AllocateAndFillTestTables() {
	uint16 i;
	char16 cjis, unicode;
	
	TestUnicodeToCjisTable = malloc(65536 * sizeof(char16));
	if (TestUnicodeToCjisTable == NULL) 
		fprintf(stderr, "Can't allocate TestUnicodeToCjisTable!!");

	TestCjisToUnicodeTable = malloc(cjisCount * sizeof(char16));
	if (TestCjisToUnicodeTable == NULL) 
		fprintf(stderr, "Can't allocate TestCjisToUnicodeTable!!");

	TestPackedUnicodeToCjisIndex = malloc(indexCount * sizeof(char16));
	if (TestPackedUnicodeToCjisIndex == NULL) 
		fprintf(stderr, "Can't allocate TestPackedUnicodeToCjisIndex!!");

	TestPackedUnicodeToCjisTable = malloc(packCount * sizeof(char16));
	if (TestPackedUnicodeToCjisTable == NULL) 
		fprintf(stderr, "Can't allocate TestPackedUnicodeToCjisTable!!");
	
	/* zero out array */
	fprintf (stderr, "\nZeroing tables");
	for (unicode = 0; unicode < 65535; ++unicode) {
		TestUnicodeToCjisTable[unicode] = cannotMap;
	};
	TestUnicodeToCjisTable[65535] = 0;
	for (unicode = 0; unicode < cjisCount; ++unicode) {
		TestCjisToUnicodeTable[unicode] = cannotMap;
	};
	
	if (realData) {/* read in values from file */
		FILE * inputFile;
		char16 sjis, maxCjis = 0;
		char sjisBuffer[200];

		fprintf (stderr, "\nReading conversion data from file.");
		inputFile = fopen("CJKXRef.jis","r");
		if (inputFile == NULL)
			fprintf(stderr, "Can't open CJKXRef.jis file!!");
		while (true) {
			fgets(sjisBuffer, 200, inputFile);
			if (feof(inputFile)) break;
			if (strlen(sjisBuffer) != 0) {	/* skip empty lines */
				int sjisInt, unicodeInt;
				#if 0
					fprintf (stderr, "line = %s", sjisBuffer);
				#endif
				sscanf(sjisBuffer, "0x%04X U+%04X\n",
					&sjisInt, &unicodeInt);
				sjis = sjisInt;
				unicode = unicodeInt;
				cjis = SjisToCjis(sjis);
				if (cjis > maxCjis) maxCjis = cjis;
				if (cjis >= cjisCount) {
					fprintf (stderr, "line = %s", sjisBuffer);
					fprintf (stderr, "sjis=0x%04X, unicode=0x%04X, cjis=0x%04X\n",
						sjis, unicode, cjis, sjisBuffer);
					fprintf(stderr, "cjis too big!!");
				} else {
					TestCjisToUnicodeTable[cjis] = unicode;
				};
				TestUnicodeToCjisTable[unicode] = cjis;
				#if 0
					fprintf (stderr, "sjis=0x%04X, unicode=0x%04X, cjis=0x%04X\n",
						sjis, unicode, cjis, sjisBuffer);
				#endif
			};
		};
		fclose(inputFile);
		#if testing
			fprintf (stderr, "\n maxCjis=0x%04X", maxCjis);
		#endif
	} else {
		/* insert fake cjis codes for testing */
		fprintf (stderr, "\nInserting fake cjis codes for testing");
		cjis = 0;
		for (unicode = minHan; unicode <= maxHan && cjis < cjisCount; ++unicode, ++cjis) {
			if (rand() < RAND_MAX / 3) { /* so roughly 1/3 of the time */ 
				TestCjisToUnicodeTable[cjis] = unicode;
				TestUnicodeToCjisTable[unicode] = cjis;
			};
			#if 0
			if ((unicode & 0xF) == 0) fprintf (stderr, "\n%04X:	", unicode);
			fprintf (stderr, "%04X,",TestUnicodeToCjisTable[unicode]);
			#endif
		};
	fprintf (stderr, "\n unicode=0x%04X, cjis=0x%04X", unicode, cjis);
	};
	fprintf (stderr, "\n Pack the data");
	PackUnicodeToCjisTable(
		TestUnicodeToCjisTable,
		TestPackedUnicodeToCjisIndex,
		TestPackedUnicodeToCjisTable,
		&testIndexCount,
		&testPackedCount);
	fprintf (stderr, "\nindex count = %i, pack count = %i, total bytes = %i",
		testIndexCount, testPackedCount, (testIndexCount + testPackedCount)*2);
	
	/* print out first bit of tables for hand comparison */
	#define savePacked 1
	#if savePacked
	{
		FILE * outputFile;
		
		outputFile = fopen("sjisPacked","w");
		if (outputFile == NULL)
			fprintf(stderr, "Can't open sjisPacked file!!");

		fprintf (stderr, "\n Saving unpacked Data");
		for (unicode = minHan; unicode <= maxHan; ++unicode) {
			if ((unicode & 0xF) == 0) fprintf (outputFile, "\n%04X:	", unicode);
			fprintf (outputFile, "%04X,",TestUnicodeToCjisTable[unicode]);
		};

		fprintf (stderr, "\n Saving indices");
		for (i = 0; i < testIndexCount; ++i) {
			if ((i & 0xF) == 0) fprintf (outputFile, "\n%04X:	", minHan + (i << indexPower));
			fprintf (outputFile, "%04X,",TestPackedUnicodeToCjisIndex[i]);
		};

		fprintf (stderr, "\n Saving packed table");
		for (i = 0; i < testPackedCount; ++i) {
			char16 code, zeros;
	
			if ((i & 0xF) == 0) fprintf (outputFile, "\n%04X:	", i);
			code = TestPackedUnicodeToCjisTable[i] >> 3;
			zeros = TestPackedUnicodeToCjisTable[i] & 7;
			fprintf (outputFile, "%04X|%1X,",code, zeros);
		};

		fprintf (outputFile, "\n");

		fclose(outputFile);
	};
	#endif
};

/**********************************************************************/
void MeasureGap() {
	FILE * inputFile;
	long lastUnicode = 0, delta, sum = 0;
	char sjisBuffer[200];
	#define statisticsCount 1000
	int16 statistics[statisticsCount];
	int i;

	/* try opening file */
	fprintf (stderr, "\nMeasuring gaps in UniHan.");
	inputFile = fopen("CJKXRef.jis","r");
	if (inputFile == NULL)
		fprintf(stderr, "Can't open CJKXRef.jis file!!");
	
	/* clear out statistics table */
	for (i = 0; i < statisticsCount; ++i) statistics[i] = 0;
	
	/* collect data */
	while (true) {
		fgets(sjisBuffer, 200, inputFile);
		if (feof(inputFile)) break;
		if (strlen(sjisBuffer) != 0) {	/* skip empty lines */
			int sjisInt, unicodeInt;
			sscanf(sjisBuffer, "0x%04X U+%04X\n", &sjisInt, &unicodeInt);
			if (lastUnicode != 0) { 
				delta = unicodeInt - lastUnicode;	/* always ordered */
				if (delta < 0 || delta >= statisticsCount )
					fprintf(stderr, "Delta out of bounds!!");
				++statistics[delta];
			};
			lastUnicode = unicodeInt;
		};
	};
	
	/* print data */
	fprintf(stderr, "\nGap   = ");
	for (i = 0; i < statisticsCount; ++i) {
		if (statistics[i] != 0) {
			fprintf(stderr, "%4i, ", i);
		};
	};
	fprintf(stderr, "\nCount = ");
	for (i = 0; i < statisticsCount; ++i) {
		if (statistics[i] != 0) {
			fprintf(stderr, "%4i, ", statistics[i]);
		};
	};
	fprintf(stderr, "\nSum   = ");
	for (i = 0; i < statisticsCount; ++i) {
		if (statistics[i] != 0) {
			sum += statistics[i];
			fprintf(stderr, "%4i, ", sum);
		};
	};
	fprintf(stderr, "\n");
	fclose(inputFile);
};
/**********************************************************************/
void FixJis() {
	FILE * inputFile, * outputFile;
	char16 sjis, sjis2, cjis, unicode;
	long lastCjis = 0, lastUnicode = 0;
	char sjisBuffer[200];

	/* try opening files */
	fprintf (stderr, "\nTesting gaps in Sjis.");
	inputFile = fopen("sjisSorted","r");
	if (inputFile == NULL)
		fprintf(stderr, "Can't open sjisSorted file!!");
	outputFile = fopen("sjisFixed","w");
	if (outputFile == NULL)
		fprintf(stderr, "Can't open outputFile file!!");

	/* create data */
	while (true) {
		fgets(sjisBuffer, 200, inputFile);
		if (feof(inputFile)) break;
		if (strlen(sjisBuffer) != 0) {	/* skip empty lines */
			int sjisInt, unicodeInt;
			sscanf(sjisBuffer, "0x%04X U+%04X\n", &sjisInt, &unicodeInt);
			sjis = sjisInt;
			unicode = unicodeInt;
			cjis = SjisToCjis(sjis);
			if (cjis - lastCjis != 1) {
				fprintf(stderr, "Gap found!\n");
				fprintf(outputFile, "?????\n");
			};
			sjis2 = CjisToSjis(cjis);
			if (sjis != sjis2) {
				fprintf(stderr, "Failed round trip: s=%04X c=%04X s=%04X\n",
					sjis, cjis, sjis2);
				fprintf(outputFile, "&&&&&\n");
			};
			fprintf(outputFile, "s=%04X u=%04X c=%04X s=%04X c=%c%04X u=%c%04X\n", 
				sjis, unicode, cjis, sjis2,
				(cjis - lastCjis < 0) ? '-' : '+', abs(cjis - lastCjis),
				(unicode - lastUnicode < 0) ? '-' : '+', abs(unicode - lastUnicode));
			if ((cjis & 0xFF) == 0) fprintf(stderr,".");
			lastCjis = cjis;
			lastUnicode = unicode;
		};
	};
	fclose(inputFile);
	fclose(outputFile);
};
/**********************************************************************/

main () {
	register uint16 i;
	uint16 failures = 0;
	
	/* Debugger(); */
	fprintf (stderr, "\nAllocating Tables");
	AllocateAndFillTestTables();
	
	fprintf (stderr, "\nTesting equivalence");
	/* now test equivalence */
	for (i = minHan; i <= maxHan; ++i) {
		register char16 packed, unpacked;
		unpacked = TestUnicodeToCjisTable[i];
		packed = UnicodeToCjis(
			TestPackedUnicodeToCjisIndex,
			TestPackedUnicodeToCjisTable,
			i);
		if (packed != unpacked ) { 
			fprintf (stderr, "\nFailed at %04X: unpacked = %04X, packed = %04X",
				i, unpacked, packed);
			failures++;
		};
	};
	fprintf (stderr, "\nFailure count = %i\n", failures);
	return 0;
};