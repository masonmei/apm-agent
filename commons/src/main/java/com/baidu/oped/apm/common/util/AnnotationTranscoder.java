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


import com.baidu.oped.apm.common.bo.IntStringStringValue;
import com.baidu.oped.apm.common.bo.IntStringValue;
import com.baidu.oped.apm.common.buffer.AutomaticBuffer;
import com.baidu.oped.apm.common.buffer.Buffer;
import com.baidu.oped.apm.common.buffer.FixedBuffer;
import com.baidu.oped.apm.thrift.dto.TAnnotation;
import com.baidu.oped.apm.thrift.dto.TAnnotationValue;
import com.baidu.oped.apm.thrift.dto.TIntStringStringValue;
import com.baidu.oped.apm.thrift.dto.TIntStringValue;

/**
 * @author emeroad
 */
public class AnnotationTranscoder {

    static final byte CODE_STRING = 0;
    static final byte CODE_NULL = 1;
    static final byte CODE_INT = 2;
    static final byte CODE_LONG = 3;

    static final byte CODE_BOOLEAN_TRUE = 4;
    static final byte CODE_BOOLEAN_FALSE = 5;

    static final byte CODE_BYTEARRAY = 6;
    static final byte CODE_BYTE = 7;

    static final byte CODE_SHORT = 8;
    static final byte CODE_FLOAT = 9;
    static final byte CODE_DOUBLE = 10;
    static final byte CODE_TOSTRING = 11;
    // multivalue
    static final byte CODE_INT_STRING = 20;
    static final byte CODE_INT_STRING_STRING = 21;


    public Object getMappingValue(TAnnotation annotation) {
        final TAnnotationValue value = annotation.getValue();
        if (value == null) {
            return null;
        }
        return value.getFieldValue();
    }


    public Object decode(final byte dataType, final byte[] data) {
        switch (dataType) {
            case CODE_STRING:
                return decodeString(data);
            case CODE_BOOLEAN_TRUE:
                return Boolean.TRUE;
            case CODE_BOOLEAN_FALSE:
                return Boolean.FALSE;
            case CODE_INT: {
                final Buffer buffer = new FixedBuffer(data);
                return buffer.readSVarInt();
            }
            case CODE_LONG: {
                final Buffer buffer = new FixedBuffer(data);
                return buffer.readSVarLong();
            }
            case CODE_BYTE:
                return data[0];
            case CODE_SHORT:
                final Buffer buffer = new FixedBuffer(data);
                return (short)buffer.readSVarInt();
            case CODE_FLOAT:
                return Float.intBitsToFloat(BytesUtils.bytesToInt(data, 0));
            case CODE_DOUBLE:
                return Double.longBitsToDouble(BytesUtils.bytesToLong(data, 0));
            case CODE_BYTEARRAY:
                return data;
            case CODE_NULL:
                return null;
            case CODE_TOSTRING:
                return decodeString(data);
            case CODE_INT_STRING:
                return decodeIntStringValue(data);
            case CODE_INT_STRING_STRING:
                return decodeIntStringStringValue(data);
        }
        throw new IllegalArgumentException("unsupported DataType:" + dataType);
    }

    public byte getTypeCode(Object o) {
        if (o == null) {
            return CODE_NULL;
        }
        if (o instanceof String) {
            return CODE_STRING;
        } else if (o instanceof Long) {
            return CODE_LONG;
        } else if (o instanceof Integer) {
            return CODE_INT;
        } else if (o instanceof Boolean) {
            if (Boolean.TRUE.equals(o)) {
                return CODE_BOOLEAN_TRUE;
            }
            return CODE_BOOLEAN_FALSE;
        } else if (o instanceof Byte) {
            return CODE_BYTE;
        } else if (o instanceof Short) {
            return CODE_SHORT;
        } else if (o instanceof Float) {
            // not supported by thrift
            return CODE_FLOAT;
        } else if (o instanceof Double) {
            return CODE_DOUBLE;
        } else if (o instanceof byte[]) {
            return CODE_BYTEARRAY;
        } else if(o instanceof TIntStringValue) {
            return CODE_INT_STRING;
        } else if(o instanceof TIntStringStringValue) {
            return CODE_INT_STRING_STRING;
        }
        return CODE_TOSTRING;
    }

