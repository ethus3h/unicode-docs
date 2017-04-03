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
/* Unicode to SJIS Conversion */
/* by Mark E. Davis */
/**********************************************************************/
/* Background: */
/* There are 6,355 Han characters in the Shift-JIS character set. */
/* These are roughly randomly distributed through the 20,902 UniHan */
/* characters (those from U+4E00 to U+0x9FA5. */

/* There is an algorithmic mapping that takes all of the SJIS */
/* characters onto a contiguous range of numbers from 0 to 6,354. */
/* Those latter numbers we will call Cjis (Contiguous JIS). */

/* Converting from Sjis to Unicode is easy: first map Sjis to Cjis, */
/* then map from Cjis to Unicode with a 12,710 byte table. */

/* Going backwards is trickier, since Unicode Han is sparsely */
/* populated with Sjis values, roughly 1 of every 3. */
/* The straightforward way to to map from 12,710 Unicode to Sjis */
/* with a 41,804 byte table. However, in tight memory situations, */
/* this may be too large. */

/* The following code presents a reasonably efficient method */
/* of mapping backwards with an table of 14,802 bytes. */

/* (Left for the reader: with some loss in efficiency */
/* the Cjis-Unicode table could be shaved by about 800 bytes,*/
/* and the Unicode-Cjis table by about 720 bytes.) */
/**********************************************************************/
#ifndef __KANJIMAPPING__
#define __KANJIMAPPING__

#ifndef __STDLIB__
	#include <stdlib.h>
#endif
#ifndef __STDIO__
	#include <stdio.h>
#endif
#ifndef __STRING__
	#include <string.h>
#endif
#ifndef __TYPES__
	#include <sys/types.h>
#endif

typedef enum {
	false = 0,
	true = 1
} Boolean;

/**********************************************************************/
/* The following belong in a machine-dependent header file */
/**********************************************************************/

typedef signed short int16;
typedef unsigned short uint16;
typedef uint16 char16;
typedef unsigned char char8;	

/**********************************************************************/
/* interesting constants for Sjis & Unicode */
/**********************************************************************/

#define cannotMap (char16)0x1FFF	/* returned if can't convert */

#define cjisCount (uint16)6356		/* actual SJIS Level 1&2 Han */
#define minHan (char16)0x4E00
#define maxHan (char16)0x9FA5
#define hanCount (char16)(maxHan - minHan + 1)

/**********************************************************************/

char16 SjisToCjis(
		register char16 sjis);

char16 CjisToSjis (
		register char16 cjis);
		
char16 CjisToUnicode(
		register char16 * CjisToUnicodeTable,
		register char16 cjis);

char16 UnicodeToCjis (
		register char16 * PackedUnicodeToCjisIndex, 
		register char16 * PackedUnicodeToCjisTable,
		register char16 unicode);

void PackUnicodeToCjisTable(
		char16 * UnicodeToCjisTable,
		char16 * PackedUnicodeToCjisIndex, 
		char16 * PackedUnicodeToCjisTable,
		char16 * indexCountPtr,
		char16 * packedCountPtr);
		
/* Note: these different tables could be packed into one, */
/* but for clarity they are kept separate in this code */

/**********************************************************************/
/* The following constants are used both internally and for testing. */
/* The indexPower determines the size vs speed tradeoff. */
/* Since about 1/3 of Unicode Han are JIS characters, */
/* distributed relatively randomly, we loop on average about: */
/*		1/3 * 1/2 * (1<<indexPower) times */
/* and the index table size = about: */
/*		(20902 / (1<<indexPower) + 1) * 2 bytes */
/* The number of indices also can change the packed table size */
/* since it may force the creation of spurious item. */
/**********************************************************************/
/* e.g. indexPower		av. loops	approx. total index bytes */
/*			8			43			  80 * 2 */
/*			7			22			 160 * 2 */
/*			6			11			 330 * 2 */
/*			5			 6			 650 * 2 */
/*			4			 3			1310 * 2 */
/**********************************************************************/

#define indexPower 6			/* go for medium speed/size tradeoff */
#define deltaPower 3			/* 16 - #bits in cjisCount!!! */

#define indexCount (((hanCount - 1) >> indexPower) + 1)
#define packCount (cjisCount+1000)

/* note: in production code we would figure out packCount exactly, */
/* but for now we just add on some slop */

/**********************************************************************/
/* End of header file */
/**********************************************************************/
#endif
