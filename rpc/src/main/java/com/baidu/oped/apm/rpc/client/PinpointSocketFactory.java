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

package com.baidu.oped.apm.rpc.client;


import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineException;
import org.jboss.netty.channel.socket.nio.NioClientBossPool;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioWorkerPool;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.ThreadNameDeterminer;
import org.jboss.netty.util.Timeout;
import org.jboss.netty.util.Timer;
import org.jboss.netty.util.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baidu.oped.apm.common.util.PinpointThreadFactory;
import com.baidu.oped.apm.rpc.PinpointSocketException;
import com.baidu.oped.apm.rpc.stream.DisabledServerStreamChannelMessageListener;
import com.baidu.oped.apm.rpc.stream.ServerStreamChannelMessageListener;
import com.baidu.oped.apm.rpc.util.AssertUtils;
import com.baidu.oped.apm.rpc.util.LoggerFactorySetup;
import com.baidu.oped.apm.rpc.util.TimerFactory;

/**
 * @author emeroad
 * @author koo.taejin
 */
public class PinpointSocketFactory {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public static final String CONNECT_TIMEOUT_MILLIS = "connectTimeoutMillis";
    private static final int DEFAULT_CONNECT_TIMEOUT = 5000;
    private static final long DEFAULT_TIMEOUTMILLIS = 3 * 1000;
    private static final long DEFAULT_PING_DELAY = 60 * 1000 * 5;
    private static final long DEFAULT_ENABLE_WORKER_PACKET_DELAY = 60 * 1000 * 1;


    private volatile boolean released;
    private ClientBootstrap bootstrap;
    private Map<String, Object> properties = Collections.emptyMap();
    
    private long reconnectDelay = 3 * 1000;
    private final Timer timer;

    // it's better to be a long value. even though keeping ping period from client to server short,
    // disconnection between them dose not be detected quickly.
    // rather keeping it from server to client short help detect disconnection as soon as possible.
    private long pingDelay = DEFAULT_PING_DELAY;
    private long enableWorkerPacketDelay = DEFAULT_ENABLE_WORKER_PACKET_DELAY;
    private long timeoutMillis = DEFAULT_TIMEOUTMILLIS;
    
    private MessageListener messageListener = SimpleLoggingMessageListener.LISTENER;
    private ServerStreamChannelMessageListener serverStreamChannelMessageListener = DisabledServerStreamChannelMessageListener.INSTANCE;
    
    static {
        LoggerFactorySetup.setupSlf4jLoggerFactory();
    }

    public PinpointSocketFactory() {
        this(1, 1);
    }

    public PinpointSocketFactory(int bossCount, int workerCount) {
        if (bossCount < 1) {
            throw new IllegalArgumentException("bossCount is negative: " + bossCount);
        }

        // create a timer earlier because it is used for connectTimeout
        Timer timer = createTimer();
        ClientBootstrap bootstrap = createBootStrap(bossCount, workerCount, timer);
        setOptions(bootstrap);
        addPipeline(bootstrap);

        this.bootstrap = bootstrap;
        this.timer = timer;
    }

    private Timer createTimer() {
        HashedWheelTimer timer = TimerFactory.createHashedWheelTimer("Pinpoint-SocketFactory-Timer", 100, TimeUnit.MILLISECONDS, 512);
        timer.start();
        return timer;
    }

    private void addPipeline(ClientBootstrap bootstrap) {
        SocketClientPipelineFactory socketClientPipelineFactory = new SocketClientPipelineFactory(this);
        bootstrap.setPipelineFactory(socketClientPipelineFactory);
    }

    private void setOptions(ClientBootstrap bootstrap) {
        // connectTimeout
        bootstrap.setOption(CONNECT_TIMEOUT_MILLIS, DEFAULT_CONNECT_TIMEOUT);
        // read write timeout needed?  isn't it needed because of nio?

        // tcp setting
        bootstrap.setOption("tcpNoDelay", true);
        bootstrap.setOption("keepAlive", true);
        // buffer setting
        bootstrap.setOption("sendBufferSize", 1024 * 64);
        bootstrap.setOption("receiveBufferSize", 1024 * 64);

    }

    public void setConnectTimeout(int connectTimeout) {
        if (connectTimeout < 0) {
            throw new IllegalArgumentException("connectTimeout cannot be a negative number");
        }
        bootstrap.setOption(CONNECT_TIMEOUT_MILLIS, connectTimeout);
    }

    public int getConnectTimeout() {
        return (Integer) bootstrap.getOption(CONNECT_TIMEOUT_MILLIS);
    }