    public byte[] encode(Object o, int typeCode) {
        switch (typeCode) {
            case CODE_STRING:
                return encodeString((String) o);
            case CODE_INT: {
                final Buffer buffer = new FixedBuffer(BytesUtils.VINT_MAX_SIZE);
                buffer.putSVar((Integer)o);
                return buffer.getBuffer();
            }
            case CODE_BOOLEAN_TRUE: {
                return new byte[0];
            }
            case CODE_BOOLEAN_FALSE: {
                return new byte[0];
            }
            case CODE_LONG: {
                final Buffer buffer = new FixedBuffer(BytesUtils.VLONG_MAX_SIZE);
                buffer.putSVar((Long)o);
                return buffer.getBuffer();
            }
            case CODE_BYTE: {
                final byte[] bytes = new byte[1];
                bytes[0] = (Byte)o;
                return bytes;
            }
            case CODE_SHORT: {
                final Buffer buffer = new FixedBuffer(BytesUtils.VINT_MAX_SIZE);
                buffer.putSVar((Short) o);
                return buffer.getBuffer();
            }
            case CODE_FLOAT: {
                final byte[] buffer = new byte[4];
                BytesUtils.writeInt(Float.floatToRawIntBits((Float) o), buffer, 0);
                return buffer;
            }
            case CODE_DOUBLE: {
                final byte[] buffer = new byte[8];
                BytesUtils.writeLong(Double.doubleToRawLongBits((Double) o), buffer, 0);
                return buffer;
            }
            case CODE_BYTEARRAY:
                return (byte[]) o;
            case CODE_NULL:
                return null;
            case CODE_TOSTRING:
                final String str = o.toString();
                return encodeString(str);
            case CODE_INT_STRING:
                return encodeIntStringValue(o);
            case CODE_INT_STRING_STRING:
                return encodeIntStringStringValue(o);
        }
        throw new IllegalArgumentException("unsupported DataType:" + typeCode + " data:" + o);
    }


    private Object decodeIntStringValue(byte[] data) {
        final Buffer buffer = new FixedBuffer(data);
        final int intValue = buffer.readSVarInt();
        final String stringValue  = BytesUtils.toString(buffer.readPrefixedBytes());
        return new IntStringValue(intValue, stringValue);
    }

    private byte[] encodeIntStringValue(Object value) {
        final TIntStringValue tIntStringValue = (TIntStringValue) value;
        final int intValue = tIntStringValue.getIntValue();
        final byte[] stringValue = BytesUtils.toBytes(tIntStringValue.getStringValue());
        // TODO increase by a more precise value
        final int bufferSize = getBufferSize(stringValue, 4 + 8);
        final Buffer buffer = new AutomaticBuffer(bufferSize);
        buffer.putSVar(intValue);
        buffer.putPrefixedBytes(stringValue);
        return buffer.getBuffer();
    }

    private int getBufferSize(byte[] stringValue, int reserve) {
        if (stringValue == null) {
            return reserve;
        } else {
            return stringValue.length + reserve;
        }
    }

    private Object decodeIntStringStringValue(byte[] data) {
        final Buffer buffer = new FixedBuffer(data);
        final int intValue = buffer.readSVarInt();
        final String stringValue1  = BytesUtils.toString(buffer.readPrefixedBytes());
        final String stringValue2  = BytesUtils.toString(buffer.readPrefixedBytes());
        return new IntStringStringValue(intValue, stringValue1, stringValue2);
    }

    private byte[] encodeIntStringStringValue(Object o) {
        final TIntStringStringValue tIntStringStringValue = (TIntStringStringValue) o;
        final int intValue = tIntStringStringValue.getIntValue();
        final byte[] stringValue1 = BytesUtils.toBytes(tIntStringStringValue.getStringValue1());
        final byte[] stringValue2 = BytesUtils.toBytes(tIntStringStringValue.getStringValue2());
        // TODO increase by a more precise value
        final int bufferSize = getBufferSize(stringValue1, stringValue2, 4 + 8);
        final Buffer buffer = new AutomaticBuffer(bufferSize);
        buffer.putSVar(intValue);
        buffer.putPrefixedBytes(stringValue1);
        buffer.putPrefixedBytes(stringValue2);
        return buffer.getBuffer();
    }

    private int getBufferSize(byte[] stringValue1, byte[] stringValue2, int reserve) {
        int length = 0;
        if (stringValue1 != null) {
            length += stringValue1.length;
        }
        if (stringValue2 != null) {
            length += stringValue2.length;

        }
        return length + reserve;
    }


    /**
     * Decode the string with the current character set.
     */
    protected String decodeString(byte[] data) {
        return BytesUtils.toString(data);
    }

    /**
     * Encode a string into the current character set.
     */
    protected byte[] encodeString(String in) {
        return BytesUtils.toBytes(in);
    }
}
