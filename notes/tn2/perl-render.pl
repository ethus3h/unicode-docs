#!C:/MySoft/Perl/bin/perl.exe -w

sub min {
    my $myx; my $myy;
    ($myx, $myy) = @_;
    if ($myx < $myy) { return ($myx); }
    else { return ($myy); }
}

sub max {
    my $myx; my $myy;
    ($myx, $myy) = @_;
    if ($myx > $myy) { return ($myx); }
    else { return ($myy); }
}

printf("%% WhiteBox(float xpos, ypos, wid, hi)\n/WhiteBox {\n");
printf("\t0.5 setgray\n");
printf("\t/hi exch def /wid exch def /ypos exch def /xpos exch def\n");
printf("\tnewpath\n");
printf("\txpos ypos moveto\n");
printf("\txpos wid add ypos lineto\n");
printf("\txpos wid add ypos hi add lineto\n");
printf("\txpos ypos hi add lineto\n");
printf("\tclosepath stroke\n");
printf("\t0 setgray\n");
printf("\t} def\n");

printf("0.1 0.1 scale\n1000 1000 translate\n");
printf("/Times-Roman findfont 1000 scalefont setfont\n");
printf("0 setgray\n");

#
# Set some constants -- the "gap" for this font, based on CapHeight
#

$CapHeight = 662;
$TheGap = $CapHeight / 8.0;

# Bounding rect of the Base glyph...
$Q_x = 34; $Q_y = -178; $Q_w = 667; $Q_h = 854;
$TheBaseLetter = "Q";

# Bounding rect of Dieresis glyph...
$Dieresis_x = 18; $Dieresis_y = 523; $Dieresis_w = 297; $Dieresis_h = 100;

# Bounding rect of Ring glyph...
$Ring_x = 67; $Ring_y = 512; $Ring_w = 199; $Ring_h = 199;

# Bounding rect of Cedilla glyph... (The cedilla has a "descent" instead of
# an ascent. In a case where height is negative, it must be flipped positive
# to assure that a rect's height is always positive.)
$Cedilla_x = 52; $Cedilla_y = -215; $Cedilla_w = 209; $Cedilla_h = 215;

# Bounding rect of dot accent...
$Dot_x = 118; $Dot_y = 523; $Dot_w = 98; $Dot_h = 100;

#
# Top of loop, iteration 1... Set the bounding rect for the base glyph "Q".
#
$BaseRect_x = $Q_x;
$BaseRect_y = $Q_y;
$BaseRect_w = $Q_w;
$BaseRect_h = $Q_h;

#
# Set the bounding rect for the mark "dieresis"...
#
$MarkRect_x = $Dieresis_x;
$MarkRect_y = $Dieresis_y;
$MarkRect_w = $Dieresis_w;
$MarkRect_h = $Dieresis_h;

#
# show the base glyph and draw a box around it...
#
printf("0 0 moveto /%s glyphshow\n\n", $TheBaseLetter);
printf("%1.0f %1.0f %1.0f %1.0f WhiteBox\n\n", $BaseRect_x, $BaseRect_y, $BaseRect_w, $BaseRect_h);

#
# Calculate where to put the "above detached" NS mark relative to the base rectangle.
#
$ns_Xpoint = ($BaseRect_x - $MarkRect_x) + (($BaseRect_w - $MarkRect_w) / 2.0);
$ns_Ypoint = (($BaseRect_y + $BaseRect_h) + $TheGap) - $MarkRect_y;

#
# Show the dieresis and draw a box around it...
#
printf("0 0 moveto\n");
printf("%1.0f %1.0f rmoveto /dieresis glyphshow\n", $ns_Xpoint, $ns_Ypoint);
printf("%1.0f %1.0f %1.0f %1.0f WhiteBox\n\n", $ns_Xpoint + $MarkRect_x, $ns_Ypoint + $MarkRect_y, $MarkRect_w, $MarkRect_h);

