package com.leo.algorithm;

import java.util.Arrays;

public class Hex {

    public static void main(String[] args) {
        System.out.println(Arrays.toString(hex2Bytes("e9")));  //-23
    }

    public static byte[] hex2Bytes(String hexString) {
        if (hexString == null || hexString.equals("")) {
            return null;
        }

        int length = hexString.length() / 2;
        char[] hexChars = hexString.toCharArray();
        byte[] bytes = new byte[length];
        String hexDigits = "0123456789abcdef";
        for (int i = 0; i < length; i++) {
            int pos = i * 2; // 两个字符对应一个byte
            int h = hexDigits.indexOf(hexChars[pos]) << 4; // 注1
            int l = hexDigits.indexOf(hexChars[pos + 1]); // 注2
            if(h == -1 || l == -1) { // 非16进制字符
                return null;
            }
            bytes[i] = (byte) (h | l);
        }
        return bytes;
    }

    public static String bytes2Hex(byte[] src) {
        if (src == null || src.length <= 0) {
            return null;
        }

        char[] res = new char[src.length * 2]; // 每个byte对应两个字符
        final char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
        for (int i = 0, j = 0; i < src.length; i++) {
            res[j++] = hexDigits[src[i] >> 4 & 0x0f]; // 先存byte的高4位
            res[j++] = hexDigits[src[i] & 0x0f]; // 再存byte的低4位
        }

        return new String(res);
    }


//    public static String bytes2Hex(byte[] src){
//        if (src == null || src.length <= 0) {
//            return null;
//        }
//
//        StringBuilder stringBuilder = new StringBuilder("");
//        for (int i = 0; i < src.length; i++) {
//            // 之所以用byte和0xff相与，是因为int是32位，与0xff相与后就舍弃前面的24位，只保留后8位
//            String str = Integer.toHexString(src[i] & 0xff);
//            if (str.length() < 2) { // 不足两位要补0
//                stringBuilder.append(0);
//            }
//            stringBuilder.append(str);
//        }
//        return stringBuilder.toString();
//    }
}
