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

package com.baidu.oped.apm.profiler.modifier.tomcat.aspect;

import com.baidu.oped.apm.bootstrap.context.Header;
import com.baidu.oped.apm.profiler.interceptor.aspect.Aspect;
import com.baidu.oped.apm.profiler.interceptor.aspect.JointPoint;
import com.baidu.oped.apm.profiler.interceptor.aspect.PointCut;

import java.util.Enumeration;

/**
 * filtering pinpoint header
 * @author emeroad
 */
@Aspect
public abstract class RequestFacadeAspect {

    @PointCut
    public String getHeader(String name) {
        if (Header.hasHeader(name)) {
            return null;
        }
        return __getHeader(name);
    }

    @JointPoint
    abstract String __getHeader(String name);


    @PointCut
    public Enumeration getHeaders(String name) {
        final Enumeration headers = Header.getHeaders(name);
        if (headers != null) {
            return headers;
        }
        return __getHeaders(name);
    }

    @JointPoint
    abstract Enumeration __getHeaders(String name);


    @PointCut
    public Enumeration getHeaderNames() {
        final Enumeration enumeration = __getHeaderNames();
        return Header.filteredHeaderNames(enumeration);
    }

    @JointPoint
    abstract Enumeration __getHeaderNames();

}
