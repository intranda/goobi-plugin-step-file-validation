pdfinfo $1 | gawk -f /opt/digiverso/tools/namedKeys.awk | xmllint --format -
