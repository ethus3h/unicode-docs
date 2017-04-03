#!/bin/bash
mv README.md .README.md
rm -r *
mv .README.md README.md
wget --warc-file=unicode -e robots="off" -p http://www.unicode.org/reports/ http://www.unicode.org/notes/ http://www.unicode.org/history/
wget --warc-file=unicode_public -e robots="off" -R CodeCharts.pdf,RSIndex.pdf,ucd.all.flat.zip,ucd.all.grouped.zip,ucd.unihan.grouped.zip,ucd.unihan.flat.zip,ucd.nounihan.flat.zip,ucd.nounihan.grouped.zip,tools.zip,core.zip,keyboards.zip,cldr-tools-*.zip,cldr-common-*.zip,cldr-keyboards-*.zip,UCD.zip,Unihan.zip,json-full.zip,json_full.zip,posix.zip,tests.zip,json.zip,CodeCharts-MulticolHan.pdf,CodeCharts-noHan.pdf -p http://ftp.unicode.org/Public/
mv ftp.unicode.org/* .
mv www.unicode.org/* .
find Public -path "*/charts/blocks*" -name "U*.pdf" -delete
crystallize *.warc.gz {ftp,www}.unicode.org
