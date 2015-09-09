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

package com.baidu.oped.apm.profiler.util;


import com.baidu.oped.apm.bootstrap.util.StringUtils;
import com.baidu.oped.apm.thrift.dto.*;

import org.apache.thrift.TBase;

/**
 * @author emeroad
 */
public final class AnnotationValueMapper {

    private AnnotationValueMapper() {
    }

    public static void mappingValue(TAnnotation annotation, Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof String) {
            annotation.setValue(TAnnotationValue.stringValue((String) value));
            return;
        } else if (value instanceof Integer) {
            annotation.setValue(TAnnotationValue.intValue((Integer) value));
            return;
        } else if (value instanceof Long) {
            annotation.setValue(TAnnotationValue.longValue((Long) value));
            return;
        } else if (value instanceof Boolean) {
            annotation.setValue(TAnnotationValue.boolValue((Boolean) value));
            return;
        } else if (value instanceof Byte) {
            annotation.setValue(TAnnotationValue.byteValue((Byte) value));
            return;
        } else if (value instanceof Float) {
            // thrift does not contain "float" type
            annotation.setValue(TAnnotationValue.doubleValue((Float) value));
            return;
        } else if (value instanceof Double) {
            annotation.setValue(TAnnotationValue.doubleValue((Double) value));
            return;
        } else if (value instanceof byte[]) {
            annotation.setValue(TAnnotationValue.binaryValue((byte[]) value));
            return;
        } else if (value instanceof Short) {
            annotation.setValue(TAnnotationValue.shortValue((Short) value));
            return;
        } else if (value instanceof TBase) {
            throw new IllegalArgumentException("TBase not supported. Class:" + value.getClass());
        }
        String str = StringUtils.drop(value.toString());
        annotation.setValue(TAnnotationValue.stringValue(str));
    }

}
