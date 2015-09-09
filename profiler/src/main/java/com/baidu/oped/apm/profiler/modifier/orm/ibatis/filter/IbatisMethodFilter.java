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

package com.baidu.oped.apm.profiler.modifier.orm.ibatis.filter;

import java.lang.reflect.Modifier;

import com.baidu.oped.apm.bootstrap.instrument.MethodFilter;
import com.baidu.oped.apm.bootstrap.instrument.MethodInfo;

/**
 * @author Hyun Jeong
 */
public abstract class IbatisMethodFilter implements MethodFilter {

    private static final boolean TRACK = false;
    private static final boolean DO_NOT_TRACK = true;

    protected abstract boolean shouldTrackMethod(String methodName);

    @Override
    public boolean filter(MethodInfo ctMethod) {
        final int modifiers = ctMethod.getModifiers();
        if (!Modifier.isPublic(modifiers) || Modifier.isStatic(modifiers) || Modifier.isAbstract(modifiers) || Modifier.isNative(modifiers)) {
            return DO_NOT_TRACK;
        }
        return filterApiForTracking(ctMethod);
    }

    private boolean filterApiForTracking(MethodInfo ctMethod) {
        if (!shouldTrackMethod(ctMethod.getName())) {
            return DO_NOT_TRACK;
        }

        final int parameterIndexToMatch = 0; // 0-based index
        String[] parameterTypes = ctMethod.getParameterTypes();
        if (parameterTypes != null && parameterTypes.length > 0) {
            return parameterTypeMatches(parameterTypes, parameterIndexToMatch, String.class);
        } else {
            return TRACK;
        }
    }

    private boolean parameterTypeMatches(final String[] parameterTypes, final int parameterIndex, final Class<?> parameterType) {
        if (parameterTypes == null || parameterTypes.length <= parameterIndex) {
            return DO_NOT_TRACK;
        }
        if (parameterType.getName().equals(parameterTypes[parameterIndex])) {
            return TRACK;
        }
        return DO_NOT_TRACK;
    }

}
