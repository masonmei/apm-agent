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

package com.baidu.oped.apm.profiler.javaassist;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.ProtectionDomain;

/**
 * @author emeroad
 */
public class TestBootstrapClass {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Test
    public void test() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, IOException, CannotCompileException {

        Class cl = Class.forName("java.lang.ClassLoader");

        Method defineClass1 = cl.getDeclaredMethod("defineClass", new Class[]{String.class, byte[].class, int.class, int.class});

        Method defineClass2 = cl.getDeclaredMethod("defineClass", new Class[]{String.class, byte[].class, int.class, int.class, ProtectionDomain.class});

        ClassPool cp = new ClassPool();
        cp.appendSystemPath();
        CtClass ctClass = cp.makeClass("com.test.Test");
        byte[] bytes = ctClass.toBytecode();


        ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
        logger.info(systemClassLoader.getClass().getName());

        defineClass(defineClass1, systemClassLoader, new Object[]{"com.test.Test", bytes, 0, bytes.length});
        Class<?> aClass = systemClassLoader.loadClass("com.test.Test");

        logger.info("{}", aClass.getClass().getClassLoader());


    }

    private Object defineClass(Method method, ClassLoader loader, Object[] objects) throws InvocationTargetException, IllegalAccessException {
        method.setAccessible(true);
        try {
            return method.invoke(loader, objects);
        } finally {
            method.setAccessible(false);
        }
    }


    @Test
    public void testJdkClassClassLoader() throws IOException {
        URL url = new URL("http://www.nave.com");


        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

        logger.info(urlConnection.toString());
        logger.info("{}", urlConnection.getClass().getClassLoader());
        ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
        ClassLoader parent = systemClassLoader.getParent();
        logger.info("parent:{}", parent);
        logger.info("pparent:{}", parent.getParent());

        logger.info("{}", String.class.getClassLoader());
        logger.info("{}", TestBootstrapClass.class.getClassLoader());


        urlConnection.disconnect();


    }

    @Test
    public void testReflection() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        java.lang.ClassLoader contextClassLoader = java.lang.Thread.currentThread().getContextClassLoader();
        java.lang.Class<?> interceptorRegistry = contextClassLoader.loadClass("com.baidu.oped.apm.bootstrap.interceptor.InterceptorRegistry");
        java.lang.reflect.Method getInterceptorMethod = interceptorRegistry.getMethod("getInterceptor", new java.lang.Class[]{int.class});
        java.lang.Object interceptor = getInterceptorMethod.invoke(interceptorRegistry, Integer.valueOf(1));


        java.lang.reflect.Method beforeMethod = interceptor.getClass().getMethod("before", java.lang.Object.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Object[].class);
        beforeMethod.invoke(interceptor, null, null, null, null, null);

    }

}

