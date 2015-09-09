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

package com.baidu.oped.apm.profiler.modifier.tomcat;

import java.security.ProtectionDomain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baidu.oped.apm.bootstrap.Agent;
import com.baidu.oped.apm.bootstrap.instrument.ByteCodeInstrumentor;
import com.baidu.oped.apm.bootstrap.instrument.InstrumentClass;
import com.baidu.oped.apm.bootstrap.interceptor.Interceptor;
import com.baidu.oped.apm.profiler.modifier.AbstractModifier;

/**
 * @author hyungil.jeong
 */
public class WebappLoaderModifier extends AbstractModifier {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public WebappLoaderModifier(ByteCodeInstrumentor byteCodeInstrumentor, Agent agent) {
        super(byteCodeInstrumentor, agent);
    }

    @Override
    public String getTargetClass() {
        return "org/apache/catalina/loader/WebappLoader";
    }

    @Override
    public byte[] modify(ClassLoader classLoader, String javassistClassName, ProtectionDomain protectedDomain, byte[] classFileBuffer) {
        logger.info("Modifying. {}", javassistClassName);
        
        try {
            InstrumentClass webappLoader = byteCodeInstrumentor.getClass(classLoader, javassistClassName, classFileBuffer);

            Interceptor webappLoaderStartInterceptor = byteCodeInstrumentor.newInterceptor(classLoader, protectedDomain,
                    "com.baidu.oped.apm.profiler.modifier.tomcat.interceptor.WebappLoaderStartInterceptor", null, null);
            
            boolean isHooked = false;
            // Tomcat 6 - org.apache.catalina.loader.WebappLoader.start()
            if (webappLoader.hasDeclaredMethod("start", null)) {
                webappLoader.addInterceptor("start", null, webappLoaderStartInterceptor);
                isHooked = true;
            }
            // Tomcat 7, 8 - org.apache.catalina.loader.WebappLoader.startInternal()
            else if (webappLoader.hasDeclaredMethod("startInternal", null)) {
                webappLoader.addInterceptor("startInternal", null, webappLoaderStartInterceptor);
                isHooked = true;
            }

            if (isHooked) {
                logger.info("{} class is converted.", javassistClassName);
            } else {
                logger.warn("{} class not converted - start() or startInternal() method not found.", javassistClassName);
            }
            return webappLoader.toBytecode();
        } catch (Exception e) {
            if (logger.isWarnEnabled()) {
                logger.warn("modify failed. Cause:" + e.getMessage(), e);
            }
        }
        return null;
    }
}
