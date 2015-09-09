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

package com.baidu.oped.apm.test;

import org.junit.Assert;

import org.junit.Before;
import org.junit.BeforeClass;

import com.baidu.oped.apm.bootstrap.context.TraceContext;
import com.baidu.oped.apm.bootstrap.interceptor.ByteCodeMethodDescriptorSupport;
import com.baidu.oped.apm.bootstrap.interceptor.Interceptor;
import com.baidu.oped.apm.bootstrap.interceptor.MethodDescriptor;
import com.baidu.oped.apm.bootstrap.interceptor.TraceContextSupport;
import com.baidu.oped.apm.bootstrap.logging.PLoggerFactory;
import com.baidu.oped.apm.profiler.logging.Slf4jLoggerBinder;

public class BaseInterceptorTest {

    Interceptor interceptor;
    MethodDescriptor descriptor;

    public void setInterceptor(Interceptor interceptor) {
        this.interceptor = interceptor;
    }

    public void setMethodDescriptor(MethodDescriptor methodDescriptor) {
        this.descriptor = methodDescriptor;
    }

    @BeforeClass
    public static void before() {
        PLoggerFactory.initialize(new Slf4jLoggerBinder());
    }

    @Before
    public void beforeEach() {
        if (interceptor == null) {
            Assert.fail("set the interceptor first.");
        }

        if (interceptor instanceof TraceContextSupport) {
            // sampler

            // trace context
            TraceContext traceContext = new MockTraceContextFactory().create();
            ((TraceContextSupport) interceptor).setTraceContext(traceContext);
        }

        if (interceptor instanceof ByteCodeMethodDescriptorSupport) {
            if (descriptor != null) {
                ((ByteCodeMethodDescriptorSupport)interceptor).setMethodDescriptor(descriptor);
            }
        }
    }

}
