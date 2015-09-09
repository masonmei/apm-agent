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

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.net.URL;
import java.util.Arrays;

/**
 * @author emeroad
 */
public class JavaAssistTest {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private ClassPool pool;

    @Before
    public void setUp() throws Exception {
        pool = new ClassPool();
        pool.appendSystemPath();
    }

    @Test
    public void newClass() {

    }

    @Test
    public void testAssist() throws NotFoundException, NoSuchMethodException {

        CtClass ctClass = pool.get(String.class.getName());
        logger.info(ctClass.toString());
        String s = "";
//        ctClass.getMethod("valueOf", "(D)");

        CtMethod[] methods = ctClass.getMethods();
//        for (CtMethod method :  methods) {
//            logger.debug("{} {}", method.getMethodInfo(), method.getSignature());
//        }

        CtMethod endsWith = ctClass.getMethod("endsWith", "(Ljava/lang/String;)Z");
        logger.info(endsWith.getMethodInfo().toString());
        logger.info(endsWith.getSignature());
        logger.info(endsWith.getLongName());
        logger.info(endsWith.toString());
        logger.info(endsWith.getName());
        logger.info(endsWith.getMethodInfo().getName());
        logger.info(endsWith.getMethodInfo().getDescriptor());

        Method endsWith1 = String.class.getMethod("endsWith", new Class[]{String.class});
        logger.debug(endsWith1.toString());

    }

    @Test
    public void test() {
        sout("/java/lang/String.class");
        sout("java.lang.String.class");

    }

    private void sout(String str) {
        URL resource = this.getClass().getClassLoader().getResource(str);
        logger.info("{}", resource);
    }

    @Test
    public void genericTest() throws NotFoundException {
        CtClass testClass = pool.get("com.baidu.oped.apm.profiler.javaassist.TestClass");
//        CtMethod setb = testClass.getMethod("setb");
        CtMethod[] declaredMethods = testClass.getDeclaredMethods();
        for (CtMethod declaredMethod : declaredMethods) {
            logger.info(declaredMethod.toString());
            logger.info(declaredMethod.getGenericSignature());
            logger.info(declaredMethod.getSignature());
            logger.info("paramTypes:{}", Arrays.toString(declaredMethod.getParameterTypes()));
            logger.info(declaredMethod.getMethodInfo2().getDescriptor());
            logger.info(declaredMethod.getMethodInfo().getDescriptor());
//            logger.info(declaredMethod.());
        }


        CtMethod setb = testClass.getDeclaredMethod("setA", new CtClass[]{pool.get("int")});
        logger.info(setb.toString());
        CtMethod setStringArray = testClass.getDeclaredMethod("setStringArray", new CtClass[]{pool.get("java.lang.String[]")});
        logger.info(setStringArray.toString());



    }

    @Test
    public void innerClass() throws NotFoundException {
        CtClass testClass = pool.get("com.baidu.oped.apm.profiler.javaassist.TestClass");
        logger.debug(testClass.toString());
        CtClass[] nestedClasses = testClass.getNestedClasses();
        for(CtClass nested : nestedClasses) {
            logger.debug("nestedClass:{}", nested);
        }

        CtClass innerClass = pool.get("com.baidu.oped.apm.profiler.javaassist.TestClass$InnerClass");
        logger.debug("{}", innerClass);

        CtClass class1 = pool.get("com.baidu.oped.apm.profiler.javaassist.TestClass$1");
        logger.debug("{}", class1);
    }
}
