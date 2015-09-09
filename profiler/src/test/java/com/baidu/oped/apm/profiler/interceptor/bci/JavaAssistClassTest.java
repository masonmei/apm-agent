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

package com.baidu.oped.apm.profiler.interceptor.bci;

import com.baidu.oped.apm.bootstrap.config.ProfilerConfig;
import com.baidu.oped.apm.bootstrap.context.DatabaseInfo;
import com.baidu.oped.apm.bootstrap.instrument.ByteCodeInstrumentor;
import com.baidu.oped.apm.bootstrap.instrument.InstrumentClass;
import com.baidu.oped.apm.bootstrap.instrument.InstrumentException;
import com.baidu.oped.apm.bootstrap.instrument.MethodInfo;
import com.baidu.oped.apm.bootstrap.interceptor.Interceptor;
import com.baidu.oped.apm.bootstrap.interceptor.tracevalue.BindValueTraceValue;
import com.baidu.oped.apm.bootstrap.interceptor.tracevalue.DatabaseInfoTraceValue;
import com.baidu.oped.apm.bootstrap.interceptor.tracevalue.IntTraceValue;
import com.baidu.oped.apm.bootstrap.interceptor.tracevalue.ObjectTraceValue;
import com.baidu.oped.apm.bootstrap.logging.PLoggerFactory;
import com.baidu.oped.apm.common.ServiceType;
import com.baidu.oped.apm.profiler.DefaultAgent;
import com.baidu.oped.apm.profiler.interceptor.bci.JavaAssistByteCodeInstrumentor;
import com.baidu.oped.apm.profiler.logging.Slf4jLoggerBinder;
import com.baidu.oped.apm.profiler.modifier.db.interceptor.UnKnownDatabaseInfo;
import com.baidu.oped.apm.test.MockAgent;
import com.baidu.oped.apm.test.TestClassLoader;
import com.baidu.oped.apm.test.TestModifier;

import javassist.bytecode.Descriptor;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author emeroad
 */
public class JavaAssistClassTest {
    private Logger logger = LoggerFactory.getLogger(JavaAssistByteCodeInstrumentor.class.getName());

    @Test
    public void testClassHierarchy() throws InstrumentException {

        ByteCodeInstrumentor byteCodeInstrumentor = new JavaAssistByteCodeInstrumentor();

        String testObjectName = "com.baidu.oped.apm.profiler.interceptor.bci.TestObject";

//        final CallLoader loader = null; // systemClassLoader
//        final ClassLoader loader = ClassLoader.getSystemClassLoader();
        InstrumentClass testObject = byteCodeInstrumentor.getClass(null, testObjectName, null);

        Assert.assertEquals(testObject.getName(), testObjectName);

        String testObjectSuperClass = testObject.getSuperClass();
        Assert.assertEquals("java.lang.Object", testObjectSuperClass);

        String[] testObjectSuperClassInterfaces = testObject.getInterfaces();
        Assert.assertEquals(testObjectSuperClassInterfaces.length, 0);

        InstrumentClass classHierarchyObject = byteCodeInstrumentor.getClass(null, "com.baidu.oped.apm.profiler.interceptor.bci.ClassHierarchyObject", null);
        String hierarchySuperClass = classHierarchyObject.getSuperClass();
        Assert.assertEquals("java.util.HashMap", hierarchySuperClass);

        String[] hierarchyInterfaces = classHierarchyObject.getInterfaces();
        Assert.assertEquals(hierarchyInterfaces.length, 2);
        Assert.assertEquals(hierarchyInterfaces[0], "java.lang.Runnable");
        Assert.assertEquals(hierarchyInterfaces[1], "java.lang.Comparable");
    }


    @Test
    public void testDeclaredMethod() throws InstrumentException {

        ByteCodeInstrumentor byteCodeInstrumentor = new JavaAssistByteCodeInstrumentor();

        String testObjectName = "com.baidu.oped.apm.profiler.interceptor.bci.TestObject";

        InstrumentClass testObject = byteCodeInstrumentor.getClass(null, testObjectName, null);

        Assert.assertEquals(testObject.getName(), testObjectName);

        MethodInfo declaredMethod = testObject.getDeclaredMethod("callA", null);
        Assert.assertNotNull(declaredMethod);

    }

