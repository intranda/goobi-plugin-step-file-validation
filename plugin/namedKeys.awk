BEGIN { 
   FS="|";
   printf("<?xml version=\"1.0\" ?>\n<pdfinfo>\n");
}

NF==1 {
   sub(/:/,"^",$1); 
   split($1, a, "^"); for (i in a) {
    if (i == 1) {
    	gsub(/[ \t]+/,"",a[1]);
        printf("<%s>", a[1]);
        }
    if (i == 2) {
    	gsub(/^[ \t]+/,"",a[2]);
        printf("%s", a[2]);
        printf("</%s>\n", a[1]);
        }
   } 
}

END {
   printf("</pdfinfo>\n");
}