    public long getReconnectDelay() {
        return reconnectDelay;
    }

    public void setReconnectDelay(long reconnectDelay) {
        if (reconnectDelay < 0) {
            throw new IllegalArgumentException("reconnectDelay cannot be a negative number");
        }
        this.reconnectDelay = reconnectDelay;
    }

    public long getPingDelay() {
        return pingDelay;
    }

    public void setPingDelay(long pingDelay) {
        if (pingDelay < 0) {
            throw new IllegalArgumentException("pingDelay cannot be a negative number");
        }
        this.pingDelay = pingDelay;
    }
    
    public long getEnableWorkerPacketDelay() {
        return enableWorkerPacketDelay;
    }

    public void setEnableWorkerPacketDelay(long enableWorkerPacketDelay) {
        if (enableWorkerPacketDelay < 0) {
            throw new IllegalArgumentException("EnableWorkerPacketDelay cannot be a negative number");
        }
         this.enableWorkerPacketDelay = enableWorkerPacketDelay;
    }

    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    public void setTimeoutMillis(long timeoutMillis) {
        if (timeoutMillis < 0) {
            throw new IllegalArgumentException("timeoutMillis cannot be a negative number");
        }
        this.timeoutMillis = timeoutMillis;
    }

    private ClientBootstrap createBootStrap(int bossCount, int workerCount, Timer timer) {
        // profiler, collector,
        logger.debug("createBootStrap boss:{}, worker:{}", bossCount, workerCount);
        NioClientSocketChannelFactory nioClientSocketChannelFactory = createChannelFactory(bossCount, workerCount, timer);
        return new ClientBootstrap(nioClientSocketChannelFactory);
    }

    private NioClientSocketChannelFactory createChannelFactory(int bossCount, int workerCount, Timer timer) {
        ExecutorService boss = Executors.newCachedThreadPool(new PinpointThreadFactory("Pinpoint-Client-Boss", true));
        NioClientBossPool bossPool = new NioClientBossPool(boss, bossCount, timer, ThreadNameDeterminer.CURRENT);

        ExecutorService worker = Executors.newCachedThreadPool(new PinpointThreadFactory("Pinpoint-Client-Worker", true));
        NioWorkerPool workerPool = new NioWorkerPool(worker, workerCount, ThreadNameDeterminer.CURRENT);
        return new NioClientSocketChannelFactory(bossPool, workerPool);
    }

    public PinpointSocket connect(String host, int port) throws PinpointSocketException {
        SocketAddress address = new InetSocketAddress(host, port);
        ChannelFuture connectFuture = bootstrap.connect(address);
        SocketHandler socketHandler = getSocketHandler(connectFuture, address);

        PinpointSocket pinpointSocket = new PinpointSocket(socketHandler);
        traceSocket(pinpointSocket);
        return pinpointSocket;
    }

    public PinpointSocket reconnect(String host, int port) throws PinpointSocketException {
        SocketAddress address = new InetSocketAddress(host, port);
        ChannelFuture connectFuture = bootstrap.connect(address);
        SocketHandler socketHandler = getSocketHandler(connectFuture, address);

        PinpointSocket pinpointSocket = new PinpointSocket(socketHandler);
        traceSocket(pinpointSocket);
        return pinpointSocket;
    }

    /*
        trace mechanism is needed in case of calling close without closing socket
        it is okay to make that later because this is a exceptional case.
     */
    private void traceSocket(PinpointSocket pinpointSocket) {

    }

    public PinpointSocket scheduledConnect(String host, int port) {
        PinpointSocket pinpointSocket = new PinpointSocket(new ReconnectStateSocketHandler());
        SocketAddress address = new InetSocketAddress(host, port);
        reconnect(pinpointSocket, address);
        return pinpointSocket;
    }

    SocketHandler getSocketHandler(ChannelFuture connectFuture, SocketAddress address) {
        if (address == null) {
            throw new NullPointerException("address");
        }

        SocketHandler socketHandler = getSocketHandler(connectFuture.getChannel());
        socketHandler.setConnectSocketAddress(address);

        connectFuture.awaitUninterruptibly();
        if (!connectFuture.isSuccess()) {
            throw new PinpointSocketException("connect fail. " + address, connectFuture.getCause());
        }
        socketHandler.open();
        return socketHandler;
    }

