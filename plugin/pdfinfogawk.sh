pdfinfo $1 | gawk -f /opt/digiverso/goobi/config/namedKeys.awk | xmllint --format -
