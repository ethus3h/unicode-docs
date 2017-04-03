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
/* Code file for KanjiMapping */
/* by Mark E. Davis */
/**********************************************************************/
#ifndef __KANJIMAPPING__
	#include "KANJIMAP.H"
#endif
/**********************************************************************/
/* General utilities */
/**********************************************************************/
#define testing 1

#define MaskFromPower(x) ((1 << (x)) - 1)

/**********************************************************************/
/* Interesting character constants for Sjis (shift JIS) conversion */
/**********************************************************************/
/* note: each define should have been const char8, but this version */
/* of C is a bit weak. */

#define sjisFirstMin1 (char8)0x81
#define sjisFirstMax1 (char8)0x9F
#define sjisFirstMin2 (char8)0xE0
#define sjisFirstMax2 (char8)0xFC
#define sjisFirstGap (char8)(sjisFirstMin2 - sjisFirstMax1 - 1)
#define sjisFirstCount (char8)(sjisFirstMax2 - sjisFirstMin2 + 1\
									+ sjisFirstMax1 - sjisFirstMin1 + 1)

#define sjisSecondMin1 (char8)0x40
#define sjisSecondMax1 (char8)0x7E
#define sjisSecondMin2 (char8)0x80
#define sjisSecondMax2 (char8)0xFC
#define sjisSecondGap (char8)(sjisSecondMin2 - sjisSecondMax1 - 1)
#define sjisSecondCount (char8)(sjisSecondMax2 - sjisSecondMin2 + 1\
								+ sjisSecondMax1 - sjisSecondMin1 + 1)

#define sjisHanSingleton 0x8157
#define sjisHan2Start 0x0582
#define sjisHan3Start 0x0B95
#define sjisHan3Gap 43

/**********************************************************************/
/* Utility routines for converting between Cjis and Sjis codes. */
/* The first Sjis Han character is 8157. After that there is a gap */
/* until 889F. There is another gap of 43 later on, otherwise */
/* numbers are contiguous until EAA4 */

/* We first map the theoretical Sjis characters, then compact this */
/* range, so that 8157 -> 0, 889F -> 1, 88A0 -> 2, etc. */
/**********************************************************************/

char16 SjisToCjis(
		register char16 sjis) {
	register char8 first;
	register char8 second;
	register char16 cjis;
	
	if (sjis == sjisHanSingleton) return 0;
	first = sjis >> 8;
	if (first > sjisFirstMax1) first -= sjisFirstGap;
	first -= sjisFirstMin1;
		
	second = sjis & 0xFF;
	if (second > sjisSecondMax1) second -= sjisSecondGap;
	second -= sjisSecondMin1;
		
	cjis = first * sjisSecondCount + second - (sjisHan2Start - 1);
	if (cjis > sjisHan3Start) cjis -= sjisHan3Gap;
	return cjis;
};

/**********************************************************************/

char16 CjisToSjis (
		register char16 cjis) {
	register char8 first;
	register char8 second;
	
	if (cjis == 0) return sjisHanSingleton;
	if (cjis > sjisHan3Start) cjis += sjisHan3Gap;
	cjis += (sjisHan2Start - 1);
		
	/* Note: some compilers can do / and % in one step */
	/* otherwise, * is usually faster than % is */
	first = cjis / sjisSecondCount;	
	second = cjis - (first * sjisSecondCount);	
	second += sjisSecondMin1;
	if (second > sjisSecondMax1) second += sjisSecondGap;
		
	first += sjisFirstMin1;
	if (first > sjisFirstMax1) first += sjisFirstGap;
	return ((first << 8) | second);
};
/**********************************************************************/
/* now for converting Cjis to Unicode */
/**********************************************************************/

char16 CjisToUnicode(
		register char16 * CjisToUnicodeTable,
		register char16 cjis) {
	/* use a simple table lookup */
	return CjisToUnicodeTable[cjis];
};

/**********************************************************************/
/* The following is the more complicated routines for mapping from */
/* Unicode to Cjis in a small amount of space. */

/* The gaps between the mappable Unicodes have the following */
/* statistics: */
/* Gap   =    1,    2,    3,    4,    5,    6,    7,    8,    9,   10 */
/* Count = 2325, 1362,  849,  513,  397,  238,  178,  130,   83,   53 */
/* Sum   = 2325, 3687, 4536, 5049, 5446, 5684, 5862, 5992, 6075, 6128 */

/* Gap   =   11,   12,   13,   14,   15,   16,   17,   18,   19,   20 */
/* Count =   47,   28,   37,   26,   20,    8,    6,   12,   11,    4 */
/* Sum   = 6175, 6203, 6240, 6266, 6286, 6294, 6300, 6312, 6323, 6327 */