    public ChannelFuture reconnect(final SocketAddress remoteAddress) {
        if (remoteAddress == null) {
            throw new NullPointerException("remoteAddress");
        }

        ChannelPipeline pipeline;
        final ClientBootstrap bootstrap = this.bootstrap;
        try {
            pipeline = bootstrap.getPipelineFactory().getPipeline();
        } catch (Exception e) {
            throw new ChannelPipelineException("Failed to initialize a pipeline.", e);
        }
        SocketHandler socketHandler = (PinpointSocketHandler) pipeline.getLast();
        socketHandler.initReconnect();


        // Set the options.
        Channel ch = bootstrap.getFactory().newChannel(pipeline);
        boolean success = false;
        try {
            ch.getConfig().setOptions(bootstrap.getOptions());
            success = true;
        } finally {
            if (!success) {
                ch.close();
            }
        }

        // Connect.
        return ch.connect(remoteAddress);
    }

    public Timeout newTimeout(TimerTask task, long delay, TimeUnit unit) {
        return this.timer.newTimeout(task, delay, unit);
    }


    private SocketHandler getSocketHandler(Channel channel) {
        return (SocketHandler) channel.getPipeline().getLast();
    }

    void reconnect(final PinpointSocket pinpointSocket, final SocketAddress socketAddress) {
        ConnectEvent connectEvent = new ConnectEvent(pinpointSocket, socketAddress);
        timer.newTimeout(connectEvent, reconnectDelay, TimeUnit.MILLISECONDS);
    }


    private class ConnectEvent implements TimerTask {

        private final Logger logger = LoggerFactory.getLogger(getClass());
        private final PinpointSocket pinpointSocket;
        private final SocketAddress socketAddress;

        private ConnectEvent(PinpointSocket pinpointSocket, SocketAddress socketAddress) {
            if (pinpointSocket == null) {
                throw new NullPointerException("pinpointSocket must not be null");
            }
            if (socketAddress == null) {
                throw new NullPointerException("socketAddress must not be null");
            }

            this.pinpointSocket = pinpointSocket;
            this.socketAddress = socketAddress;
        }

        @Override
        public void run(Timeout timeout) {
            if (timeout.isCancelled()) {
                return;
            }

            // Just return not to try reconnection when event has been fired but pinpointSocket already closed.
            if (pinpointSocket.isClosed()) {
                logger.debug("pinpointSocket is already closed.");
                return;
            }

            logger.warn("try reconnect. connectAddress:{}", socketAddress);
            final ChannelFuture channelFuture = reconnect(socketAddress);
            Channel channel = channelFuture.getChannel();
            final SocketHandler socketHandler = getSocketHandler(channel);
            socketHandler.setConnectSocketAddress(socketAddress);
            socketHandler.setPinpointSocket(pinpointSocket);

            channelFuture.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (future.isSuccess()) {
                        Channel channel = future.getChannel();
                        logger.warn("reconnect success {}, {}", socketAddress, channel);
                        socketHandler.open();
                        pinpointSocket.reconnectSocketHandler(socketHandler);
                    } else {
                         if (!pinpointSocket.isClosed()) {

                         /*
                            // comment out because exception message can be taken at exceptionCought
                            if (logger.isWarnEnabled()) {
                                Throwable cause = future.getCause();
                                logger.warn("reconnect fail. {} Caused:{}", socketAddress, cause.getMessage());
                            }
                          */
                            reconnect(pinpointSocket, socketAddress);
                        } else {
                            logger.info("pinpointSocket is closed. stop reconnect.");
                        }
                    }
                }
            });
        }
    }


    public void release() {
        synchronized (this) {
            if (released) {
                return;
            }
            released = true;
        }

        if (bootstrap != null) {
            bootstrap.releaseExternalResources();
        }
        Set<Timeout> stop = this.timer.stop();
        if (!stop.isEmpty()) {
            logger.info("stop Timeout:{}", stop.size());
        }

        // stop, cancel something?
    }

    Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> agentProperties) {
        AssertUtils.assertNotNull(properties, "agentProperties must not be null");

        this.properties = Collections.unmodifiableMap(agentProperties);
    }

    public MessageListener getMessageListener() {
        return messageListener;
    }

    public void setMessageListener(MessageListener messageListener) {
        AssertUtils.assertNotNull(messageListener, "messageListener must not be null");

        this.messageListener = messageListener;
    }

    public ServerStreamChannelMessageListener getServerStreamChannelMessageListener() {
        return serverStreamChannelMessageListener;
    }

    public void setServerStreamChannelMessageListener(ServerStreamChannelMessageListener serverStreamChannelMessageListener) {
        AssertUtils.assertNotNull(messageListener, "messageListener must not be null");

        this.serverStreamChannelMessageListener = serverStreamChannelMessageListener;
    }

}
