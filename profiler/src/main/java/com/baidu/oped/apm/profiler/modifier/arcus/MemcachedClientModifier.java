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
import java.util.List;

import com.baidu.oped.apm.bootstrap.Agent;
import com.baidu.oped.apm.bootstrap.instrument.ByteCodeInstrumentor;
import com.baidu.oped.apm.bootstrap.instrument.InstrumentClass;
import com.baidu.oped.apm.bootstrap.instrument.MethodInfo;
import com.baidu.oped.apm.bootstrap.instrument.Type;
import com.baidu.oped.apm.bootstrap.interceptor.Interceptor;
import com.baidu.oped.apm.bootstrap.interceptor.ParameterExtractorSupport;
import com.baidu.oped.apm.bootstrap.interceptor.SimpleAroundInterceptor;
import com.baidu.oped.apm.bootstrap.interceptor.tracevalue.ObjectTraceValue2;
import com.baidu.oped.apm.profiler.modifier.AbstractModifier;
import com.baidu.oped.apm.profiler.modifier.arcus.interceptor.ArcusScope;
import com.baidu.oped.apm.profiler.modifier.arcus.interceptor.IndexParameterExtractor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author netspider
 * @author emeroad
 */
public class MemcachedClientModifier extends AbstractModifier {

    private final Logger logger = LoggerFactory.getLogger(MemcachedClientModifier.class.getName());

    public MemcachedClientModifier(ByteCodeInstrumentor byteCodeInstrumentor, Agent agent) {
        super(byteCodeInstrumentor, agent);
    }

    public String getTargetClass() {
        return "net/spy/memcached/MemcachedClient";
    }

    public byte[] modify(ClassLoader classLoader, String javassistClassName,
            ProtectionDomain protectedDomain, byte[] classFileBuffer) {
        if (logger.isInfoEnabled()) {
            logger.info("Modifing. {}", javassistClassName);
        }

        try {
            InstrumentClass aClass = byteCodeInstrumentor.getClass(classLoader, javassistClassName, classFileBuffer);

            String[] args = {"java.lang.String", "net.spy.memcached.ops.Operation"};
            if (!checkCompatibility(aClass, args)) {
                return null;
            }
//            serviceCode->ObjectTraceValue2
            aClass.addTraceValue(ObjectTraceValue2.class);

            Interceptor addOpInterceptor = byteCodeInstrumentor.newInterceptor(classLoader, protectedDomain,
                    "com.baidu.oped.apm.profiler.modifier.arcus.interceptor.AddOpInterceptor");
            aClass.addInterceptor("addOp", args, addOpInterceptor, Type.before);

            // Inject ApiInterceptor to all public methods.
            final List<MethodInfo> declaredMethods = aClass.getDeclaredMethods(new MemcachedMethodFilter());

            for (MethodInfo method : declaredMethods) {
                SimpleAroundInterceptor apiInterceptor = (SimpleAroundInterceptor) byteCodeInstrumentor.newInterceptor(classLoader, protectedDomain, "com.baidu.oped.apm.profiler.modifier.arcus.interceptor.ApiInterceptor");
                if (agent.getProfilerConfig().isMemcachedKeyTrace()) {
                    final int index = ParameterUtils.findFirstString(method, 3);
                    if (index != -1) {
                        ((ParameterExtractorSupport)apiInterceptor).setParameterExtractor(new IndexParameterExtractor(index));
                    }
                }
                aClass.addScopeInterceptor(method.getName(), method.getParameterTypes(), apiInterceptor, ArcusScope.SCOPE);
            }
            return aClass.toBytecode();
        } catch (Exception e) {
            if (logger.isWarnEnabled()) {
                logger.warn(e.getMessage(), e);
            }
            return null;
        }
    }

    private boolean checkCompatibility(InstrumentClass aClass, String[] args) {
        // if addOp exists, compatibility is okay for now.
        final boolean addOp = aClass.hasDeclaredMethod("addOp", args);
        if (!addOp) {
            logger.warn("addOp() not found. skip MemcachedClientModifier");
         }
        return addOp;
    }

}