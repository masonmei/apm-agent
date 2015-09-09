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

package com.baidu.oped.apm.profiler.plugin;

import java.security.ProtectionDomain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baidu.oped.apm.bootstrap.Agent;
import com.baidu.oped.apm.bootstrap.instrument.ByteCodeInstrumentor;
import com.baidu.oped.apm.bootstrap.instrument.InstrumentClass;
import com.baidu.oped.apm.bootstrap.plugin.DedicatedClassEditor;
import com.baidu.oped.apm.bootstrap.plugin.PluginClassLoaderFactory;
import com.baidu.oped.apm.exception.PinpointException;
import com.baidu.oped.apm.profiler.modifier.AbstractModifier;

public class ClassEditorAdaptor extends AbstractModifier {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final DedicatedClassEditor editor;
    private final PluginClassLoaderFactory classLoaderFactory;

    
    public ClassEditorAdaptor(ByteCodeInstrumentor byteCodeInstrumentor, Agent agent, DedicatedClassEditor editor, PluginClassLoaderFactory classLoaderFactory) {
        super(byteCodeInstrumentor, agent);
        this.editor = editor;
        this.classLoaderFactory = classLoaderFactory;
    }

    @Override
    public byte[] modify(ClassLoader classLoader, String className, ProtectionDomain protectionDomain, byte[] classFileBuffer) {
        logger.debug("Editing class {}", className);
        
        ClassLoader forPlugin = classLoaderFactory.get(classLoader);
        
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(forPlugin);
        
        try {
            InstrumentClass target = byteCodeInstrumentor.getClass(classLoader, className, classFileBuffer);
            return editor.edit(classLoader, target);
        } catch (PinpointException e) {
            throw e;
        } catch (Exception e) {
            String msg = "Fail to invoke plugin class editor " + editor.getClass().getName() + " for " + editor.getTargetClassName();
            logger.warn(msg, e);
            throw new PinpointException(msg, e);
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }
        
    }

    @Override
    public String getTargetClass() {
        return editor.getTargetClassName().replace('.', '/');
    }
}
