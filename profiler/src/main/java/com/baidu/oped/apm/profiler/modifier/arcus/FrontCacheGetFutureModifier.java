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

import com.baidu.oped.apm.bootstrap.Agent;
import com.baidu.oped.apm.bootstrap.instrument.ByteCodeInstrumentor;
import com.baidu.oped.apm.bootstrap.instrument.InstrumentClass;
import com.baidu.oped.apm.bootstrap.interceptor.Interceptor;
import com.baidu.oped.apm.bootstrap.interceptor.SimpleAroundInterceptor;
import com.baidu.oped.apm.bootstrap.interceptor.tracevalue.ObjectTraceValue3;
import com.baidu.oped.apm.bootstrap.interceptor.tracevalue.ObjectTraceValue4;
import com.baidu.oped.apm.profiler.modifier.AbstractModifier;
import com.baidu.oped.apm.profiler.modifier.arcus.interceptor.ArcusScope;
import com.baidu.oped.apm.profiler.modifier.arcus.interceptor.FrontCacheGetFutureConstructInterceptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.ProtectionDomain;

/**
 * @author harebox
 */
public class FrontCacheGetFutureModifier extends AbstractModifier {

    protected Logger logger;

    public FrontCacheGetFutureModifier(ByteCodeInstrumentor byteCodeInstrumentor, Agent agent) {
        super(byteCodeInstrumentor, agent);
        this.logger = LoggerFactory.getLogger(this.getClass());
    }

    public byte[] modify(ClassLoader classLoader, String javassistClassName, ProtectionDomain protectedDomain, byte[] classFileBuffer) {
        if (logger.isInfoEnabled()) {
            logger.info("Modifying. {}", javassistClassName);
        }

        try {
            InstrumentClass aClass = byteCodeInstrumentor.getClass(classLoader, javassistClassName, classFileBuffer);

//            cacheName->ObjectTraceValue3
            aClass.addTraceValue(ObjectTraceValue3.class);

//            cacheKey->ObjectTraceValue4
            aClass.addTraceValue(ObjectTraceValue4.class);

            Interceptor frontCacheGetFutureConstructInterceptor = new FrontCacheGetFutureConstructInterceptor();
            aClass.addConstructorInterceptor(new String[]{"net.sf.ehcache.Element"}, frontCacheGetFutureConstructInterceptor);

            SimpleAroundInterceptor frontCacheGetFutureGetInterceptor = (SimpleAroundInterceptor) byteCodeInstrumentor.newInterceptor(classLoader, protectedDomain, "com.baidu.oped.apm.profiler.modifier.arcus.interceptor.FrontCacheGetFutureGetInterceptor");
            aClass.addScopeInterceptor("get", new String[]{Long.TYPE.toString(), "java.util.concurrent.TimeUnit"}, frontCacheGetFutureGetInterceptor, ArcusScope.SCOPE);
            aClass.addScopeInterceptor("get", new String[]{}, frontCacheGetFutureGetInterceptor, ArcusScope.SCOPE);

            return aClass.toBytecode();
        } catch (Exception e) {
            if (logger.isWarnEnabled()) {
                logger.warn(e.getMessage(), e);
            }
            return null;
        }
    }

    @Override
    public String getTargetClass() {
        return "net/spy/memcached/plugin/FrontCacheGetFuture";
    }
}
