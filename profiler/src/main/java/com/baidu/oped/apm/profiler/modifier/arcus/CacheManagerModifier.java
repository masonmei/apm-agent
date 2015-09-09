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

package com.baidu.oped.apm.profiler.modifier.arcus;

import java.security.ProtectionDomain;

import com.baidu.oped.apm.bootstrap.Agent;
import com.baidu.oped.apm.bootstrap.instrument.ByteCodeInstrumentor;
import com.baidu.oped.apm.bootstrap.instrument.InstrumentClass;
import com.baidu.oped.apm.bootstrap.interceptor.tracevalue.ObjectTraceValue2;
import com.baidu.oped.apm.profiler.modifier.AbstractModifier;
import com.baidu.oped.apm.profiler.modifier.arcus.interceptor.CacheManagerConstructInterceptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author netspider
 */
public class CacheManagerModifier extends AbstractModifier {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public CacheManagerModifier(ByteCodeInstrumentor byteCodeInstrumentor, Agent agent) {
        super(byteCodeInstrumentor, agent);
    }

    public String getTargetClass() {
        return "net/spy/memcached/CacheManager";
    }

    public byte[] modify(ClassLoader classLoader, String javassistClassName, ProtectionDomain protectedDomain, byte[] classFileBuffer) {
        if (logger.isInfoEnabled()) {
            logger.info("Modifing. {}", javassistClassName);
        }

        try {
            InstrumentClass aClass = byteCodeInstrumentor.getClass(classLoader, javassistClassName, classFileBuffer);

//            serviceCode->ObjectTraceValue2
            aClass.addTraceValue(ObjectTraceValue2.class);

            aClass.addConstructorInterceptor(new String[]{"java.lang.String", "java.lang.String", "net.spy.memcached.ConnectionFactoryBuilder", "java.util.concurrent.CountDownLatch", "int", "int"}, new CacheManagerConstructInterceptor());

            return aClass.toBytecode();
        } catch (Exception e) {
            if (logger.isWarnEnabled()) {
                logger.warn(e.getMessage(), e);
            }
            return null;
        }
    }
}