#
# Calculate the new, enclosing bounding rect for the combination,
# and draw a box around the whole thing.
# ns_Xextent, ns_Yextent is the upper right corner of the mark glyph...
# 
$ns_Xextent = $ns_Xpoint + $MarkRect_x + $MarkRect_w;
$ns_Yextent = $ns_Ypoint + $MarkRect_y + $MarkRect_h;
$NewBaseBRect_x = min($BaseRect_x, $ns_Xpoint + $MarkRect_x);
$NewBaseBRect_y = min($BaseRect_y, $ns_Ypoint + $MarkRect_y);
$NewBaseBRect_w = max($BaseRect_x + $BaseRect_w, $ns_Xextent) - $BaseRect_x;
$NewBaseBRect_h = max($BaseRect_y + $BaseRect_h, $ns_Yextent) - $BaseRect_y;

printf("%1.0f %1.0f %1.0f %1.0f WhiteBox\t%% finished.\n\n", $NewBaseBRect_x, $NewBaseBRect_y, $NewBaseBRect_w, $NewBaseBRect_h);

#
# Reset the "bounding rect" to the total enclosing rect calculated above...
#
$BaseRect_x = $NewBaseBRect_x;
$BaseRect_y = $NewBaseBRect_y;
$BaseRect_w = $NewBaseBRect_w;
$BaseRect_h = $NewBaseBRect_h;

#
# Now we're back at the top of the loop, looking at "ring above" glyph...
#
$MarkRect_x = $Ring_x;
$MarkRect_y = $Ring_y;
$MarkRect_w = $Ring_w;
$MarkRect_h = $Ring_h;

#
# Calculate the offsets; where to put the "above detached" NS mark relative to the base.
#
$ns_Xpoint = ($BaseRect_x - $MarkRect_x) + (($BaseRect_w - $MarkRect_w) / 2.0);
$ns_Ypoint = (($BaseRect_y + $BaseRect_h) + $TheGap) - $MarkRect_y;

#
# Show the ring and draw a box around it...
#
printf("0 0 moveto\n");
printf("%1.0f %1.0f rmoveto /ring glyphshow\n", $ns_Xpoint, $ns_Ypoint);
printf("%1.0f %1.0f %1.0f %1.0f WhiteBox\n\n", $ns_Xpoint + $MarkRect_x, $ns_Ypoint + $MarkRect_y, $MarkRect_w, $MarkRect_h);

#
# Calculate the new, enclosing bounding rect for the combination,
# and draw a box around the whole thing.
# ns_Xextent, ns_Yextent is the upper right corner of the mark glyph...
# 
$ns_Xextent = $ns_Xpoint + $MarkRect_x + $MarkRect_w;
$ns_Yextent = $ns_Ypoint + $MarkRect_y + $MarkRect_h;
$NewBaseBRect_x = min($BaseRect_x, $ns_Xpoint + $MarkRect_x);
$NewBaseBRect_y = min($BaseRect_y, $ns_Ypoint + $MarkRect_y);
$NewBaseBRect_w = max($BaseRect_x + $BaseRect_w, $ns_Xextent) - $BaseRect_x;
$NewBaseBRect_h = max($BaseRect_y + $BaseRect_h, $ns_Yextent) - $BaseRect_y;

printf("%1.0f %1.0f %1.0f %1.0f WhiteBox\t%% finished.\n\n", $NewBaseBRect_x, $NewBaseBRect_y, $NewBaseBRect_w, $NewBaseBRect_h);

#
# Reset the "bounding rect" to the total enclosing rect calculated above...
#
$BaseRect_x = $NewBaseBRect_x;
$BaseRect_y = $NewBaseBRect_y;
$BaseRect_w = $NewBaseBRect_w;
$BaseRect_h = $NewBaseBRect_h;

#
# Now we're back at the top of the loop, looking at the "cedilla" mark glyph...
#
$MarkRect_x = $Cedilla_x;
$MarkRect_y = $Cedilla_y;
$MarkRect_w = $Cedilla_w;
$MarkRect_h = $Cedilla_h;

#
# Calculate the offsets; where to put the "below attached" NS mark relative to the base.
# (For attached mark glyphs, we don't add the GAP to the Y value, or in the case of
# a descender, we don't SUBTRACT the gap value.)
#
$ns_Xpoint = ($BaseRect_x - $MarkRect_x) + (($BaseRect_w - $MarkRect_w) / 2.0);
$ns_Ypoint = $BaseRect_y - $MarkRect_y - $MarkRect_h;