/* Gap   =   21,   22,   23,   24,   25,   27,   29,   33,   34,   52 */
/* Count =    4,    2,    1,    4,    1,    2,    1,    1,    1,    1 */
/* Sum   = 6331, 6333, 6334, 6338, 6339, 6341, 6342, 6343, 6344, 6345 */

/* Gap   =   55,   61,   63,   78,   87,  109,  154,  157,  246 */ 
/* Count =    2,    1,    1,    1,    1,    1,    1,    1,    1 */ 
/* Sum   = 6347, 6348, 6349, 6350, 6351, 6352, 6353, 6354, 6355 */ 

/**********************************************************************/
/* first some constants */

#define indexMask MaskFromPower(indexPower)
#define deltaMask MaskFromPower(deltaPower)
#define deltaCount deltaMask /* ((1 << (deltaPower)) - 1) */

/**********************************************************************/
/* This routine packs an unpacked mapping table into a packed mapping */
/* table. */
/* The algorithm uses the 3 unused bits in a Cjis to indicate */
/* compressed following zeros */
/* Since this routine is only used offline to make the array, it does */
/* not need to be especially optimized. */
/* Note: if we used a byte table, we could eliminate about */
/* another 700 bytes, at the expense of a little decrease in speed. */
/**********************************************************************/

void PackUnicodeToCjisTable(
		char16 * UnicodeToCjisTable,
		char16 * PackedUnicodeToCjisIndex, 
		char16 * PackedUnicodeToCjisTable,
		char16 * indexCountPtr,
		char16 * packedCountPtr) {
	uint16 i;
	uint16 packCounter = 0;
	uint16 offsetCounter = 1;
	uint16 indexCounter = 0;

	PackedUnicodeToCjisIndex[indexCounter++] = packCounter;
	PackedUnicodeToCjisTable[packCounter++]
		= UnicodeToCjisTable[minHan] << deltaPower;
	for (i = minHan + 1; i <= maxHan; ++i) {
		if (UnicodeToCjisTable[i] != cannotMap	/* if cjis to pack */
				|| (i & indexMask) == 0			/* on block boundary */
				|| i == maxHan) {				/* the last one */
				
			/* save the offset counter in the previous element */
			if (packCounter >= packCount) fprintf(stderr, "Pack counter too large!!");
			if (offsetCounter <= deltaCount) {		/* use low bits */
				PackedUnicodeToCjisTable[packCounter-1] |= offsetCounter;
			} else {					/* extender word afterwards */
				PackedUnicodeToCjisTable[packCounter++] = offsetCounter;
			};
			offsetCounter = 1;

			/* set the index array on block boundaries */
			if ((i & indexMask) == 0) {
				if (indexCounter >= indexCount) 
					fprintf(stderr, "Index counter too large!!");
				PackedUnicodeToCjisIndex[indexCounter++] = packCounter;
			};
			
			/* set the Cjis value in table, leaving room in low bits */
			PackedUnicodeToCjisTable[packCounter++]
				= UnicodeToCjisTable[i] << deltaPower;
		} else {
			/* just got another zero, so register it */
			++offsetCounter;
		};
	};
	*indexCountPtr = indexCounter;
	*packedCountPtr = packCounter;
};
/**********************************************************************/
/* This routine maps from a unicode to a Cjis using a packed table. */
/* The algorithm uses the 3 unused bits in a Cjis to indicate */
/* following zeros that would be in a flat Unicode-to-Cjis table */
/* Note: the extender word does not really reduce the size, but does */
/* reduce the worst-case lookup time */
/**********************************************************************/

char16 UnicodeToCjis (
		register char16 * PackedUnicodeToCjisIndex, 
		register char16 * PackedUnicodeToCjisTable,
		register char16 unicode) {
	register char16 result = cannotMap;
	
	/* the test for minHan <= unicode <= maxHan) could be 1 test, */
	/* on machines where unsigneds = large signs, e.g 2's complement */
	if (unicode <= maxHan) {
		if (unicode >= minHan) {
			register int16 offset;
			register int16 delta;
			register const char16 * startPtr;
			register const char16 deltaMaskReg = deltaMask;
	
			unicode -= minHan;
			
			/* get start and end values from table */
			startPtr = & PackedUnicodeToCjisTable 
					[PackedUnicodeToCjisIndex [unicode >> indexPower]];
			offset = unicode & indexMask;
			
			/* now loop through until we find the right index */
			/* on 68xxx machines usually 5 instructions per loop */
			while (offset > 0) {
				delta = *startPtr++ & deltaMaskReg; /* use low bits */
				/* otherwise special case: use extender word */
				if (delta == 0) delta = *startPtr++; 
				offset -= delta;
			};
		
			/* by definition, we are guaranteed to terminate */
			if (offset == 0) result = (*startPtr >> deltaPower);
		};
	};
	return result;
};

/**********************************************************************/
/* End of code file */
/**********************************************************************/

