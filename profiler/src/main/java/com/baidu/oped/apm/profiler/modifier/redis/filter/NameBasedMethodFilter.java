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

package com.baidu.oped.apm.profiler.modifier.redis.filter;

import java.lang.reflect.Modifier;
import java.util.Set;

import com.baidu.oped.apm.bootstrap.instrument.MethodFilter;
import com.baidu.oped.apm.bootstrap.instrument.MethodInfo;

/**
 * Name based on method filter
 * 
 * @author jaehong.kim
 *
 */
public class NameBasedMethodFilter implements MethodFilter {
    private static final int SYNTHETIC = 0x00001000;
    private final Set<String> methodNames;

    public NameBasedMethodFilter(final Set<String> methodNames) {
        this.methodNames = methodNames;
    }

    @Override
    public boolean filter(MethodInfo ctMethod) {
        final int modifiers = ctMethod.getModifiers();

        if (isSynthetic(modifiers) || !Modifier.isPublic(modifiers) || Modifier.isStatic(modifiers) || Modifier.isAbstract(modifiers) || Modifier.isNative(modifiers)) {
            return true;
        }

        if (methodNames.contains(ctMethod.getName())) {
            return false;
        }

        return true;
    }

    private boolean isSynthetic(int mod) {
        return (mod & SYNTHETIC) != 0;
    }
}