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

package com.baidu.oped.apm.profiler;

import java.io.File;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.List;

import com.baidu.oped.apm.profiler.util.JavaAssistUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baidu.oped.apm.bootstrap.config.ProfilerConfig;
import com.baidu.oped.apm.bootstrap.instrument.ByteCodeInstrumentor;
import com.baidu.oped.apm.bootstrap.plugin.ClassEditor;
import com.baidu.oped.apm.bootstrap.plugin.DedicatedClassEditor;
import com.baidu.oped.apm.bootstrap.plugin.PluginClassLoaderFactory;
import com.baidu.oped.apm.bootstrap.plugin.ProfilerPlugin;
import com.baidu.oped.apm.bootstrap.plugin.ProfilerPluginContext;
import com.baidu.oped.apm.profiler.modifier.AbstractModifier;
import com.baidu.oped.apm.profiler.modifier.DefaultModifierRegistry;
import com.baidu.oped.apm.profiler.modifier.Modifier;
import com.baidu.oped.apm.profiler.modifier.ModifierProvider;
import com.baidu.oped.apm.profiler.modifier.ModifierRegistry;
import com.baidu.oped.apm.profiler.plugin.ClassEditorAdaptor;
import com.baidu.oped.apm.profiler.plugin.PluginLoader;

/**
 * @author emeroad
 * @author netspider
 */
public class ClassFileTransformerDispatcher implements ClassFileTransformer {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final boolean isDebug = logger.isDebugEnabled();

    private final ClassLoader agentClassLoader = this.getClass().getClassLoader();

    private final ModifierRegistry modifierRegistry;

    private final DefaultAgent agent;
    private final ByteCodeInstrumentor byteCodeInstrumentor;
    private final ClassFileRetransformer retransformer;

    private final ProfilerConfig profilerConfig;

    private final ClassFileFilter skipFilter;

    public ClassFileTransformerDispatcher(DefaultAgent agent, ByteCodeInstrumentor byteCodeInstrumentor, ClassFileRetransformer retransformer) {
        if (agent == null) {
            throw new NullPointerException("agent must not be null");
        }
        if (byteCodeInstrumentor == null) {
            throw new NullPointerException("byteCodeInstrumentor must not be null");
        }
        if (retransformer == null) {
            throw new NullPointerException("retransformer must not be null");
        }
        
        this.agent = agent;
        this.byteCodeInstrumentor = byteCodeInstrumentor;
        this.retransformer = retransformer;
        this.profilerConfig = agent.getProfilerConfig();
        this.modifierRegistry = createModifierRegistry();
        this.skipFilter = new DefaultClassFileFilter(agentClassLoader);
    }

    @Override
    public byte[] transform(ClassLoader classLoader, String jvmClassName, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classFileBuffer) throws IllegalClassFormatException {
        if (skipFilter.doFilter(classLoader, jvmClassName, classBeingRedefined, protectionDomain, classFileBuffer)) {
            return null;
        }

        AbstractModifier findModifier = this.modifierRegistry.findModifier(jvmClassName);
        if (findModifier == null) {
            // TODO For debug
            // TODO What if a modifier is duplicated?
            if (this.profilerConfig.getProfilableClassFilter().filter(jvmClassName)) {
                // Added to see if call stack view is OK on a test machine.
                findModifier = this.modifierRegistry.findModifier("*");
            } else {
                return null;
            }
        }

        if (isDebug) {
            logger.debug("[transform] cl:{} className:{} Modifier:{}", classLoader, jvmClassName, findModifier.getClass().getName());
        }
        final String javaClassName = JavaAssistUtils.jvmNameToJavaName(jvmClassName);

        try {
            final Thread thread = Thread.currentThread();
            final ClassLoader before = getContextClassLoader(thread);
            thread.setContextClassLoader(this.agentClassLoader);
            try {
                return findModifier.modify(classLoader, javaClassName, protectionDomain, classFileBuffer);
            } finally {
                // The context class loader have to be recovered even if it was null.
                thread.setContextClassLoader(before);
            }
        }
        catch (Throwable e) {
            logger.error("Modifier:{} modify fail. cl:{} ctxCl:{} agentCl:{} Cause:{}",
                    findModifier.getTargetClass(), classLoader, Thread.currentThread().getContextClassLoader(), agentClassLoader, e.getMessage(), e);
            return null;
        }
    }

