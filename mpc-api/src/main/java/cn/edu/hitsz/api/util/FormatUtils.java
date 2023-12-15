package cn.edu.hitsz.api.util;

public class FormatUtils {

    public static String format(String str) {
        return str.replace(" ", "").replace("\n", "").replace("\r", "");
    }
}
