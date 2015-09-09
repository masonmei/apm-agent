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

import java.util.HashMap;
import java.util.Map;

import com.baidu.oped.apm.bootstrap.interceptor.SimpleAroundInterceptor;
import com.baidu.oped.apm.bootstrap.interceptor.TargetClassLoader;
import com.baidu.oped.apm.bootstrap.interceptor.tracevalue.MapTraceValue;
import com.baidu.oped.apm.bootstrap.logging.PLogger;
import com.baidu.oped.apm.bootstrap.logging.PLoggerFactory;

/**
 * Jedis pipeline (redis client) setClient method interceptor
 * 
 * @author jaehong.kim
 *
 */
public class JedisPipelineSetClientMethodInterceptor implements SimpleAroundInterceptor, TargetClassLoader {
    private final PLogger logger = PLoggerFactory.getLogger(this.getClass());
    private final boolean isDebug = logger.isDebugEnabled();

    @Override
    public void before(Object target, Object[] args) {
        if (isDebug) {
            logger.beforeInterceptor(target, args);
        }

        // check trace endPoint
        if (!(target instanceof MapTraceValue) || !(args[0] instanceof MapTraceValue)) {
            return;
        }

        // first arg is redis.clients.jedis.Client
        final Map<String, Object> clientTraceValue = ((MapTraceValue) args[0]).__getTraceBindValue();
        if (clientTraceValue == null) {
            return;
        }

        final Map<String, Object> traceValue = new HashMap<String, Object>();
        traceValue.put("endPoint", clientTraceValue.get("endPoint"));
        ((MapTraceValue) target).__setTraceBindValue(traceValue);
    }

    @Override
    public void after(Object target, Object[] args, Object result, Throwable throwable) {
    }
}