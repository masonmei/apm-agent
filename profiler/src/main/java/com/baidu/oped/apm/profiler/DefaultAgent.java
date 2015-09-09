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

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baidu.oped.apm.ProductInfo;
import com.baidu.oped.apm.bootstrap.Agent;
import com.baidu.oped.apm.bootstrap.config.ProfilerConfig;
import com.baidu.oped.apm.bootstrap.context.ServerMetaDataHolder;
import com.baidu.oped.apm.bootstrap.context.TraceContext;
import com.baidu.oped.apm.bootstrap.instrument.ByteCodeInstrumentor;
import com.baidu.oped.apm.bootstrap.logging.PLogger;
import com.baidu.oped.apm.bootstrap.logging.PLoggerBinder;
import com.baidu.oped.apm.bootstrap.logging.PLoggerFactory;
import com.baidu.oped.apm.bootstrap.sampler.Sampler;
import com.baidu.oped.apm.exception.PinpointException;
import com.baidu.oped.apm.profiler.context.DefaultServerMetaDataHolder;
import com.baidu.oped.apm.profiler.context.DefaultTraceContext;
import com.baidu.oped.apm.profiler.context.storage.BufferedStorageFactory;
import com.baidu.oped.apm.profiler.context.storage.SpanStorageFactory;
import com.baidu.oped.apm.profiler.context.storage.StorageFactory;
import com.baidu.oped.apm.profiler.interceptor.bci.JavaAssistByteCodeInstrumentor;
import com.baidu.oped.apm.profiler.logging.Slf4jLoggerBinder;
import com.baidu.oped.apm.profiler.monitor.AgentStatMonitor;
import com.baidu.oped.apm.profiler.receiver.CommandDispatcher;
import com.baidu.oped.apm.profiler.receiver.service.EchoService;
import com.baidu.oped.apm.profiler.receiver.service.ThreadDumpService;
import com.baidu.oped.apm.profiler.sampler.SamplerFactory;
import com.baidu.oped.apm.profiler.sender.DataSender;
import com.baidu.oped.apm.profiler.sender.EnhancedDataSender;
import com.baidu.oped.apm.profiler.sender.TcpDataSender;
import com.baidu.oped.apm.profiler.sender.UdpDataSender;
import com.baidu.oped.apm.profiler.util.ApplicationServerTypeResolver;
import com.baidu.oped.apm.profiler.util.PreparedStatementUtils;
import com.baidu.oped.apm.profiler.util.RuntimeMXBeanUtils;
import com.baidu.oped.apm.rpc.ClassPreLoader;
import com.baidu.oped.apm.rpc.PinpointSocketException;
import com.baidu.oped.apm.rpc.client.PinpointSocket;
import com.baidu.oped.apm.rpc.client.PinpointSocketFactory;

/**
 * @author emeroad
 * @author koo.taejin
 * @author hyungil.jeong
 */
public class DefaultAgent implements Agent {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final PLoggerBinder binder;

    private final ByteCodeInstrumentor byteCodeInstrumentor;
    private final ClassFileTransformer classFileTransformer;
    
    private final String agentPath;
    private final ProfilerConfig profilerConfig;

    private final AgentInfoSender agentInfoSender;
    private final AgentStatMonitor agentStatMonitor;

    private final TraceContext traceContext;

    private final PinpointSocketFactory factory;
    private final PinpointSocket socket;

    private final EnhancedDataSender tcpDataSender;
    private final DataSender statDataSender;
    private final DataSender spanDataSender;

    private final AgentInformation agentInformation;
    private final ServerMetaDataHolder serverMetaDataHolder;

    private volatile AgentStatus agentStatus;

    static {
        // Preload classes related to pinpoint-rpc module.
        ClassPreLoader.preload();
    }

