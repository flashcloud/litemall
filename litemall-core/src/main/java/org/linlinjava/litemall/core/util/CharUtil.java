package org.linlinjava.litemall.core.util;

import java.util.Random;

public class CharUtil {

    public static enum CharType {
        REGISTER(0),
        LOGIN(1),
        RESET_PASSWORD(2),
        RESET_PHONE(3),
        FORGET_PASSWORD(4);

        final int typeValue;

        CharType(int value) {
            this.typeValue = value;
        }
        public int getTypeValue() {
            return typeValue;
        }
    }

    public static String getRandomString(Integer num) {
        String base = "abcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < num; i++) {
            int number = random.nextInt(base.length());
            sb.append(base.charAt(number));
        }
        return sb.toString();
    }

    public static String getRandomNum(Integer num) {
        String base = "0123456789";
        Random random = new Random();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < num; i++) {
            int number = random.nextInt(base.length());
            sb.append(base.charAt(number));
        }
        return sb.toString();
    }

}
