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

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.Map;

import org.junit.Test;

import com.baidu.oped.apm.bootstrap.context.TraceContext;
import com.baidu.oped.apm.bootstrap.instrument.ByteCodeInstrumentor;
import com.baidu.oped.apm.bootstrap.instrument.InstrumentClass;
import com.baidu.oped.apm.bootstrap.instrument.MethodInfo;
import com.baidu.oped.apm.bootstrap.instrument.Scope;
import com.baidu.oped.apm.bootstrap.interceptor.Interceptor;
import com.baidu.oped.apm.bootstrap.interceptor.tracevalue.TraceValue;
import com.baidu.oped.apm.bootstrap.plugin.ClassEditor;
import com.baidu.oped.apm.bootstrap.plugin.ClassEditorBuilder;
import com.baidu.oped.apm.bootstrap.plugin.ProfilerPluginContext;
import com.baidu.oped.apm.bootstrap.plugin.TypeUtils;
import com.baidu.oped.apm.bootstrap.plugin.ClassEditorBuilder.InterceptorBuilder;
import com.baidu.oped.apm.bootstrap.plugin.ClassEditorBuilder.MetadataBuilder;

public class ClassEditorBuilderTest {

    @Test
    public void test() throws Exception {
        ByteCodeInstrumentor instrumentor = mock(ByteCodeInstrumentor.class);
        TraceContext traceContext = mock(TraceContext.class);
        InstrumentClass aClass = mock(InstrumentClass.class);
        MethodInfo aMethod = mock(MethodInfo.class);
        Scope aScope = mock(Scope.class);
        
        ClassLoader classLoader = getClass().getClassLoader();
        String methodName = "someMethod";
        Class<?>[] parameterTypes = new Class<?>[] { String.class };
        String[] parameterTypeNames = TypeUtils.toClassNames(parameterTypes);
        String scopeName = "test";
        
        when(instrumentor.getScope(scopeName)).thenReturn(aScope);
        when(aClass.getDeclaredMethod(methodName, parameterTypeNames)).thenReturn(aMethod);
        when(aMethod.getName()).thenReturn(methodName);
        when(aMethod.getParameterTypes()).thenReturn(parameterTypeNames);
        when(aClass.addInterceptor(eq(methodName), eq(parameterTypeNames), isA(Interceptor.class))).thenReturn(0);
        
        
        ProfilerPluginContext helper = new ProfilerPluginContext(instrumentor, traceContext);
        ClassEditorBuilder builder = helper.newClassEditorBuilder();
        MetadataBuilder mb = builder.newMetadataBuilder();
        mb.inject("com.baidu.oped.apm.bootstrap.plugin.ClassEditorBuilderTest$TestMetadata");
        mb.initializeWithDefaultConstructorOf("java.util.HashMap");
        
        InterceptorBuilder ib = builder.newInterceptorBuilder();
        ib.intercept(methodName, parameterTypeNames);
        ib.with("com.baidu.oped.apm.bootstrap.plugin.TestInterceptor");
        ib.constructedWith("provided");
        ib.in(scopeName);
        
        ClassEditor editor = builder.build();
        
        editor.edit(classLoader, aClass);
        
        verify(aClass).addInterceptor(eq(methodName), isA(String[].class), isA(Interceptor.class));
        verify(aClass).addTraceValue(TestMetadata.class, "new java.util.HashMap();");
    }
    
    public static interface TestMetadata extends TraceValue {
        public Map<String, Object> getMap();
        public void setMap(Map<String, Object> map);
    }
}
