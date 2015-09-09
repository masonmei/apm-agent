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
package com.baidu.oped.apm.profiler.modifier.log.logback.interceptor;

import org.slf4j.MDC;

import com.baidu.oped.apm.bootstrap.context.Trace;
import com.baidu.oped.apm.bootstrap.context.TraceContext;
import com.baidu.oped.apm.bootstrap.interceptor.SimpleAroundInterceptor;
import com.baidu.oped.apm.bootstrap.interceptor.TargetClassLoader;
import com.baidu.oped.apm.bootstrap.interceptor.TraceContextSupport;
import com.baidu.oped.apm.profiler.modifier.log.MdcKey;

/**
 * @author minwoo.jung
 */
public class LoggingEventOfLogbackInterceptor implements SimpleAroundInterceptor, TraceContextSupport, TargetClassLoader {

    private TraceContext traceContext;

    @Override
    public void before(Object target, Object[] args) {
        Trace trace = traceContext.currentTraceObject();
        
        if (trace == null) {
            MDC.remove(MdcKey.TRANSACTION_ID);
            MDC.remove(MdcKey.SPAN_ID);
            return;
        } else {
            MDC.put(MdcKey.TRANSACTION_ID, trace.getTraceId().getTransactionId());
            MDC.put(MdcKey.SPAN_ID, String.valueOf(trace.getTraceId().getSpanId()));
        }
    }

    @Override
    public void after(Object target, Object[] args, Object result, Throwable throwable) {
    }

    @Override
    public void setTraceContext(TraceContext traceContext) {
        this.traceContext = traceContext;
    }

}