    public DefaultAgent(String agentPath, String agentArgs, Instrumentation instrumentation, ProfilerConfig profilerConfig) {
        if (agentPath == null) {
            throw new NullPointerException("agentPath must not be null");
        }
        if (instrumentation == null) {
            throw new NullPointerException("instrumentation must not be null");
        }
        if (profilerConfig == null) {
            throw new NullPointerException("profilerConfig must not be null");
        }
        
        this.binder = new Slf4jLoggerBinder();
        bindPLoggerFactory(this.binder);

        dumpSystemProperties();
        dumpConfig(profilerConfig);

        changeStatus(AgentStatus.INITIALIZING);

        this.agentPath = agentPath;
        this.profilerConfig = profilerConfig;

        final ApplicationServerTypeResolver typeResolver = new ApplicationServerTypeResolver(profilerConfig.getApplicationServerType());
        if (!typeResolver.resolve()) {
            throw new PinpointException("ApplicationServerType not found.");
        }
        this.byteCodeInstrumentor = new JavaAssistByteCodeInstrumentor(this);
        if (logger.isInfoEnabled()) {
            logger.info("DefaultAgent classLoader:{}", this.getClass().getClassLoader());
        }
        
        final AgentInformationFactory agentInformationFactory = new AgentInformationFactory();
        this.agentInformation = agentInformationFactory.createAgentInformation(typeResolver.getServerType());
        logger.info("agentInformation:{}", agentInformation);

        CommandDispatcher commandDispatcher = new CommandDispatcher();
        commandDispatcher.registerCommandService(new ThreadDumpService());
        commandDispatcher.registerCommandService(new EchoService());
        
        this.factory = createPinpointSocketFactory(commandDispatcher);
        this.socket = createPinpointSocket(this.profilerConfig.getCollectorTcpServerIp(), this.profilerConfig.getCollectorTcpServerPort(), factory);

        this.serverMetaDataHolder = createServerMetaDataHolder();
        
        this.tcpDataSender = createTcpDataSender(socket);

        this.spanDataSender = createUdpSpanDataSender(this.profilerConfig.getCollectorSpanServerPort(), "Pinpoint-UdpSpanDataExecutor",
                this.profilerConfig.getSpanDataSenderWriteQueueSize(), this.profilerConfig.getSpanDataSenderSocketTimeout(),
                this.profilerConfig.getSpanDataSenderSocketSendBufferSize());
        this.statDataSender = createUdpStatDataSender(this.profilerConfig.getCollectorStatServerPort(), "Pinpoint-UdpStatDataExecutor",
                this.profilerConfig.getStatDataSenderWriteQueueSize(), this.profilerConfig.getStatDataSenderSocketTimeout(),
                this.profilerConfig.getStatDataSenderSocketSendBufferSize());

        this.traceContext = createTraceContext(agentInformation.getServerType());

        this.agentInfoSender = new AgentInfoSender(tcpDataSender, profilerConfig.getAgentInfoSendRetryInterval(), this.agentInformation, this.serverMetaDataHolder);

        this.agentStatMonitor = new AgentStatMonitor(this.statDataSender, this.agentInformation.getAgentId(), this.agentInformation.getStartTime());
        
        
        ClassFileRetransformer retransformer = new ClassFileRetransformer(instrumentation);
        instrumentation.addTransformer(retransformer, true);
        this.classFileTransformer = new ClassFileTransformerDispatcher(this, byteCodeInstrumentor, retransformer);
        instrumentation.addTransformer(this.classFileTransformer);


        preLoadClass();

        /**
         * FIXME
         * In case of Tomcat, com.baidu.oped.apm.profiler.modifier.tomcat.interceptor.CatalinaAwaitInterceptor invokes start() method 
         * before entering await() method of org.apache.catalina.startup.Catalina. But for other applications, it must be invoked directly. 
         */
        if (typeResolver.isManuallyStartupRequired()) {
            start();
        }
    }

    private void preLoadClass() {
        logger.debug("preLoadClass:{}", PreparedStatementUtils.class.getName(), PreparedStatementUtils.findBindVariableSetMethod());
    }

    public ByteCodeInstrumentor getByteCodeInstrumentor() {
        return byteCodeInstrumentor;
    }

    public ClassFileTransformer getClassFileTransformer() {
        return classFileTransformer;
    }

    private void dumpSystemProperties() {
        if (logger.isInfoEnabled()) {
            Properties properties = System.getProperties();
            Set<String> strings = properties.stringPropertyNames();
            for (String key : strings) {
                logger.info("SystemProperties {}={}", key, properties.get(key));
            }
        }
    }

    private void dumpConfig(ProfilerConfig profilerConfig) {
        if (logger.isInfoEnabled()) {
            logger.info("{}\n{}", "dumpConfig", profilerConfig);

        }
    }

    public ProfilerConfig getProfilerConfig() {
        return profilerConfig;
    }

    private void changeStatus(AgentStatus status) {
        this.agentStatus = status;
        if (logger.isDebugEnabled()) {
            logger.debug("Agent status is changed. {}", status);
        }
    }

    private void bindPLoggerFactory(PLoggerBinder binder) {
        final String binderClassName = binder.getClass().getName();
        PLogger pLogger = binder.getLogger(binder.getClass().getName());
        pLogger.info("PLoggerFactory.initialize() bind:{} cl:{}", binderClassName, binder.getClass().getClassLoader());
        // Set binder to static LoggerFactory
        // Should we unset binder at shutdonw hook or stop()?
        PLoggerFactory.initialize(binder);
    }

    private TraceContext createTraceContext(short serverType) {
        final StorageFactory storageFactory = createStorageFactory();
        logger.info("StorageFactoryType:{}", storageFactory);

        final Sampler sampler = createSampler();
        logger.info("SamplerType:{}", sampler);
        
        final int jdbcSqlCacheSize = profilerConfig.getJdbcSqlCacheSize();
        final DefaultTraceContext traceContext = new DefaultTraceContext(jdbcSqlCacheSize, serverType, storageFactory, sampler, this.serverMetaDataHolder);
        traceContext.setAgentInformation(this.agentInformation);
        traceContext.setPriorityDataSender(this.tcpDataSender);

        traceContext.setProfilerConfig(profilerConfig);

        return traceContext;
    }

