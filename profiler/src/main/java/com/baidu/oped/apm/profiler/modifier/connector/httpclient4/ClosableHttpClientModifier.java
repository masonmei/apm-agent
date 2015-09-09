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
import com.baidu.oped.apm.bootstrap.instrument.DefaultScopeDefinition;
import com.baidu.oped.apm.bootstrap.instrument.InstrumentClass;
import com.baidu.oped.apm.bootstrap.instrument.InstrumentException;
import com.baidu.oped.apm.bootstrap.instrument.Scope;
import com.baidu.oped.apm.bootstrap.instrument.ScopeDefinition;
import com.baidu.oped.apm.bootstrap.interceptor.Interceptor;
import com.baidu.oped.apm.profiler.modifier.AbstractModifier;
import com.baidu.oped.apm.profiler.modifier.connector.httpclient4.interceptor.HttpClient4Scope;

/**
 * Apache httpclient 4.3 CloseableHttpClient modifier
 * 
 * <p/>
 * <p/>
 * <pre>
 * http://grepcode.com/file/repo1.maven.org/maven2/org.apache.httpcomponents/httpclient/4.3/org/apache/http/impl/client/CloseableHttpClient.java?av=h#CloseableHttpClient
 * </pre>
 *
 * @author netspider
 * @author minwoo.jung
 */
public class ClosableHttpClientModifier extends AbstractModifier {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public ClosableHttpClientModifier(ByteCodeInstrumentor byteCodeInstrumentor, Agent agent) {
        super(byteCodeInstrumentor, agent);
    }

    public String getTargetClass() {
        return "org/apache/http/impl/client/CloseableHttpClient";
    }

    public byte[] modify(ClassLoader classLoader, String javassistClassName, ProtectionDomain protectedDomain, byte[] classFileBuffer) {
        if (logger.isInfoEnabled()) {
            logger.info("Modifing. {}", javassistClassName);
        }

        try {
            InstrumentClass aClass = byteCodeInstrumentor.getClass(classLoader, javassistClassName, classFileBuffer);
            ScopeDefinition scopeDefinition = new DefaultScopeDefinition(HttpClient4Scope.SCOPE, ScopeDefinition.Type.ATTACHMENT);
            Scope scope = byteCodeInstrumentor.getScope(scopeDefinition);

            addHttpRequestApi(classLoader, protectedDomain, aClass, scope);

            addHttpUriRequestApi(classLoader, protectedDomain, aClass, scope);

            return aClass.toBytecode();
        } catch (Throwable e) {
            logger.warn("httpClient4 modifier error. Caused:{}", e.getMessage(), e);
            return null;
        }
    }
    
    private void addHttpRequestApi(ClassLoader classLoader, ProtectionDomain protectedDomain, InstrumentClass aClass, Scope scope) throws InstrumentException {
        Interceptor httpRequestApi1 = newHttpRequestInterceptor(classLoader, protectedDomain ,false, scope);
        aClass.addInterceptor("execute", new String[]{"org.apache.http.HttpHost", "org.apache.http.HttpRequest"}, httpRequestApi1);

        Interceptor httpRequestApi2 = newHttpRequestInterceptor(classLoader, protectedDomain, false, scope);
        aClass.addInterceptor("execute", new String[]{"org.apache.http.HttpHost", "org.apache.http.HttpRequest", "org.apache.http.protocol.HttpContext"}, httpRequestApi2);

        Interceptor httpRequestApi3 = newHttpRequestInterceptor(classLoader, protectedDomain, true, scope);
        aClass.addInterceptor("execute", new String[]{"org.apache.http.HttpHost", "org.apache.http.HttpRequest", "org.apache.http.client.ResponseHandler"}, httpRequestApi3);

        Interceptor httpRequestApi4 = newHttpRequestInterceptor(classLoader, protectedDomain, true, scope);
        aClass.addInterceptor("execute", new String[]{"org.apache.http.HttpHost", "org.apache.http.HttpRequest", "org.apache.http.client.ResponseHandler", "org.apache.http.protocol.HttpContext"}, httpRequestApi4);
    }

    private Interceptor newHttpRequestInterceptor(ClassLoader classLoader, ProtectionDomain protectedDomain, boolean isHasCallbackParam, Scope scope) throws InstrumentException {
        return byteCodeInstrumentor.newInterceptor(classLoader, protectedDomain, "com.baidu.oped.apm.profiler.modifier.connector.httpclient4.interceptor.HttpRequestExecuteInterceptor", new Object[] {isHasCallbackParam, scope}, new Class[] {boolean.class, Scope.class});
    }
    
    private void addHttpUriRequestApi(ClassLoader classLoader, ProtectionDomain protectedDomain, InstrumentClass aClass, Scope scope) throws InstrumentException {
        Interceptor httpUriRequestInterceptor1 = newHttpUriRequestInterceptor(classLoader, protectedDomain, false, scope);
        aClass.addInterceptor("execute", new String[]{"org.apache.http.client.methods.HttpUriRequest"}, httpUriRequestInterceptor1);

        Interceptor httpUriRequestInterceptor2 = newHttpUriRequestInterceptor(classLoader, protectedDomain, false, scope);
        aClass.addInterceptor("execute", new String[]{"org.apache.http.client.methods.HttpUriRequest", "org.apache.http.protocol.HttpContext"}, httpUriRequestInterceptor2);

        Interceptor httpUriRequestInterceptor3 = newHttpUriRequestInterceptor(classLoader, protectedDomain, true, scope);
        aClass.addInterceptor("execute", new String[]{"org.apache.http.client.methods.HttpUriRequest", "org.apache.http.client.ResponseHandler"}, httpUriRequestInterceptor3);

        Interceptor httpUriRequestInterceptor4 = newHttpUriRequestInterceptor(classLoader, protectedDomain, true, scope);
        aClass.addInterceptor("execute", new String[]{"org.apache.http.client.methods.HttpUriRequest", "org.apache.http.client.ResponseHandler", "org.apache.http.protocol.HttpContext"}, httpUriRequestInterceptor4);
    }

    private Interceptor newHttpUriRequestInterceptor(ClassLoader classLoader, ProtectionDomain protectedDomain, boolean isHasCallbackParam, Scope scope) throws InstrumentException {
        return byteCodeInstrumentor.newInterceptor(classLoader, protectedDomain, "com.baidu.oped.apm.profiler.modifier.connector.httpclient4.interceptor.HttpUriRequestExecuteInterceptor", new Object[] {isHasCallbackParam, scope}, new Class[] {boolean.class, Scope.class});
    }
}