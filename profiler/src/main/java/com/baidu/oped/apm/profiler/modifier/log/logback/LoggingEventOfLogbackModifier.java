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
package com.baidu.oped.apm.profiler.modifier.log.logback;

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
 * This modifier support slf4j 1.4.1 version and logback 0.9.8 version, or greater.
 * Because package name of MDC class is different on under those version 
 * and under those version is too old.
 * By the way slf4j 1.4.0 version release on May 2007.
 * Refer to url http://mvnrepository.com/artifact/org.slf4j/slf4j-api for detail.
 * 
 * @author minwoo.jung
 */
public class LoggingEventOfLogbackModifier extends AbstractModifier {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public LoggingEventOfLogbackModifier(ByteCodeInstrumentor byteCodeInstrumentor, Agent agent) {
        super(byteCodeInstrumentor, agent);
    }

    
    @Override
    public byte[] modify(ClassLoader classLoader, String javassistClassName, ProtectionDomain protectedDomain, byte[] classFileBuffer) {
        if (logger.isInfoEnabled()) {
            logger.info("Modifing. {}", javassistClassName);
        }
        
        try {
            InstrumentClass mdcClass = byteCodeInstrumentor.getClass(classLoader, "org.slf4j.MDC", classFileBuffer);
            
            if (!mdcClass.hasMethod("put", new String[]{"java.lang.String", "java.lang.String"}, "void")) {
                logger.warn("modify fail. Because put method does not existed org.slf4j.MDC class.");
                return null;
            }
            if (!mdcClass.hasMethod("remove", new String[]{"java.lang.String"}, "void")) {
                logger.warn("modify fail. Because remove method does not existed org.slf4j.MDC class.");
                return null;
            }
        } catch (InstrumentException e) {
            logger.warn("modify fail. Because org.slf4j.MDC does not existed. Cause:" + e.getMessage(), e);
            return null;
        }
        
        try {
            InstrumentClass loggingEvent = byteCodeInstrumentor.getClass(classLoader, javassistClassName, classFileBuffer);
            
            Interceptor interceptor1 = byteCodeInstrumentor.newInterceptor(classLoader, protectedDomain, "com.baidu.oped.apm.profiler.modifier.log.logback.interceptor.LoggingEventOfLogbackInterceptor");
            loggingEvent.addConstructorInterceptor(new String[]{"java.lang.String", "ch.qos.logback.classic.Logger", "ch.qos.logback.classic.Level", "java.lang.String", "java.lang.Throwable", "java.lang.Object[]"}, interceptor1);

            Interceptor interceptor2 = byteCodeInstrumentor.newInterceptor(classLoader, protectedDomain, "com.baidu.oped.apm.profiler.modifier.log.logback.interceptor.LoggingEventOfLogbackInterceptor");
            loggingEvent.addConstructorInterceptor(new String[]{}, interceptor2);
            
            return loggingEvent.toBytecode();
        } catch (InstrumentException e) {
            logger.warn("modify fail. Cause:" + e.getMessage(), e);
            return null;
        }
    }

    @Override
    public String getTargetClass() {
        return "ch/qos/logback/classic/spi/LoggingEvent";
    }

}
