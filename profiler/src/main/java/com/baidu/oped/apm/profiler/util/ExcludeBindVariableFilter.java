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

import java.lang.reflect.Method;

/**
 * @author emeroad
 */
public class ExcludeBindVariableFilter implements BindVariableFilter {

    private String[] excudes;

    public ExcludeBindVariableFilter(String[] excludes) {
        if (excludes == null) {
            throw new NullPointerException("excludes must not be null");
        }
        this.excudes = excludes;
    }

    @Override
    public boolean filter(Method method) {
        if (method == null) {
            throw new NullPointerException("method must not be null");
        }
        for (String exclude : excudes) {
            if(method.getName().equals(exclude)) {
                return false;
            }
        }
        return true;
    }
}
