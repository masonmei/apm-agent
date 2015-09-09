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

package com.baidu.oped.apm.profiler.modifier.connector.httpclient4;

import java.security.ProtectionDomain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baidu.oped.apm.bootstrap.Agent;
import com.baidu.oped.apm.bootstrap.instrument.ByteCodeInstrumentor;
import com.baidu.oped.apm.bootstrap.instrument.InstrumentClass;
import com.baidu.oped.apm.bootstrap.instrument.InstrumentException;
import com.baidu.oped.apm.bootstrap.interceptor.Interceptor;
import com.baidu.oped.apm.profiler.modifier.AbstractModifier;

/**
 * For HTTP Client 4.3 or later.
 * 
 * @author netspider
 * 
 */
public class ClosableHttpAsyncClientModifier extends AbstractModifier {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public ClosableHttpAsyncClientModifier(ByteCodeInstrumentor byteCodeInstrumentor, Agent agent) {
        super(byteCodeInstrumentor, agent);
    }

    @Override
    public String getTargetClass() {
        return "org/apache/http/impl/nio/client/CloseableHttpAsyncClient";
    }

    @Override
    public byte[] modify(ClassLoader classLoader, String javassistClassName, ProtectionDomain protectedDomain, byte[] classFileBuffer) {
        if (logger.isInfoEnabled()) {
            logger.info("Modifing. {} @ {}", javassistClassName, classLoader);
        }


        try {
            InstrumentClass aClass = byteCodeInstrumentor.getClass(classLoader, javassistClassName, classFileBuffer);
 
            /**
             * Below two methods are overloaded, but they don't call each other. No Scope required.
             */
            Interceptor executeInterceptor = byteCodeInstrumentor.newInterceptor(classLoader,
                    protectedDomain,
                    "com.baidu.oped.apm.profiler.modifier.connector.httpclient4.interceptor.AsyncClientExecuteInterceptor");

            String[] executeParams = new String[] {
                    "org.apache.http.HttpHost",
                    "org.apache.http.HttpRequest",
                    "org.apache.http.protocol.HttpContext",
                    "org.apache.http.concurrent.FutureCallback"
                    };
            aClass.addInterceptor("execute", executeParams, executeInterceptor);

            /**
             *
             */
            Interceptor internalExecuteInterceptor = byteCodeInstrumentor.newInterceptor(classLoader,
                    protectedDomain,
                    "com.baidu.oped.apm.profiler.modifier.connector.httpclient4.interceptor.AsyncInternalClientExecuteInterceptor");

            String[] internalExecuteParams = new String[] {
                    "org.apache.http.nio.protocol.HttpAsyncRequestProducer",
                    "org.apache.http.nio.protocol.HttpAsyncResponseConsumer",
                    "org.apache.http.concurrent.FutureCallback"
                    };
            aClass.addInterceptor("execute", internalExecuteParams, internalExecuteInterceptor);

            return aClass.toBytecode();
        } catch (InstrumentException e) {
            logger.info("modify fail. Cause:{}", e.getMessage(), e);
            return null;
        }
    }
}
