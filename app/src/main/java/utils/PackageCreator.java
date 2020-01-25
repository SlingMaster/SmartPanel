/*
 * Copyright (c) 2018. by RFControls. All Rights Reserved.
 * www.http://rfcontrols.com/
 * Design and Programming by Alex Dovby
 */

package utils;

import com.jsc.smartpanel.Constants;

import java.io.ByteArrayOutputStream;

public class PackageCreator {

    // ========================================
    public static byte[] createHeader(int cmd, int index, short crc) {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();

        try {
            outStream.write(Constants.START);
            outStream.write(cmd);
            outStream.write(intToBytes(index * 1024));
            outStream.write(shortToBytes(crc));
        } catch (Exception e) {
            System.out.println("createPackage Error");
            return null;
        }
        // byte[] mass = outStream.toByteArray();
        return outStream.toByteArray();
    }

    // ========================================
    static byte[] createPackage(byte[] header, byte[] body) {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        byte[] encodehHeader = encodePackage(header);
        byte[] encodeBody = encodePackage(body);

//        FileUtils.writeBinFileSD("Download", "srcP1.bin", PackageCreator.copyPartArray(body, 0 * Constants.BYTE_SIZE,
//                body.length));
//
//        FileUtils.writeBinFileSD("Download", "encodeP1.bin", PackageCreator.copyPartArray(encode, 0 * Constants.BYTE_SIZE,
//                encode.length));
        try {
            outStream.write(Constants.DELIMITER);
            outStream.write(encodehHeader);
            outStream.write(encodeBody);
            outStream.write(Constants.DELIMITER);
        } catch (Exception e) {
            System.out.println("createPackage Error");
            return null;
        }
        // System.out.println("outStream" + outStream.toByteArray());
        // Debug.printByteArray("outStream", outStream.toByteArray());
        return outStream.toByteArray();
    }

    // ========================================
    static byte[] createPackage(byte[] header) {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        byte[] encodehHeader = encodePackage(header);

        try {
            outStream.write(Constants.DELIMITER);
            outStream.write(encodehHeader);
            outStream.write(Constants.DELIMITER);
        } catch (Exception e) {
            System.out.println("createPackage Error");
            return null;
        }
        // System.out.println("outStream" + outStream.toByteArray());
        // Debug.printByteArray("outStream", outStream.toByteArray());
        return outStream.toByteArray();
    }

    // ========================================
    public static byte[] encodePackage(byte[] _package) {
        ByteArrayOutputStream encode_package = new ByteArrayOutputStream();
        for (byte b : _package) {
            String hex = String.format("%02X", b); //Integer.toHexString(b & 0xFF).toUpperCase();
            //System.out.println("| HEX : |" + hex + "| Constants.VAL_C0:" + " |" + Constants.VAL_C0 + "|");
            //System.out.println("| DEC : " + b + " | Length : " + _package.length);
            if (hex.equals(Constants.VAL_C0)) {
                // DBDC ------------
                encode_package.write(0xDB);
                encode_package.write(0xDC);
            } else {
                if (hex.equals(Constants.VAL_DB)) {
                    // DBDD ------------
                    encode_package.write(0xDB);
                    encode_package.write(0xDD);
                } else {
                    encode_package.write(b);
                }
            }
        }
        // System.out.println("encodePackage Package length : |" + _package.length + " bytes | new size: " + encode_package.toByteArray().length + " bytes");
        return encode_package.toByteArray();
    }

    // ========================================
    public static byte[] decodePackage(byte[] _package) {
        ByteArrayOutputStream encode_package = new ByteArrayOutputStream();

        for (int i = 0; i < _package.length; i++) {
            //System.out.println(i + "| | DEC : " + _package[i]);
            String hex = String.format("%02X", _package[i]);

            //Integer.toHexString(b & 0xFF).toUpperCase();
            // System.out.println("| HEX : |" + hex);
            if (hex.equals(Constants.VAL_DB)) {
                String nextByte = String.format("%02X", _package[i + 1]);
                //System.out.println(i + " | nextByte : " + nextByte);
                if (nextByte.equals(Constants.VAL_DC)) {
                    encode_package.write(0xC0);
                    i++;
                }
                if (nextByte.equals(Constants.VAL_DD)) {
                    encode_package.write(0xDB);
                    i++;
                }
            } else {
                encode_package.write(_package[i]);
            }
        }

        // System.out.println("decodePackage Package length : |" + _package.length + " bytes | new size: " + encode_package.toByteArray().length + " bytes");
        return encode_package.toByteArray();
    }