    protected StorageFactory createStorageFactory() {
        if (profilerConfig.isIoBufferingEnable()) {
            return new BufferedStorageFactory(this.spanDataSender, this.profilerConfig, this.agentInformation);
        } else {
            return new SpanStorageFactory(spanDataSender);

        }
    }

    private Sampler createSampler() {
        boolean samplingEnable = this.profilerConfig.isSamplingEnable();
        int samplingRate = this.profilerConfig.getSamplingRate();

        SamplerFactory samplerFactory = new SamplerFactory();
        return samplerFactory.createSampler(samplingEnable, samplingRate);
    }
    
    protected ServerMetaDataHolder createServerMetaDataHolder() {
        List<String> vmArgs = RuntimeMXBeanUtils.getVmArgs();
        ServerMetaDataHolder serverMetaDataHolder = new DefaultServerMetaDataHolder(vmArgs);
        return serverMetaDataHolder;
    }

    protected PinpointSocketFactory createPinpointSocketFactory(CommandDispatcher commandDispatcher) {
        PinpointSocketFactory pinpointSocketFactory = new PinpointSocketFactory();
        pinpointSocketFactory.setTimeoutMillis(1000 * 5);

        Map<String, Object> properties = this.agentInformation.toMap();
        
        boolean isSupportServerMode = this.profilerConfig.isTcpDataSenderCommandAcceptEnable();
        
        if (isSupportServerMode) {
            pinpointSocketFactory.setMessageListener(commandDispatcher);
            pinpointSocketFactory.setServerStreamChannelMessageListener(commandDispatcher);

            properties.put(AgentHandshakePropertyType.SUPPORT_SERVER.getName(), true);
        } else {
            properties.put(AgentHandshakePropertyType.SUPPORT_SERVER.getName(), false);
        }

        pinpointSocketFactory.setProperties(properties);
        return pinpointSocketFactory;
    }

    protected PinpointSocket createPinpointSocket(String host, int port, PinpointSocketFactory factory) {
        PinpointSocket socket = null;
        for (int i = 0; i < 3; i++) {
            try {
                socket = factory.connect(host, port);
                logger.info("tcp connect success:{}/{}", host, port);
                return socket;
            } catch (PinpointSocketException e) {
                logger.warn("tcp connect fail:{}/{} try reconnect, retryCount:{}", host, port, i);
            }
        }
        logger.warn("change background tcp connect mode  {}/{} ", host, port);
        socket = factory.scheduledConnect(host, port);

        return socket;
    }

    protected EnhancedDataSender createTcpDataSender(PinpointSocket socket) {
        return new TcpDataSender(socket);
    }

    protected DataSender createUdpStatDataSender(int port, String threadName, int writeQueueSize, int timeout, int sendBufferSize) {
        return new UdpDataSender(this.profilerConfig.getCollectorStatServerIp(), port, threadName, writeQueueSize, timeout, sendBufferSize);
    }
    
    protected DataSender createUdpSpanDataSender(int port, String threadName, int writeQueueSize, int timeout, int sendBufferSize) {
        return new UdpDataSender(this.profilerConfig.getCollectorSpanServerIp(), port, threadName, writeQueueSize, timeout, sendBufferSize);
    }

    protected EnhancedDataSender getTcpDataSender() {
        return tcpDataSender;
    }

    protected DataSender getStatDataSender() {
        return statDataSender;
    }

    protected DataSender getSpanDataSender() {
        return spanDataSender;
    }

    public TraceContext getTraceContext() {
        return traceContext;
    }

    public AgentInformation getAgentInformation() {
        return agentInformation;
    }
    
    public String getAgentPath() {
        return agentPath;
    }
    
    @Override
    public void start() {
        synchronized (this) {
            if (this.agentStatus == AgentStatus.INITIALIZING) {
                changeStatus(AgentStatus.RUNNING);
            } else {
                logger.warn("Agent already started.");
                return;
            }
        }
        logger.info("Starting {} Agent.", ProductInfo.CAMEL_NAME);
        this.agentInfoSender.start();
        this.agentStatMonitor.start();
    }

    @Override
    public void stop() {
        synchronized (this) {
            if (this.agentStatus == AgentStatus.RUNNING) {
                changeStatus(AgentStatus.STOPPED);
            } else {
                logger.warn("Cannot stop agent. Current status = [{}]", this.agentStatus);
                return;
            }
        }
        logger.info("Stopping {} Agent.", ProductInfo.CAMEL_NAME);

        this.agentInfoSender.stop();
        this.agentStatMonitor.stop();

        // Need to process stop
        this.spanDataSender.stop();
        this.statDataSender.stop();
        this.tcpDataSender.stop();

        if (this.socket != null) {
            this.socket.close();
        }
        if (this.factory != null) {
            this.factory.release();
        }
        PLoggerFactory.unregister(this.binder);
    }

}
