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

package com.baidu.oped.apm.test;

import com.baidu.oped.apm.bootstrap.Agent;
import com.baidu.oped.apm.bootstrap.instrument.ByteCodeInstrumentor;
import com.baidu.oped.apm.bootstrap.interceptor.Interceptor;
import com.baidu.oped.apm.profiler.modifier.AbstractModifier;

import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;

/**
 * @author emeroad
 */
public abstract class TestModifier extends AbstractModifier {

    private String targetClass;

    public final List<Interceptor> interceptorList = new ArrayList<Interceptor>();

    public TestModifier(ByteCodeInstrumentor byteCodeInstrumentor, Agent agent) {
        super(byteCodeInstrumentor, agent);
    }


    public void setTargetClass(String targetClass) {
        this.targetClass = targetClass;
    }

    @Override
    public String getTargetClass() {
        return targetClass;
    }

    public void addInterceptor(Interceptor interceptor) {
        this.interceptorList.add(interceptor);
    }

    public List<Interceptor> getInterceptorList() {
        return interceptorList;
    }

    public Interceptor getInterceptor(int index) {
        return interceptorList.get(index);
    }




    @Override
    public abstract byte[] modify(ClassLoader classLoader, String className, ProtectionDomain protectedDomain, byte[] classFileBuffer);


}