    @Test
    public void testDeclaredMethods() throws InstrumentException {

        ByteCodeInstrumentor byteCodeInstrumentor = new JavaAssistByteCodeInstrumentor();

        String testObjectName = "com.baidu.oped.apm.profiler.interceptor.bci.TestObject";

        InstrumentClass testObject = byteCodeInstrumentor.getClass(null, testObjectName, null);
        Assert.assertEquals(testObject.getName(), testObjectName);

        int findMethodCount = 0;
        for (MethodInfo methodInfo : testObject.getDeclaredMethods()) {
            if (!methodInfo.getName().equals("callA")) {
                continue;
            }
            String[] parameterTypes = methodInfo.getParameterTypes();
            if (parameterTypes == null || parameterTypes.length == 0) {
                findMethodCount++;
            }
        }
        Assert.assertEquals(findMethodCount, 1);
    }



    @Test
    public void addTraceValue() throws Exception {
        final TestClassLoader loader = getTestClassLoader();
        final String javassistClassName = "com.baidu.oped.apm.profiler.interceptor.bci.TestObject";
        final TestModifier testModifier = new TestModifier(loader.getInstrumentor(), loader.getAgent()) {

            @Override
            public byte[] modify(ClassLoader classLoader, String className, ProtectionDomain protectedDomain, byte[] classFileBuffer) {
                try {
                    logger.info("modify cl:{}", classLoader);

                    InstrumentClass aClass = byteCodeInstrumentor.getClass(classLoader, javassistClassName, classFileBuffer);

                    Interceptor interceptor = byteCodeInstrumentor.newInterceptor(classLoader, protectedDomain, "com.baidu.oped.apm.profiler.interceptor.TestBeforeInterceptor");
                    addInterceptor(interceptor);
                    aClass.addTraceValue(ObjectTraceValue.class);
                    aClass.addTraceValue(IntTraceValue.class);
                    aClass.addTraceValue(DatabaseInfoTraceValue.class);
                    aClass.addTraceValue(BindValueTraceValue.class);

                    logger.info(interceptor.getClass().getClassLoader().toString());
                    String methodName = "callA";
                    aClass.addInterceptor(methodName, null, interceptor);
                    return aClass.toBytecode();
                } catch (InstrumentException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e.getMessage(), e);
                }
            }
        };
        testModifier.setTargetClass(javassistClassName);
        loader.addModifier(testModifier);
        loader.initialize();

        Class<?> testObjectClazz = loader.loadClass(javassistClassName);
        final String methodName = "callA";
        logger.info("class:{}", testObjectClazz.toString());
        final Object testObject = testObjectClazz.newInstance();
        Method callA = testObjectClazz.getMethod(methodName);
        callA.invoke(testObject);


        if (testObject instanceof ObjectTraceValue) {
            ObjectTraceValue objectTraceValue = (ObjectTraceValue) testObject;
            objectTraceValue.__setTraceObject("a");
            Object get = objectTraceValue.__getTraceObject();
            Assert.assertEquals("a", get);
        } else {
            Assert.fail("ObjectTraceValue implements fail");
        }

        if (testObject instanceof IntTraceValue) {
            IntTraceValue intTraceValue = (IntTraceValue) testObject;
            intTraceValue.__setTraceInt(1);
            int a = intTraceValue.__getTraceInt();
            Assert.assertEquals(1, a);
        } else {
            Assert.fail("IntTraceValue implements fail");
        }

        if (testObject instanceof DatabaseInfoTraceValue) {
            DatabaseInfoTraceValue databaseInfoTraceValue = (DatabaseInfoTraceValue) testObject;
            databaseInfoTraceValue.__setTraceDatabaseInfo(UnKnownDatabaseInfo.INSTANCE);
            DatabaseInfo databaseInfo = databaseInfoTraceValue.__getTraceDatabaseInfo();
            Assert.assertSame(UnKnownDatabaseInfo.INSTANCE, databaseInfo);
        } else {
            Assert.fail("DatabaseInfoTraceValue implements fail");
        }

