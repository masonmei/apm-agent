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

package com.baidu.oped.apm.bootstrap.plugin;

import com.baidu.oped.apm.bootstrap.instrument.InstrumentClass;
import com.baidu.oped.apm.bootstrap.instrument.InstrumentException;
import com.baidu.oped.apm.bootstrap.instrument.MethodFilter;
import com.baidu.oped.apm.bootstrap.instrument.MethodInfo;
import com.baidu.oped.apm.bootstrap.interceptor.Interceptor;

public class FilteringInterceptorInjector implements InterceptorInjector {
    private final MethodFilter filter;
    private final InterceptorFactory factory;
    private final boolean singletonInterceptor;
    
    public FilteringInterceptorInjector(MethodFilter filter, InterceptorFactory factory, boolean singletonInterceptor) {
        this.filter = filter;
        this.factory = factory;
        this.singletonInterceptor = singletonInterceptor;
    }
    
    @Override
    public void inject(ClassLoader classLoader, InstrumentClass target) throws InstrumentException {
        int interceptorId = -1;
        
        for (MethodInfo methodInfo : target.getDeclaredMethods(filter)) {
            String targetMethodName = methodInfo.getName();
            String[] targetParameterTypes = methodInfo.getParameterTypes();
            
            if (singletonInterceptor && interceptorId != -1) {
                target.reuseInterceptor(targetMethodName, targetParameterTypes, interceptorId);
            } else {
                Interceptor interceptor = factory.getInterceptor(classLoader, target, methodInfo);
                interceptorId = target.addInterceptor(targetMethodName, targetParameterTypes, interceptor);
            }
        }
    }
}
