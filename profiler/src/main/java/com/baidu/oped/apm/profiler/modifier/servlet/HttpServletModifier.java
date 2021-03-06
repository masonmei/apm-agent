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

package com.baidu.oped.apm.profiler.modifier.servlet;

import java.security.ProtectionDomain;

import com.baidu.oped.apm.bootstrap.Agent;
import com.baidu.oped.apm.bootstrap.instrument.ByteCodeInstrumentor;
import com.baidu.oped.apm.bootstrap.instrument.InstrumentClass;
import com.baidu.oped.apm.bootstrap.instrument.InstrumentException;
import com.baidu.oped.apm.bootstrap.interceptor.Interceptor;
import com.baidu.oped.apm.profiler.modifier.AbstractModifier;
import com.baidu.oped.apm.profiler.modifier.method.interceptor.MethodInterceptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author netspider
 * 
 */
public class HttpServletModifier extends AbstractModifier {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public HttpServletModifier(ByteCodeInstrumentor byteCodeInstrumentor, Agent agent) {
        super(byteCodeInstrumentor, agent);
    }

    public String getTargetClass() {
        return "javax/servlet/http/HttpServlet";
    }

    public byte[] modify(ClassLoader classLoader, String javassistClassName, ProtectionDomain protectedDomain, byte[] classFileBuffer) {
        if (logger.isInfoEnabled()) {
            logger.info("Modifing. {}", javassistClassName);
        }


        try {
            InstrumentClass servlet = byteCodeInstrumentor.getClass(classLoader, javassistClassName, classFileBuffer);
            Interceptor doGetInterceptor = new MethodInterceptor();
            servlet.addInterceptor("doGet", new String[] { "javax.servlet.http.HttpServletRequest", "javax.servlet.http.HttpServletResponse" }, doGetInterceptor);

            Interceptor doPostInterceptor = new MethodInterceptor();
            servlet.addInterceptor("doPost", new String[] { "javax.servlet.http.HttpServletRequest", "javax.servlet.http.HttpServletResponse" }, doPostInterceptor);

            return servlet.toBytecode();
        } catch (InstrumentException e) {
            logger.info("modify fail. Cause:{}", e.getMessage(), e);
            return null;
        }
    }
}