    private ClassLoader getContextClassLoader(Thread thread) throws Throwable {
        try {
            return thread.getContextClassLoader();
        } catch (SecurityException se) {
            throw se;
        } catch (Throwable th) {
            if (isDebug) {
                logger.debug("getContextClassLoader(). Caused:{}", th.getMessage(), th);
            }
            throw th;
        }
    }

    private ModifierRegistry createModifierRegistry() {
        DefaultModifierRegistry modifierRepository = new DefaultModifierRegistry(agent, byteCodeInstrumentor, retransformer);

        modifierRepository.addMethodModifier();

        modifierRepository.addTomcatModifier();

        // jdbc
        modifierRepository.addJdbcModifier();

        // rpc
        modifierRepository.addConnectorModifier();

        // arcus, memcached
        modifierRepository.addArcusModifier();
        
        // orm
        modifierRepository.addOrmModifier();

        // spring beans
        modifierRepository.addSpringBeansModifier();
        
        // log4j
        modifierRepository.addLog4jModifier();
        
        // logback
        modifierRepository.addLogbackModifier();

        // redis
        modifierRepository.addRedisModifier();
        
        loadModifiers(modifierRepository);
        
        return modifierRepository;
    }
    
    private void loadModifiers(DefaultModifierRegistry modifierRepository) {
        PluginLoader<ModifierProvider> loader = new PluginLoader<ModifierProvider>(ModifierProvider.class, getClass().getClassLoader());
        
        for (ModifierProvider provider : loader.loadPlugins()) {
            for (Modifier modifier : provider.getModifiers(byteCodeInstrumentor, agent)) {
                if (modifier instanceof AbstractModifier) {
                    AbstractModifier abstractModifier = (AbstractModifier)modifier;
                    modifierRepository.addModifier(abstractModifier);
                    logger.info("Registering modifier {} from {} for {} ", abstractModifier.getClass().getName(), abstractModifier.getClass().getProtectionDomain().getCodeSource(), abstractModifier.getTargetClass());
                } else {
                    logger.warn("Ignore modifier {} from {}", modifier.getClass().getName(), modifier.getClass().getProtectionDomain().getCodeSource());
                }
            }
        }
    }

    /*
     * for plugins. This method is not used yet because plugin feature is not completed.
     */
    private void loadPlugins(DefaultModifierRegistry modifierRepository) {
        String pluginPath = agent.getAgentPath() + File.separatorChar + "plugin";
        PluginLoader<ProfilerPlugin> loader = PluginLoader.get(ProfilerPlugin.class, pluginPath);
        PluginClassLoaderFactory classLoaderFactory = new PluginClassLoaderFactory(loader.getPluginJars());
        List<ProfilerPlugin> plugins = loader.loadPlugins();
        ProfilerPluginContext pluginContext = new ProfilerPluginContext(byteCodeInstrumentor, agent.getTraceContext());
        
        for (ProfilerPlugin plugin : plugins) {
            logger.info("Loading plugin: {}", plugin.getClass().getName());
            
            for (ClassEditor editor : plugin.getClassEditors(pluginContext)) {
                if (editor instanceof DedicatedClassEditor) {
                    DedicatedClassEditor dedicated = (DedicatedClassEditor)editor;
                    logger.info("Registering class editor {} for {} ", dedicated.getClass().getName(), dedicated.getTargetClassName());
                    modifierRepository.addModifier(new ClassEditorAdaptor(byteCodeInstrumentor, agent, dedicated, classLoaderFactory));
                } else {
                    logger.warn("Ignore class editor {}", editor.getClass().getName());
                }
            }
        }
    }

}
