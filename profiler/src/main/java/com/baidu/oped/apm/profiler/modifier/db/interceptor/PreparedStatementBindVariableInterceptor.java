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

package com.baidu.oped.apm.profiler.modifier.db.interceptor;

import com.baidu.oped.apm.bootstrap.context.Trace;
import com.baidu.oped.apm.bootstrap.context.TraceContext;
import com.baidu.oped.apm.bootstrap.interceptor.StaticAroundInterceptor;
import com.baidu.oped.apm.bootstrap.interceptor.TraceContextSupport;
import com.baidu.oped.apm.bootstrap.interceptor.tracevalue.BindValueTraceValue;
import com.baidu.oped.apm.bootstrap.logging.PLogger;
import com.baidu.oped.apm.bootstrap.logging.PLoggerFactory;
import com.baidu.oped.apm.bootstrap.util.NumberUtils;
import com.baidu.oped.apm.profiler.util.bindvalue.BindValueConverter;

import java.util.Map;

/**
 * @author emeroad
 */
public class PreparedStatementBindVariableInterceptor implements StaticAroundInterceptor, TraceContextSupport {

    private final PLogger logger = PLoggerFactory.getLogger(this.getClass());
    private final boolean isDebug = logger.isDebugEnabled();

    private TraceContext traceContext;

    @Override
    public void before(Object target, String className, String methodName, String parameterDescription, Object[] args) {
    }

    @Override
    public void after(Object target, String className, String methodName, String parameterDescription, Object[] args, Object result, Throwable throwable) {

        if (isDebug) {
            logger.afterInterceptor(target, className, methodName, parameterDescription, args, result, throwable);
        }

        final Trace trace = traceContext.currentTraceObject();
        if (trace == null) {
            return;
        }
        Map<Integer, String> bindList = null;
        if (target instanceof BindValueTraceValue) {
            bindList = ((BindValueTraceValue)target).__getTraceBindValue();
        }
        if (bindList == null) {
            if (logger.isWarnEnabled()) {
                logger.warn("bindValue is null");
            }
            return;
        }
        Integer index = NumberUtils.toInteger(args[0]);
        if (index == null) {
            // something is wrong
            return;
        }
        String value = BindValueConverter.convert(methodName, args);
        bindList.put(index, value);

    }

    @Override
    public void setTraceContext(TraceContext traceContext) {
        this.traceContext = traceContext;
    }


}
