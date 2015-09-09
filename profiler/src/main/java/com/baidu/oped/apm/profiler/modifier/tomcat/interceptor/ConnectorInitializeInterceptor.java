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

package com.baidu.oped.apm.profiler.modifier.tomcat.interceptor;

import com.baidu.oped.apm.bootstrap.context.TraceContext;
import com.baidu.oped.apm.bootstrap.interceptor.SimpleAroundInterceptor;
import com.baidu.oped.apm.bootstrap.interceptor.TargetClassLoader;
import com.baidu.oped.apm.bootstrap.interceptor.TraceContextSupport;
import com.baidu.oped.apm.bootstrap.logging.PLogger;
import com.baidu.oped.apm.bootstrap.logging.PLoggerFactory;

import org.apache.catalina.connector.Connector;

/**
 * @author emeroad
 */
public class ConnectorInitializeInterceptor implements SimpleAroundInterceptor, TraceContextSupport, TargetClassLoader {

    private PLogger logger = PLoggerFactory.getLogger(this.getClass());
    private final boolean isDebug = logger.isDebugEnabled();

    private TraceContext traceContext;
    
    @Override
    public void setTraceContext(TraceContext traceContext) {
        this.traceContext = traceContext;
    }

    @Override
    public void before(Object target, Object[] args) {

    }

    @Override
    public void after(Object target, Object[] args, Object result, Throwable throwable) {
        if (isDebug) {
            logger.afterInterceptor(target, args, result, throwable);
        }
        Connector connector = (Connector) target;
        this.traceContext.getServerMetaDataHolder().addConnector(connector.getProtocol(), connector.getPort());

    }
}
