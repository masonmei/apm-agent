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

package com.baidu.oped.apm.bootstrap;

/**
 * @author emeroad
 */
public class ProfilerLibClass {

    private static final String[] PINPOINT_PROFILER_CLASS = new String[] {
            "com.baidu.oped.apm.profiler",
            "com.baidu.oped.apm.thrift",
            "com.baidu.oped.apm.rpc",
            "javassist",
            "org.slf4j",
            "org.apache.thrift",
            "org.jboss.netty",
            "com.google.common",
            "org.apache.commons.lang",
            "org.apache.log4j",
            "com.codahale.metrics"
    };

    public boolean onLoadClass(String clazzName) {
        final int length = PINPOINT_PROFILER_CLASS.length;
        for (int i = 0; i < length; i++) {
            if (clazzName.startsWith(PINPOINT_PROFILER_CLASS[i])) {
                return true;
            }
        }
        return false;
    }
}