#
# Show the cedilla and draw a box around it...
#
printf("0 0 moveto\n");
printf("%1.0f %1.0f rmoveto /cedilla glyphshow\n", $ns_Xpoint, $ns_Ypoint);
printf("%1.0f %1.0f %1.0f %1.0f WhiteBox\n\n", $ns_Xpoint + $MarkRect_x, $ns_Ypoint + $MarkRect_y, $MarkRect_w, $MarkRect_h);

#
# Calculate the new, enclosing bounding rect for the combination,
# and draw a box around the whole thing.
# ns_Xextent, ns_Yextent is the upper right corner of the mark glyph...
# 
$ns_Xextent = $ns_Xpoint + $MarkRect_x + $MarkRect_w;
$ns_Yextent = $ns_Ypoint + $MarkRect_y + $MarkRect_h;
$NewBaseBRect_x = min($BaseRect_x, $ns_Xpoint + $MarkRect_x);
$NewBaseBRect_y = min($BaseRect_y, $ns_Ypoint + $MarkRect_y);
$NewBaseBRect_w = max($BaseRect_x + $BaseRect_w, $ns_Xextent) - $BaseRect_x;
$NewBaseBRect_h = max($BaseRect_y + $BaseRect_h, $ns_Yextent) - $BaseRect_y;

printf("%1.0f %1.0f %1.0f %1.0f WhiteBox\t%% finished.\n\n", $NewBaseBRect_x, $NewBaseBRect_y, $NewBaseBRect_w, $NewBaseBRect_h);













#
# Reset the "bounding rect" to the total enclosing rect calculated above...
#
$BaseRect_x = $NewBaseBRect_x;
$BaseRect_y = $NewBaseBRect_y;
$BaseRect_w = $NewBaseBRect_w;
$BaseRect_h = $NewBaseBRect_h;

#
# Now we're back at the top of the loop, looking at the "dot accent" glyph...
#
$MarkRect_x = $Dot_x;
$MarkRect_y = $Dot_y;
$MarkRect_w = $Dot_w;
$MarkRect_h = $Dot_h;

#
# Calculate the offsets; where to put the "below detached" NS mark relative to the base.
# (For detached mark glyphs, we must subtract the GAP from the Y value.)
#
$ns_Xpoint = ($BaseRect_x - $MarkRect_x) + (($BaseRect_w - $MarkRect_w) / 2.0);
$ns_Ypoint = $BaseRect_y - $MarkRect_y - $MarkRect_h - $TheGap;

#
# Show the "dot below" accent and draw a box around it...
#
printf("0 0 moveto\n");
printf("%1.0f %1.0f rmoveto /dotaccent glyphshow\n", $ns_Xpoint, $ns_Ypoint);
printf("%1.0f %1.0f %1.0f %1.0f WhiteBox\n\n", $ns_Xpoint + $MarkRect_x, $ns_Ypoint + $MarkRect_y, $MarkRect_w, $MarkRect_h);





#
# Calculate the new, enclosing bounding rect for the combination,
# and draw a box around the whole thing.
# ns_Xextent, ns_Yextent is the upper right corner of the mark glyph...
# 
$ns_Xextent = $ns_Xpoint + $MarkRect_x + $MarkRect_w;
$ns_Yextent = $ns_Ypoint + $MarkRect_y + $MarkRect_h;
$NewBaseBRect_x = min($BaseRect_x, $ns_Xpoint + $MarkRect_x);
$NewBaseBRect_y = min($BaseRect_y, $ns_Ypoint + $MarkRect_y);
$NewBaseBRect_w = max($BaseRect_x + $BaseRect_w, $ns_Xextent) - $BaseRect_x;
$NewBaseBRect_h = max($BaseRect_y + $BaseRect_h, $ns_Yextent) - $BaseRect_y;

printf("%1.0f %1.0f %1.0f %1.0f WhiteBox\t%% finished.\n\n", $NewBaseBRect_x, $NewBaseBRect_y, $NewBaseBRect_w, $NewBaseBRect_h);




# print one more rect to accentuate the lower gap.
printf("%1.0f %1.0f %1.0f %1.0f WhiteBox\t%% finished.\n\n", $NewBaseBRect_x, $NewBaseBRect_y, $NewBaseBRect_w, $MarkRect_h);
printf("showpage\n");



