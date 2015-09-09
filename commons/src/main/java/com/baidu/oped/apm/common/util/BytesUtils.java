/*
 * Copyright 2014 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.baidu.oped.apm.common.util;


import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author emeroad
 */
public final class BytesUtils {
    public static final int SHORT_BYTE_LENGTH = 2;
    public static final int INT_BYTE_LENGTH = 4;
    public static final int LONG_BYTE_LENGTH = 8;
    public static final int LONG_LONG_BYTE_LENGTH = 16;

    public static final int VLONG_MAX_SIZE = 10;
    public static final int VINT_MAX_SIZE = 5;

    private static final byte[] EMPTY_BYTES = new byte[0];
    private static final String UTF8 = "UTF-8";
    private static final Logger LOGGER = Logger.getLogger(BytesUtils.class.getName());

    private BytesUtils() {
    }

    public static byte[] stringLongLongToBytes(final String string, final int maxStringSize, final long value1, final long value2) {
        if (string == null) {
            throw new NullPointerException("string must not be null");
        }
        if (maxStringSize < 0) {
            throw new IndexOutOfBoundsException("maxStringSize");
        }
        final byte[] stringBytes = toBytes(string);
        if (stringBytes.length > maxStringSize) {
            throw new IndexOutOfBoundsException("string is max " + stringBytes.length + ", string='" + string + "'");
        }
        final byte[] buffer = new byte[LONG_LONG_BYTE_LENGTH + maxStringSize];
        writeBytes(buffer, 0, stringBytes);
        writeFirstLong0(value1, buffer, maxStringSize);
        writeSecondLong0(value2, buffer, maxStringSize);
        return buffer;
    }

    public static int writeBytes(final byte[] buffer, int bufferOffset, final byte[] srcBytes) {
        if (srcBytes == null) {
            throw new NullPointerException("srcBytes must not be null");
        }
        return writeBytes(buffer, bufferOffset, srcBytes, 0, srcBytes.length);
    }

    public static int writeBytes(final byte[] buffer, final int bufferOffset, final byte[] srcBytes, final int srcOffset, final int srcLength) {
        if (buffer == null) {
            throw new NullPointerException("buffer must not be null");
        }
        if (srcBytes == null) {
            throw new NullPointerException("stringBytes must not be null");
        }
        if (bufferOffset < 0) {
            throw new IndexOutOfBoundsException("negative bufferOffset:" + bufferOffset);
        }
        if (srcOffset < 0) {
            throw new IndexOutOfBoundsException("negative srcOffset offset:" + srcOffset);
        }
        System.arraycopy(srcBytes, srcOffset, buffer, bufferOffset, srcLength);
        return bufferOffset + srcLength;
    }

    public static long bytesToLong(final byte[] buf, final int offset) {
        if (buf == null) {
            throw new NullPointerException("buf must not be null");
        }
        if (offset < 0) {
            throw new IndexOutOfBoundsException("negative offset:" + offset);
        }
        if (buf.length < offset + LONG_BYTE_LENGTH) {
            throw new IndexOutOfBoundsException("buf.length is too small. buf.length:" + buf.length + " offset:" + (offset + 8));
        }

        final long rv = (((long) buf[offset] & 0xff) << 56)
                | (((long) buf[offset + 1] & 0xff) << 48)
                | (((long) buf[offset + 2] & 0xff) << 40)
                | (((long) buf[offset + 3] & 0xff) << 32)
                | (((long) buf[offset + 4] & 0xff) << 24)
                | (((long) buf[offset + 5] & 0xff) << 16)
                | (((long) buf[offset + 6] & 0xff) << 8)
                | (((long) buf[offset + 7] & 0xff));
        return rv;
    }

    public static int bytesToInt(final byte[] buf, final int offset) {
        if (buf == null) {
            throw new NullPointerException("buf must not be null");
        }
        if (offset < 0) {
            throw new IndexOutOfBoundsException("negative offset:" + offset);
        }
        if (buf.length < offset + INT_BYTE_LENGTH) {
            throw new IndexOutOfBoundsException("buf.length is too small. buf.length:" + buf.length + " offset:" + (offset + 4));
        }

        final int v = ((buf[offset] & 0xff) << 24)
                | ((buf[offset + 1] & 0xff) << 16)
                | ((buf[offset + 2] & 0xff) << 8)
                | ((buf[offset + 3] & 0xff));

        return v;
    }

    public static short bytesToShort(final byte[] buf, final int offset) {
        if (buf == null) {
            throw new NullPointerException("buf must not be null");
        }
        if (offset < 0) {
            throw new IndexOutOfBoundsException("negative offset:" + offset);
        }
        if (buf.length < offset + SHORT_BYTE_LENGTH) {
            throw new IndexOutOfBoundsException("buf.length is too small. buf.length:" + buf.length + " offset:" + (offset + 2));
        }

        final short v = (short) (((buf[offset] & 0xff) << 8) | ((buf[offset + 1] & 0xff)));

        return v;
    }

