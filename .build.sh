#!/bin/bash
mv README.md .README.md
rm -r *
mv .README.md README.md
wget --warc-file=unicode -e robots="off" -p http://www.unicode.org/reports/ http://www.unicode.org/notes/ http://unicode.org/history/
wget --warc-file=unicode_public -e robots="off" -R CodeCharts.pdf,RSIndex.pdf -p http://ftp.unicode.org/Public/
crystallize *.warc.gz