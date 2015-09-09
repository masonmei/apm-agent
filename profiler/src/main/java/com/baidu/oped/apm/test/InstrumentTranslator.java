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

import com.baidu.oped.apm.profiler.DefaultAgent;
import com.baidu.oped.apm.profiler.modifier.AbstractModifier;

import javassist.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author emeroad
 */
public class InstrumentTranslator implements Translator {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final DefaultAgent agent;

    private ConcurrentMap<String, AbstractModifier> modifierMap = new ConcurrentHashMap<String, AbstractModifier>();

    private ClassLoader loader;

    public InstrumentTranslator(ClassLoader loader, DefaultAgent agent) {
        this.loader = loader;
        this.agent = agent;
    }

    public AbstractModifier addModifier(AbstractModifier modifier) {
        return modifierMap.put(modifier.getTargetClass().replace('/', '.'), modifier);
    }

    @Override
    public void start(ClassPool pool) throws NotFoundException, CannotCompileException {
//        this.pool = pool;
    }

    @Override
    public void onLoad(ClassPool pool, String classname) throws NotFoundException, CannotCompileException {
        logger.debug("loading className:{}", classname);

        try {
            // Find Modifier from agent and try transforming
            String replace = classname.replace('.', '/');
            ClassFileTransformer classFileTransformer = agent.getClassFileTransformer();
            byte[] transform = classFileTransformer.transform(this.loader, replace, null, null, null);
            if (transform != null) {
                pool.makeClass(new ByteArrayInputStream(transform));
                return;
            }
        } catch (IOException ex) {
            throw new NotFoundException(classname + " not found. Caused:" + ex.getMessage(), ex);
        } catch (IllegalClassFormatException ex) {
            throw new RuntimeException(classname + " not found. Caused:" + ex.getMessage(), ex);
        }
        
         // find from modifierMap
        findModifierMap(pool, classname);


    }
    private void findModifierMap(ClassPool pool, String classname) throws NotFoundException, CannotCompileException {
        AbstractModifier modifier = modifierMap.get(classname);
        if (modifier == null) {
            return;
        }
        logger.info("Modify loader:{}, name:{},  modifier{}", loader, classname, modifier);

        final Thread thread = Thread.currentThread();
        final ClassLoader beforeClassLoader = thread.getContextClassLoader();
        thread.setContextClassLoader(loader);
        try {
            byte[] modify = modifier.modify(this.loader, classname, null, null);
            pool.makeClass(new ByteArrayInputStream(modify));
        } catch (IOException ex) {
            throw new NotFoundException(classname + " not found. Caused:" + ex.getMessage(), ex);
        } finally {
            thread.setContextClassLoader(beforeClassLoader);
        }
    }
}