    public static short bytesToShort(final byte byte1, final byte byte2) {
        return (short) (((byte1 & 0xff) << 8) | ((byte2 & 0xff)));
    }


    public static int writeLong(final long value, final byte[] buf, int offset) {
        if (buf == null) {
            throw new NullPointerException("buf must not be null");
        }
        if (offset < 0) {
            throw new IndexOutOfBoundsException("negative offset:" + offset);
        }
        if (buf.length < offset + LONG_BYTE_LENGTH) {
            throw new IndexOutOfBoundsException("buf.length is too small. buf.length:" + buf.length + " offset:" + (offset + 8));
        }
        buf[offset++] = (byte) (value >> 56);
        buf[offset++] = (byte) (value >> 48);
        buf[offset++] = (byte) (value >> 40);
        buf[offset++] = (byte) (value >> 32);
        buf[offset++] = (byte) (value >> 24);
        buf[offset++] = (byte) (value >> 16);
        buf[offset++] = (byte) (value >> 8);
        buf[offset++] = (byte) (value);
        return offset;
    }

    public static byte writeShort1(final short value) {
        return (byte) (value >> 8);
    }

    public static byte writeShort2(final short value) {
        return (byte) (value);
    }

    public static int writeShort(final short value, final byte[] buf, int offset) {
        if (buf == null) {
            throw new NullPointerException("buf must not be null");
        }
        if (offset < 0) {
            throw new IndexOutOfBoundsException("negative offset:" + offset);
        }
        if (buf.length < offset + SHORT_BYTE_LENGTH) {
            throw new IndexOutOfBoundsException("buf.length is too small. buf.length:" + buf.length + " offset:" + (offset + 2));
        }
        buf[offset++] = (byte) (value >> 8);
        buf[offset++] = (byte) (value);
        return offset;
    }

    public static int writeInt(final int value, final byte[] buf, int offset) {
        if (buf == null) {
            throw new NullPointerException("buf must not be null");
        }
        if (offset < 0) {
            throw new IndexOutOfBoundsException("negative offset:" + offset);
        }
        if (buf.length < offset + INT_BYTE_LENGTH) {
            throw new IndexOutOfBoundsException("buf.length is too small. buf.length:" + buf.length + " offset:" + (offset + 4));
        }
        buf[offset++] = (byte) (value >> 24);
        buf[offset++] = (byte) (value >> 16);
        buf[offset++] = (byte) (value >> 8);
        buf[offset++] = (byte) (value);
        return offset;
    }

    public static int writeSVar32(final int value, final byte[] buf, final int offset) {
        return writeVar32(intToZigZag(value), buf, offset);
    }

    public static int writeVar32(int value, final byte[] buf, int offset) {
        if (buf == null) {
            throw new NullPointerException("buf must not be null");
        }
        if (offset < 0) {
            throw new IndexOutOfBoundsException("negative offset:" + offset);
        }
        while (true) {
            if ((value & ~0x7F) == 0) {
                buf[offset++] = (byte)value;
                return offset;
            } else {
                buf[offset++] = (byte)((value & 0x7F) | 0x80);
                value >>>= 7;
            }
        }
    }

    public static int writeVar64(long value, final byte[] buf, int offset) {
        if (buf == null) {
            throw new NullPointerException("buf must not be null");
        }
        if (offset < 0) {
            throw new IndexOutOfBoundsException("negative offset:" + offset);
        }
        while (true) {
            if ((value & ~0x7FL) == 0) {
                buf[offset++] = (byte)value;
                return offset;
            } else {
                buf[offset++] = (byte)(((int)value & 0x7F) | 0x80);
                value >>>= 7;
            }
        }
    }


    private static int writeFirstLong0(final long value, final byte[] buf, int offset) {
        buf[offset] = (byte) (value >> 56);
        buf[1 + offset] = (byte) (value >> 48);
        buf[2 + offset] = (byte) (value >> 40);
        buf[3 + offset] = (byte) (value >> 32);
        buf[4 + offset] = (byte) (value >> 24);
        buf[5 + offset] = (byte) (value >> 16);
        buf[6 + offset] = (byte) (value >> 8);
        buf[7 + offset] = (byte) (value);
        return offset;
    }


    private static Logger getLogger() {
        return Logger.getLogger(BytesUtils.class.getName());
    }


    private static int writeSecondLong0(final long value, final byte[] buf, int offset) {
        buf[8 + offset] = (byte) (value >> 56);
        buf[9 + offset] = (byte) (value >> 48);
        buf[10 + offset] = (byte) (value >> 40);
        buf[11 + offset] = (byte) (value >> 32);
        buf[12 + offset] = (byte) (value >> 24);
        buf[13 + offset] = (byte) (value >> 16);
        buf[14 + offset] = (byte) (value >> 8);
        buf[15 + offset] = (byte) (value);
        return offset;
    }

