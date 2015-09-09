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

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.transport.TMemoryBuffer;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.primitives.Ints;


public class BytesUtilsTest {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());


    @Test
    public void testStringLongLongToBytes() {
        BytesUtils.stringLongLongToBytes("123", 3, 1, 2);
        try {
            BytesUtils.stringLongLongToBytes("123", 2, 1, 2);
            Assert.fail("fail");
        } catch (IndexOutOfBoundsException ignore) {
        }
    }

    @Test
    public void testStringLongLongToBytes2() {
        byte[] bytes = BytesUtils.stringLongLongToBytes("123", 10, 1, 2);
        String s = BytesUtils.toStringAndRightTrim(bytes, 0, 10);
        Assert.assertEquals("123", s);
        long l = BytesUtils.bytesToLong(bytes, 10);
        Assert.assertEquals(l, 1);
        long l2 = BytesUtils.bytesToLong(bytes, 10 + BytesUtils.LONG_BYTE_LENGTH);
        Assert.assertEquals(l2, 2);
    }

    @Test
    public void testRightTrim() {
        String trim = BytesUtils.trimRight("test  ");
        Assert.assertEquals("test", trim);

        String trim1 = BytesUtils.trimRight("test");
        Assert.assertEquals("test", trim1);

        String trim2 = BytesUtils.trimRight("  test");
        Assert.assertEquals("  test", trim2);

    }


    @Test
    public void testInt() {
        int i = Integer.MAX_VALUE - 5;
        checkInt(i);
        checkInt(23464);
    }

    private void checkInt(int i) {
    	byte[] bytes = Ints.toByteArray(i);
        int i2 = BytesUtils.bytesToInt(bytes, 0);
        Assert.assertEquals(i, i2);
        int i3 = Ints.fromByteArray(bytes);
        Assert.assertEquals(i, i3);
    }


    @Test
    public void testAddStringLong() {
        byte[] testAgents = BytesUtils.add("testAgent", 11L);
        byte[] buf = ByteBuffer.allocate(17).put("testAgent".getBytes()).putLong(11L).array();
        Assert.assertArrayEquals(testAgents, buf);
    }

    @Test
    public void testAddStringLong_NullError() {
        try {
            BytesUtils.add((String) null, 11L);
            Assert.fail();
        } catch (NullPointerException ignore) {
        }
    }

    @Test
    public void testToFixedLengthBytes() {
        byte[] testValue = BytesUtils.toFixedLengthBytes("test", 10);
        Assert.assertEquals(testValue.length, 10);
        Assert.assertEquals(testValue[5], 0);

        try {
            BytesUtils.toFixedLengthBytes("test", 2);
            Assert.fail();
        } catch (IndexOutOfBoundsException ignore) {
        }

        try {
            BytesUtils.toFixedLengthBytes("test", -1);
            Assert.fail();
        } catch (IndexOutOfBoundsException ignore) {
        }

        byte[] testValue2 = BytesUtils.toFixedLengthBytes(null, 10);
        Assert.assertEquals(testValue2.length, 10);

    }

    @Test
    public void testMerge() {
        byte[] b1 = new byte[]{1, 2};
        byte[] b2 = new byte[]{3, 4};

        byte[] b3 = BytesUtils.merge(b1, b2);

        Assert.assertTrue(Arrays.equals(new byte[]{1, 2, 3, 4}, b3));
    }

    @Test
    public void testZigZag() {
        testEncodingDecodingZigZag(0);
        testEncodingDecodingZigZag(1);
        testEncodingDecodingZigZag(2);
        testEncodingDecodingZigZag(3);
    }


    private void testEncodingDecodingZigZag(int value) {
        int encode = BytesUtils.intToZigZag(value);
        int decode = BytesUtils.zigzagToInt(encode);
        Assert.assertEquals(value, decode);
    }


    @Test
    public void compactProtocolVint() throws TException {
        TMemoryBuffer tMemoryBuffer = writeVInt32(BytesUtils.zigzagToInt(64));
        logger.debug("length:{}", tMemoryBuffer.length());

        TMemoryBuffer tMemoryBuffer2 = writeVInt32(64);
        logger.debug("length:{}", tMemoryBuffer2.length());

    }

    private TMemoryBuffer writeVInt32(int i) throws TException {
        TMemoryBuffer tMemoryBuffer = new TMemoryBuffer(10);
        TCompactProtocol tCompactProtocol = new TCompactProtocol(tMemoryBuffer);
        tCompactProtocol.writeI32(i);
        return tMemoryBuffer;
    }

    @Test
    public void testWriteBytes1() {
        byte[] buffer = new byte[10];
        byte[] write = new byte[]{1, 2, 3, 4};

        Assert.assertEquals(BytesUtils.writeBytes(buffer, 0, write), write.length);
        Assert.assertArrayEquals(Arrays.copyOf(buffer, write.length), write);
    }

    @Test
    public void testWriteBytes2() {
        byte[] buffer = new byte[10];
        byte[] write = new byte[]{1, 2, 3, 4};
        int startOffset = 1;
        Assert.assertEquals(BytesUtils.writeBytes(buffer, startOffset, write), write.length + startOffset);
        Assert.assertArrayEquals(Arrays.copyOfRange(buffer, startOffset, write.length + startOffset), write);
    }
}
