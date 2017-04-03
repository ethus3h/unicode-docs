#!/bin/bash
mv README.md .README.md
rm -r *
mv .README.md README.md
wget --warc-file=unicode -e robots="off" -p http://www.unicode.org/reports/ http://www.unicode.org/notes/ http://unicode.org/history/
wget --warc-file=unicode_public -e robots="off" -R CodeCharts.pdf,RSIndex.pdf,ucd.all.flat.zip,ucd.all.grouped.zip,ucd.unihan.grouped.zip,ucd.unihan.flat.zip,ucd.nounihan.flat.zip,ucd.nounihan.grouped.zip,tools.zip,core.zip,keyboards.zip,cldr-tools-*.zip,cldr-common-*.zip,cldr-keyboards-*.zip,UCD.zip,Unihan.zip,json-full.zip,json.zip,U20000.pdf,U4E00.pdf,CodeCharts-MulticolHan.pdf,CodeCharts-noHan.pdf -p http://ftp.unicode.org/Public/
crystallize *.warc.gz
