package org.goobi.files;

public class FileUtils {


    private FileUtils () {}

    public static String removeFileExtension(String fileName) {
        if (fileName.lastIndexOf(".") > 0) {
            return fileName.substring(0, fileName.lastIndexOf('.'));
        } else {
            return fileName;
        }

    }


}
