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

package com.baidu.oped.apm.profiler.modifier.arcus.interceptor;

import com.baidu.oped.apm.bootstrap.context.Trace;
import com.baidu.oped.apm.bootstrap.context.TraceContext;
import com.baidu.oped.apm.bootstrap.interceptor.*;
import com.baidu.oped.apm.bootstrap.interceptor.tracevalue.ObjectTraceValue3Utils;
import com.baidu.oped.apm.bootstrap.logging.PLogger;
import com.baidu.oped.apm.bootstrap.logging.PLoggerFactory;
import com.baidu.oped.apm.common.ServiceType;

/**
 * @author harebox
 */
public class FrontCacheGetFutureGetInterceptor implements SimpleAroundInterceptor, ByteCodeMethodDescriptorSupport, TraceContextSupport, TargetClassLoader {

    private final PLogger logger = PLoggerFactory.getLogger(this.getClass());
    private final boolean isDebug = logger.isDebugEnabled();

//    cacheKey->ObjectTrace4

    private MethodDescriptor methodDescriptor;
    private TraceContext traceContext;

    @Override
    public void before(Object target, Object[] args) {
        if (isDebug) {
            logger.beforeInterceptor(target, args);
        }

        final Trace trace = traceContext.currentTraceObject();
        if (trace == null) {
            return;
        }

        trace.traceBlockBegin();
        trace.markBeforeTime();
    }

    @Override
    public void after(Object target, Object[] args, Object result, Throwable throwable) {
        if (isDebug) {
            logger.afterInterceptor(target, args, result, throwable);
        }

        final Trace trace = traceContext.currentTraceObject();
        if (trace == null) {
            return;
        }

        try {
            trace.recordApi(methodDescriptor);

//            String cacheKey = (String) getCacheKey.invoke(target);
//            if (cacheKey != null) {
//                // annotate it.
//            }

            final String cacheName = getCacheName(target);
            if (cacheName != null) {
                trace.recordDestinationId(cacheName);
            }

            trace.recordServiceType(ServiceType.ARCUS_EHCACHE_FUTURE_GET);
            trace.markAfterTime();
        } finally {
            trace.traceBlockEnd();
        }
    }

//    __cacheName->ObjectTrace3
    private String getCacheName(Object result) {
        final Object cacheNameObject = ObjectTraceValue3Utils.__getTraceObject3(result, null);
        if (cacheNameObject instanceof String) {
            return (String) cacheNameObject;
        }
        return null;
    }

    @Override
    public void setMethodDescriptor(MethodDescriptor descriptor) {
        this.methodDescriptor = descriptor;
        this.traceContext.cacheApi(descriptor);
    }

    @Override
    public void setTraceContext(TraceContext traceContext) {
        this.traceContext = traceContext;
    }

}
