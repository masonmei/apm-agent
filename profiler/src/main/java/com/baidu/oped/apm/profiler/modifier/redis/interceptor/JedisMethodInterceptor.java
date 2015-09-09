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

package com.baidu.oped.apm.profiler.modifier.redis.interceptor;

import java.util.Map;

import com.baidu.oped.apm.bootstrap.context.RecordableTrace;
import com.baidu.oped.apm.bootstrap.interceptor.SpanEventSimpleAroundInterceptor;
import com.baidu.oped.apm.bootstrap.interceptor.TargetClassLoader;
import com.baidu.oped.apm.bootstrap.interceptor.tracevalue.MapTraceValue;
import com.baidu.oped.apm.common.ServiceType;

/**
 * Jedis (redis client) method interceptor
 * 
 * @author jaehong.kim
 *
 */
public class JedisMethodInterceptor extends SpanEventSimpleAroundInterceptor implements TargetClassLoader {

    public JedisMethodInterceptor() {
        super(JedisMethodInterceptor.class);
    }

    @Override
    public void doInBeforeTrace(RecordableTrace trace, Object target, Object[] args) {
        trace.markBeforeTime();
    }

    @Override
    public void doInAfterTrace(RecordableTrace trace, Object target, Object[] args, Object result, Throwable throwable) {
        String endPoint = null;
        if (target instanceof MapTraceValue) {
            final Map<String, Object> traceValue = ((MapTraceValue) target).__getTraceBindValue();
            if (traceValue != null) {
                endPoint = (String) traceValue.get("endPoint");
            }
        }

        trace.recordApi(getMethodDescriptor());
        trace.recordEndPoint(endPoint != null ? endPoint : "Unknown");
        trace.recordDestinationId(ServiceType.REDIS.toString());
        trace.recordServiceType(ServiceType.REDIS);
        trace.recordException(throwable);
        trace.markAfterTime();
    }
}