        if (testObject instanceof BindValueTraceValue) {
            BindValueTraceValue bindValueTraceValue = (BindValueTraceValue) testObject;
            Map<Integer, String> integerStringMap = Collections.emptyMap();
            bindValueTraceValue.__setTraceBindValue(integerStringMap);
            Map<Integer, String> bindValueMap = bindValueTraceValue.__getTraceBindValue();
            Assert.assertSame(integerStringMap, bindValueMap);
        } else {
            Assert.fail("BindValueTraceValue implements fail");
        }

    }

    @Test
    public void testBeforeAddInterceptor() throws Exception {
        final TestClassLoader loader = getTestClassLoader();
        final String javassistClassName = "com.baidu.oped.apm.profiler.interceptor.bci.TestObject";

        final TestModifier testModifier = new TestModifier(loader.getInstrumentor(), loader.getAgent()) {

            @Override
            public byte[] modify(ClassLoader classLoader, String className, ProtectionDomain protectedDomain, byte[] classFileBuffer) {
                try {
                    logger.info("modify cl:{}", classLoader);

                    InstrumentClass aClass = byteCodeInstrumentor.getClass(classLoader, javassistClassName, classFileBuffer);

                    Interceptor interceptor = byteCodeInstrumentor.newInterceptor(classLoader, protectedDomain, "com.baidu.oped.apm.profiler.interceptor.TestBeforeInterceptor");
                    addInterceptor(interceptor);
                    logger.info(interceptor.getClass().getClassLoader().toString());
                    String methodName = "callA";
                    aClass.addInterceptor(methodName, null, interceptor);
                    return aClass.toBytecode();
                } catch (InstrumentException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e.getMessage(), e);
                }
            }
        };
        testModifier.setTargetClass(javassistClassName);
        loader.addModifier(testModifier);
        loader.initialize();



        Class<?> testObjectClazz = loader.loadClass(javassistClassName);
        final String methodName = "callA";
        logger.info("class:{}", testObjectClazz.toString());
        final Object testObject = testObjectClazz.newInstance();
        Method callA = testObjectClazz.getMethod(methodName);
        callA.invoke(testObject);


        Interceptor interceptor = testModifier.getInterceptor(0);
        assertEqualsIntField(interceptor, "call", 1);
        assertEqualsObjectField(interceptor, "className", "com.baidu.oped.apm.profiler.interceptor.bci.TestObject");
        assertEqualsObjectField(interceptor, "methodName", methodName);
        assertEqualsObjectField(interceptor, "args", null);

        assertEqualsObjectField(interceptor, "target", testObject);

    }

    private TestClassLoader getTestClassLoader() {
        PLoggerFactory.initialize(new Slf4jLoggerBinder());


        ProfilerConfig profilerConfig = new ProfilerConfig();
        profilerConfig.setApplicationServerType(ServiceType.TEST_STAND_ALONE);
        DefaultAgent agent = MockAgent.of(profilerConfig);

        return new TestClassLoader(agent);
    }

    public void assertEqualsIntField(Object target, String fieldName, int value) throws NoSuchFieldException, IllegalAccessException {
        Field field = target.getClass().getField(fieldName);
        int anInt = field.getInt(target);
        Assert.assertEquals(anInt, value);
    }

    public void assertEqualsObjectField(Object target, String fieldName, Object value) throws NoSuchFieldException, IllegalAccessException {
        Field field = target.getClass().getField(fieldName);
        Object obj = field.get(target);
        Assert.assertEquals(obj, value);
    }


    @Test
    public void testBeforeAddInterceptorFormContextClassLoader() throws Exception {
        final TestClassLoader loader = getTestClassLoader();
        final String testClassObject = "com.baidu.oped.apm.profiler.interceptor.bci.TestObjectContextClassLoader";
        final TestModifier testModifier = new TestModifier(loader.getInstrumentor(), loader.getAgent()) {

            @Override
            public byte[] modify(ClassLoader classLoader, String className, ProtectionDomain protectedDomain, byte[] classFileBuffer) {
                try {
                    logger.info("modify cl:{}", classLoader);
                    InstrumentClass aClass = byteCodeInstrumentor.getClass(classLoader, testClassObject, classFileBuffer);

                    Interceptor interceptor = byteCodeInstrumentor.newInterceptor(classLoader, protectedDomain, "com.baidu.oped.apm.profiler.interceptor.TestBeforeInterceptor");
                    addInterceptor(interceptor);
                    logger.info(interceptor.getClass().getClassLoader().toString());
                    String methodName = "callA";
                    aClass.addInterceptor(methodName, null, interceptor);
                    return aClass.toBytecode();
                } catch (InstrumentException e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
            }
        };
        testModifier.setTargetClass(testClassObject);
        loader.addModifier(testModifier);
        loader.initialize();



        Class<?> testObjectClazz = loader.loadClass("com.baidu.oped.apm.profiler.interceptor.bci.TestObjectContextClassLoader");
        final String methodName = "callA";
        logger.info("class:{}", testObjectClazz.toString());
        final Object testObject = testObjectClazz.newInstance();
        Method callA = testObjectClazz.getMethod(methodName);
        callA.invoke(testObject);


        final Interceptor interceptor = testModifier.getInterceptor(0);
        assertEqualsIntField(interceptor, "call", 1);
        assertEqualsObjectField(interceptor, "className", "com.baidu.oped.apm.profiler.interceptor.bci.TestObjectContextClassLoader");
        assertEqualsObjectField(interceptor, "methodName", methodName);
        assertEqualsObjectField(interceptor, "args", null);

        assertEqualsObjectField(interceptor, "target", testObject);


    }

    @Test
    public void testAddAfterInterceptor() throws Exception {

        final TestClassLoader loader = getTestClassLoader();
        final String testClassObject = "com.baidu.oped.apm.profiler.interceptor.bci.TestObject2";
        final TestModifier testModifier = new TestModifier(loader.getInstrumentor(), loader.getAgent()) {

            @Override
            public byte[] modify(ClassLoader classLoader, String className, ProtectionDomain protectedDomain, byte[] classFileBuffer) {
                try {
                    logger.info("modify cl:{}", classLoader);
                    InstrumentClass aClass = byteCodeInstrumentor.getClass(classLoader, testClassObject, classFileBuffer);

                    Interceptor interceptor = byteCodeInstrumentor.newInterceptor(classLoader, protectedDomain, "com.baidu.oped.apm.profiler.interceptor.TestAfterInterceptor");
                    addInterceptor(interceptor);
                    logger.info(interceptor.getClass().getClassLoader().toString());
                    String methodName = "callA";
                    aClass.addInterceptor(methodName, null, interceptor);

                    Interceptor interceptor2 = byteCodeInstrumentor.newInterceptor(classLoader, protectedDomain, "com.baidu.oped.apm.profiler.interceptor.TestAfterInterceptor");
                    addInterceptor(interceptor2);
                    String methodName2 = "callB";
                    aClass.addInterceptor(methodName2, null, interceptor2);

                    return aClass.toBytecode();
                } catch (InstrumentException e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
            }

        };
        testModifier.setTargetClass(testClassObject);
        loader.addModifier(testModifier);
        loader.initialize();



        Class<?> testObjectClazz = loader.loadClass(testClassObject);
        final String methodName = "callA";
        logger.info("class:{}", testObjectClazz.toString());
        final Object testObject = testObjectClazz.newInstance();
        Method callA = testObjectClazz.getMethod(methodName);
        Object result = callA.invoke(testObject);


        Interceptor interceptor = testModifier.getInterceptor(0);
        assertEqualsIntField(interceptor, "call", 1);
        assertEqualsObjectField(interceptor, "className", testClassObject);
        assertEqualsObjectField(interceptor, "methodName", methodName);
        assertEqualsObjectField(interceptor, "args", null);

        assertEqualsObjectField(interceptor, "target", testObject);
        assertEqualsObjectField(interceptor, "result", result);


        final String methodName2 = "callB";
        Method callBMethod = testObject.getClass().getMethod(methodName2);
        callBMethod.invoke(testObject);

        Interceptor interceptor2 = testModifier.getInterceptor(1);
        assertEqualsIntField(interceptor2, "call", 1);
        assertEqualsObjectField(interceptor2, "className", testClassObject);
        assertEqualsObjectField(interceptor2, "methodName", methodName2);
        assertEqualsObjectField(interceptor2, "args", null);

        assertEqualsObjectField(interceptor2, "target", testObject);
        assertEqualsObjectField(interceptor2, "result", null);

    }

    @Test
    public void nullDescriptor() {
        String nullDescriptor = Descriptor.ofParameters(null);
        logger.info("Descript null:{}", nullDescriptor);
    }

    @Test
    public void testLog() throws Exception {

        final TestClassLoader loader = getTestClassLoader();
        final String testClassObject = "com.baidu.oped.apm.profiler.interceptor.bci.TestLog";
        final TestModifier testModifier = new TestModifier(loader.getInstrumentor(), loader.getAgent()) {

            @Override
            public byte[] modify(ClassLoader classLoader, String className, ProtectionDomain protectedDomain, byte[] classFileBuffer) {
                try {
                    logger.info("modify cl:{}", classLoader);
                    InstrumentClass aClass = byteCodeInstrumentor.getClass(classLoader, testClassObject, classFileBuffer);

                    aClass.addDebugLogBeforeAfterMethod();
                    aClass.addDebugLogBeforeAfterConstructor();

                    return aClass.toBytecode();
                } catch (InstrumentException e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
            }

        };
        testModifier.setTargetClass(testClassObject);
        loader.addModifier(testModifier);
        loader.initialize();



        Object testObject = loader.loadClass(testClassObject).newInstance();

        Method test = testObject.getClass().getMethod("test");
        test.invoke(testObject);

        Method testString = testObject.getClass().getMethod("test", new Class[]{String.class});
        testString.invoke(testObject, "method");

        Constructor<?> constructor = testObject.getClass().getConstructor();
        Object o = constructor.newInstance();

    }
}
