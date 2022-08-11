BEGIN { 
   FS="|";
   printf("<?xml version=\"1.0\" ?>\n<file>\n");
}

NF==1 {
   sub(/:/,"^",$1); 
   split($1, a, "^"); for (i in a) {
      if (i == 2) {
    	gsub(/^[ \t]+/,"",a[2]);
        printf("<format>%s</format>\n", a[2]);
      } 
   }
}
END {
   printf("</file>\n");
}