    public static byte[] add(final String prefix, final long postfix) {
        if (prefix == null) {
            throw new NullPointerException("prefix must not be null");
        }
        byte[] agentByte = toBytes(prefix);
        return add(agentByte, postfix);
    }

    public static byte[] add(final byte[] preFix, final long postfix) {
        byte[] buf = new byte[preFix.length + LONG_BYTE_LENGTH];
        System.arraycopy(preFix, 0, buf, 0, preFix.length);
        writeLong(postfix, buf, preFix.length);
        return buf;
    }

    public static byte[] add(final byte[] preFix, final short postfix) {
        byte[] buf = new byte[preFix.length + SHORT_BYTE_LENGTH];
        System.arraycopy(preFix, 0, buf, 0, preFix.length);
        writeShort(postfix, buf, preFix.length);
        return buf;
    }

    public static byte[] add(final int preFix, final short postFix) {
        byte[] buf = new byte[INT_BYTE_LENGTH + SHORT_BYTE_LENGTH];
        writeInt(preFix, buf, 0);
        writeShort(postFix, buf, 4);
        return buf;
    }


    public static byte[] add(final long preFix, final short postFix) {
        byte[] buf = new byte[LONG_BYTE_LENGTH + SHORT_BYTE_LENGTH];
        writeLong(preFix, buf, 0);
        writeShort(postFix, buf, 8);
        return buf;
    }

    public static byte[] toBytes(final String value) {
        if (value == null) {
            return null;
        }
        try {
            return value.getBytes(UTF8);
        } catch (UnsupportedEncodingException e) {
            final Logger logger = getLogger();
            logger.log(Level.SEVERE, "String encoding fail. value:" + value + " Caused:" + e.getMessage(), e);
            return EMPTY_BYTES;
        }
    }

    public static byte[] merge(final byte[] b1, final byte[] b2) {
        if (b1 == null) {
            throw new NullPointerException("b1 must not be null");
        }
        if (b2 == null) {
            throw new NullPointerException("b2 must not be null");
        }
        final byte[] result = new byte[b1.length + b2.length];

        System.arraycopy(b1, 0, result, 0, b1.length);
        System.arraycopy(b2, 0, result, b1.length, b2.length);

        return result;
    }

    public static byte[] toFixedLengthBytes(final String str, final int length) {
        if (length < 0) {
            throw new IndexOutOfBoundsException("negative length:" + length);
        }
        final byte[] b1 = toBytes(str);
        if (b1 == null) {
            return new byte[length];
        }

        if (b1.length > length) {
            throw new IndexOutOfBoundsException("String is longer then target length of bytes.");
        }
        byte[] b = new byte[length];
        System.arraycopy(b1, 0, b, 0, b1.length);

        return b;
    }


    public static int intToZigZag(final int n) {
        return (n << 1) ^ (n >> 31);
    }

    public static int zigzagToInt(final int n) {
        return (n >>> 1) ^ -(n & 1);
    }


    public static long longToZigZag(final long n) {
        return (n << 1) ^ (n >> 63);
    }

    public static long zigzagToLong(final long n) {
        return (n >>> 1) ^ -(n & 1);
    }

    public static byte[] concat(final byte[]... arrays) {
        int totalLength = 0;

        final int length = arrays.length;
        for (int i = 0; i < length; i++) {
            totalLength += arrays[i].length;
        }

        byte[] result = new byte[totalLength];

        int currentIndex = 0;
        for (int i = 0; i < length; i++) {
            System.arraycopy(arrays[i], 0, result, currentIndex, arrays[i].length);
            currentIndex += arrays[i].length;
        }

        return result;
    }

    public static String safeTrim(final String string) {
        if (string == null) {
            return null;
        }
        return string.trim();
    }

    public static String toString(final byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        return toString(bytes, 0, bytes.length);
    }

    public static String toString(final byte [] bytes, final int offset, final int length) {
        if (bytes == null) {
            return null;
        }
        if (offset < 0) {
            throw new IndexOutOfBoundsException("negative offset:" + offset);
        }
        if (length == 0) {
            return "";
        }
        try {
            return new String(bytes, offset, length, UTF8);
        } catch (UnsupportedEncodingException e) {
            LOGGER.log(Level.SEVERE, "UTF-8 encoding fail.", e);
            return null;
        }
    }

    public static String toStringAndRightTrim(final byte[] bytes, final int offset, final int length) {
        String string = toString(bytes, offset, length);
        return trimRight(string);
    }

    public static String trimRight(final String string) {
        if (string == null) {
            return null;
        }
        final int length = string.length();
        int index = length;

        // need to use Character.isWhitespace()? may not needed.
        while (string.charAt(index - 1) <= ' ') {
            index--;
        }
        if (index == length) {
            return string;
        } else {
            return string.substring(0, index);
        }
    }
}