    // ========================================
    public static String getHeaderStr(byte[] data) {
        if (data == null) {
            return "";
        }
        byte[] header = copyPartArray(data, 0, data.length);
        if (header == null) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        for (byte b : header) {
            result.append(String.format("%02X ", b));
        }
        // System.out.println("PackageCreator | sendData: " + result);
        // return result.toString();
        String str = result.toString();
//        return " " + str.substring(0, str.length() - 1) + " ";
        return str.substring(0, str.length() - 1);
    }

    // ========================================
    static String getBody(byte[] data) {
        if (data == null) {
            return "";
        }
        int arrLength = (data.length > 7) ? 7 : data.length;
        byte[] body = copyPartArray(data, 0, arrLength);
        if (body == null) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        result.append(" ");
        for (byte b : body) {
            result.append(String.format("%02X ", b));
            // result.append("|"); // delimiter
        }
        result.append("...");
        return result.toString();
    }

    // ========================================
    public static String getVersion(byte[] data) {
        final int start = data.length - 8;
        byte[] version = copyPartArray(data, start, 3);

        StringBuilder result = new StringBuilder();
        for (byte b : version) {
            result.append(String.format("%02X", b));
            result.append("."); // delimiter
        }
        String str = result.toString();
        return str.substring(0, str.length() - 1);

//        byte b = data[0];
//        System.out.println("TSPClient | sendingPackage | sendData:" + Integer.toHexString(b & 0xFF) + " | " + data.length);
//         Integer.toHexString(b & 0xFF);
    }

    // ========================================
//    public static byte getCmd(long header) {
//        byte[] cmd = copyPartArray(longToBytes(header), 1, 1);
////        StringBuffer result = new StringBuffer();
////        result.append(String.format("%02X ", cmd[0]));
//        return cmd[0];
//    }

    // ========================================
   public static byte[] copyPartArray(byte[] data, int start, int length) {
        if (data == null)
            return null;
        if (start > data.length)
            return null;
        if (length > data.length)
            length = data.length;

        byte[] result = new byte[length];
        System.arraycopy(data, start, result, 0, length);
        return result;
    }

    private static byte[] shortToBytes(final short s) {
        return new byte[]{(byte) (s & 0x00FF), (byte) ((s & 0xFF00) >> 8)
        };
    }

    // ========================================
    private static byte[] intToBytes(int i) {
        return new byte[]{(byte) (i >>> 24), (byte) ((i << 8) >>> 24),
                (byte) ((i << 16) >>> 24), (byte) ((i << 24) >>> 24)
        };
    }

    // ====================================================
    public static int getCommandID(final String data) {
        if (data != null) {
            return Integer.parseInt(data.substring(4, 6), 16);
        }
        return 0;
    }
    // ========================================
//    public static byte[] longToBytes(long l) {
//        byte[] result = new byte[8];
//        for (int i = 7; i >= 0; i--) {
//            result[i] = (byte) (l & 0xFF);
//            l >>= 8;
//        }
//        return result;
//    }

    // ========================================
//    public static long byteToLong(byte[] array, int offset) {
//        return ((long) (array[offset] & 0xff) << 56) |
//                ((long) (array[offset + 1] & 0xff) << 48) |
//                ((long) (array[offset + 2] & 0xff) << 40) |
//                ((long) (array[offset + 3] & 0xff) << 32) |
//                ((long) (array[offset + 4] & 0xff) << 24) |
//                ((long) (array[offset + 5] & 0xff) << 16) |
//                ((long) (array[offset + 6] & 0xff) << 8) |
//                ((long) (array[offset + 7] & 0xff));
//    }
//
//    public static long byteArrayToLong(byte[] b) {
//        return (((long) b[7]) & 0xFF) +
//                ((((long) b[6]) & 0xFF) << 8) +
//                ((((long) b[5]) & 0xFF) << 16) +
//                ((((long) b[4]) & 0xFF) << 24) +
//                ((((long) b[3]) & 0xFF) << 32) +
//                ((((long) b[2]) & 0xFF) << 40) +
//                ((((long) b[1]) & 0xFF) << 48) +
//                ((((long) b[0]) & 0xFF) << 56);
//    }
}
