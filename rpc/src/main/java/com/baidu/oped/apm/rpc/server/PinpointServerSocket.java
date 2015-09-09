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

package com.baidu.oped.apm.rpc.server;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.ChannelGroupFuture;
import org.jboss.netty.channel.group.ChannelGroupFutureListener;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerBossPool;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioWorkerPool;
import org.jboss.netty.util.ThreadNameDeterminer;
import org.jboss.netty.util.Timeout;
import org.jboss.netty.util.Timer;
import org.jboss.netty.util.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baidu.oped.apm.common.util.PinpointThreadFactory;
import com.baidu.oped.apm.rpc.PinpointSocketException;
import com.baidu.oped.apm.rpc.client.WriteFailFutureListener;
import com.baidu.oped.apm.rpc.packet.ControlHandshakePacket;
import com.baidu.oped.apm.rpc.packet.HandshakeResponseCode;
import com.baidu.oped.apm.rpc.packet.Packet;
import com.baidu.oped.apm.rpc.packet.PacketType;
import com.baidu.oped.apm.rpc.packet.PingPacket;
import com.baidu.oped.apm.rpc.packet.RequestPacket;
import com.baidu.oped.apm.rpc.packet.ResponsePacket;
import com.baidu.oped.apm.rpc.packet.SendPacket;
import com.baidu.oped.apm.rpc.packet.ServerClosePacket;
import com.baidu.oped.apm.rpc.packet.stream.StreamPacket;
import com.baidu.oped.apm.rpc.server.handler.ChannelStateChangeEventHandler;
import com.baidu.oped.apm.rpc.server.handler.DoNothingChannelStateEventHandler;
import com.baidu.oped.apm.rpc.stream.DisabledServerStreamChannelMessageListener;
import com.baidu.oped.apm.rpc.stream.ServerStreamChannelMessageListener;
import com.baidu.oped.apm.rpc.stream.StreamChannelManager;
import com.baidu.oped.apm.rpc.util.AssertUtils;
import com.baidu.oped.apm.rpc.util.CpuUtils;
import com.baidu.oped.apm.rpc.util.IDGenerator;
import com.baidu.oped.apm.rpc.util.LoggerFactorySetup;
import com.baidu.oped.apm.rpc.util.TimerFactory;

/**
 * @author emeroad
 * @author koo.taejin
 */
public class PinpointServerSocket extends SimpleChannelHandler {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    // private final boolean isDebug = logger.isDebugEnabled();

    private static final long DEFAULT_TIMEOUTMILLIS = 3 * 1000;
    private static final int WORKER_COUNT = CpuUtils.workerCount();

    private volatile boolean released;
    private ServerBootstrap bootstrap;

    private Channel serverChannel;
    private final ChannelGroup channelGroup = new DefaultChannelGroup();
    private final Timer pingTimer;
    private final Timer requestManagerTimer;

    private ServerMessageListener messageListener = SimpleLoggingServerMessageListener.LISTENER;
    private ServerStreamChannelMessageListener serverStreamChannelMessageListener = DisabledServerStreamChannelMessageListener.INSTANCE;

    private WriteFailFutureListener traceSendAckWriteFailFutureListener = new WriteFailFutureListener(logger, "TraceSendAckPacket send fail.", "TraceSendAckPacket send() success.");
    private InetAddress[] ignoreAddressList;

    private final ChannelStateChangeEventHandler channelStateChangeEventHandler;

    private final PinpointServerSocketHandshaker handshaker;

    static {
        LoggerFactorySetup.setupSlf4jLoggerFactory();
    }

    public PinpointServerSocket() {
        this(DoNothingChannelStateEventHandler.INSTANCE);
    }

    public PinpointServerSocket(ChannelStateChangeEventHandler channelStateChangeEventHandler) {
        ServerBootstrap bootstrap = createBootStrap(1, WORKER_COUNT);
        setOptions(bootstrap);
        addPipeline(bootstrap);
        this.bootstrap = bootstrap;
        this.pingTimer = TimerFactory.createHashedWheelTimer("PinpointServerSocket-PingTimer", 50, TimeUnit.MILLISECONDS, 512);
        this.requestManagerTimer = TimerFactory.createHashedWheelTimer("PinpointServerSocket-RequestManager", 50, TimeUnit.MILLISECONDS, 512);

        this.channelStateChangeEventHandler = channelStateChangeEventHandler;
        this.handshaker = new PinpointServerSocketHandshaker();
    }

    public void setIgnoreAddressList(InetAddress[] ignoreAddressList) {
        if (ignoreAddressList == null) {
            throw new NullPointerException("ignoreAddressList must not be null");
        }
        this.ignoreAddressList = ignoreAddressList;
    }

    private void addPipeline(ServerBootstrap bootstrap) {
        ServerPipelineFactory serverPipelineFactory = new ServerPipelineFactory(this);
        bootstrap.setPipelineFactory(serverPipelineFactory);
    }

    void setPipelineFactory(ChannelPipelineFactory channelPipelineFactory) {
        if (channelPipelineFactory == null) {
            throw new NullPointerException("channelPipelineFactory must not be null");
        }
        bootstrap.setPipelineFactory(channelPipelineFactory);
    }

    public void setMessageListener(ServerMessageListener messageListener) {
        if (messageListener == null) {
            throw new NullPointerException("messageListener must not be null");
        }
        this.messageListener = messageListener;
    }

    public void setServerStreamChannelMessageListener(ServerStreamChannelMessageListener serverStreamChannelMessageListener) {
        AssertUtils.assertNotNull(serverStreamChannelMessageListener, "serverStreamChannelMessageListener must not be null");

        this.serverStreamChannelMessageListener = serverStreamChannelMessageListener;
    }

    private void setOptions(ServerBootstrap bootstrap) {
        // is read/write timeout necessary? don't need it because of NIO?
        // write timeout should be set through additional interceptor. write timeout exists.

        // tcp setting
        bootstrap.setOption("child.tcpNoDelay", true);
        bootstrap.setOption("child.keepAlive", true);
        // buffer setting
        bootstrap.setOption("child.sendBufferSize", 1024 * 64);
        bootstrap.setOption("child.receiveBufferSize", 1024 * 64);

        // bootstrap.setOption("child.soLinger", 0);

    }

    private short getPacketType(Object packet) {
        if (packet == null) {
            return PacketType.UNKNOWN;
        }

        if (packet instanceof Packet) {
            return ((Packet) packet).getPacketType();
        }

        return PacketType.UNKNOWN;
    }

    private ServerBootstrap createBootStrap(int bossCount, int workerCount) {
        // profiler, collector
        ExecutorService boss = Executors.newCachedThreadPool(new PinpointThreadFactory("Pinpoint-Server-Boss"));
        NioServerBossPool nioServerBossPool = new NioServerBossPool(boss, bossCount, ThreadNameDeterminer.CURRENT);

        ExecutorService worker = Executors.newCachedThreadPool(new PinpointThreadFactory("Pinpoint-Server-Worker"));
        NioWorkerPool nioWorkerPool = new NioWorkerPool(worker, workerCount, ThreadNameDeterminer.CURRENT);

        NioServerSocketChannelFactory nioClientSocketChannelFactory = new NioServerSocketChannelFactory(nioServerBossPool, nioWorkerPool);
        return new ServerBootstrap(nioClientSocketChannelFactory);
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        final Channel channel = e.getChannel();
        Object message = e.getMessage();

        logger.debug("messageReceived:{} channel:{}", message, channel);

        ChannelContext channelContext = getChannelContext(channel);
        if (!PinpointServerSocketStateCode.isRun(channelContext.getCurrentStateCode())) {
            // FIXME need change rules. 
            // as-is : do nothing when state is not run. 
            // candidate : close channel when state is not run. 
            logger.warn("MessageReceived:{} from IllegalState Channel:{} this message will be ignore.", message, channel);
            return;
        }

        final short packetType = getPacketType(message);
        switch (packetType) {
            case PacketType.APPLICATION_SEND: {
                SocketChannel socketChannel = channelContext.getSocketChannel();
                messageListener.handleSend((SendPacket) message, socketChannel);
                return;
            }
//          case PacketType.APPLICATION_TRACE_SEND: {
//          SocketChannel socketChannel = getChannelContext(channel).getSocketChannel();
//          TraceSendPacket traceSendPacket = (TraceSendPacket) message;
//          try {
//              messageListener.handleSend(traceSendPacket, socketChannel);
//          } finally {
//              TraceSendAckPacket traceSendAckPacket = new TraceSendAckPacket(traceSendPacket.getTransactionId());
//              ChannelFuture write = channel.write(traceSendAckPacket);
//              write.addListener(traceSendAckWriteFailFutureListener);
//          }
//          return;
//      }
            case PacketType.APPLICATION_REQUEST: {
                SocketChannel socketChannel = channelContext.getSocketChannel();
                messageListener.handleRequest((RequestPacket) message, socketChannel);
                return;
            }
            case PacketType.APPLICATION_RESPONSE: {
                SocketChannel socketChannel = channelContext.getSocketChannel();
                socketChannel.receiveResponsePacket((ResponsePacket) message);
                return;
            }
            case PacketType.APPLICATION_STREAM_CREATE:
            case PacketType.APPLICATION_STREAM_CLOSE:
            case PacketType.APPLICATION_STREAM_CREATE_SUCCESS:
            case PacketType.APPLICATION_STREAM_CREATE_FAIL:
            case PacketType.APPLICATION_STREAM_RESPONSE:
            case PacketType.APPLICATION_STREAM_PING:
            case PacketType.APPLICATION_STREAM_PONG:
                handleStreamPacket((StreamPacket) message, channel);
                return;
            case PacketType.CONTROL_HANDSHAKE:
                int requestId = ((ControlHandshakePacket) message).getRequestId();
                Map<Object, Object> handshakeData = this.handshaker.decodeHandshakePacket((ControlHandshakePacket) message);
    
                HandshakeResponseCode responseCode = this.handshaker.handleHandshake(handshakeData, messageListener);
                boolean isFirst = channelContext.setChannelProperties(handshakeData);
                if (isFirst) {
                    if (HandshakeResponseCode.DUPLEX_COMMUNICATION == responseCode) {
                        channelContext.changeStateToRunDuplex(PinpointServerSocketStateCode.RUN_DUPLEX);
                    } else if (HandshakeResponseCode.SIMPLEX_COMMUNICATION == responseCode) {
                        channelContext.changeStateToRunSimplex(PinpointServerSocketStateCode.RUN_SIMPLEX);
                    }
                }
                
                this.handshaker.sendHandshakeResponse(channel, requestId, responseCode, isFirst);
                return;
            case PacketType.CONTROL_CLIENT_CLOSE: {
                handleClosePacket(channel);
                return;
            }
            default:
                logger.warn("invalid messageReceived msg:{}, connection:{}", message, e.getChannel());
        }
    }

    private void handleClosePacket(Channel channel) {
        logger.debug("handleClosePacket channel:{}", channel);
        ChannelContext channelContext = getChannelContext(channel);
        channelContext.changeStateBeingShutdown(PinpointServerSocketStateCode.BEING_SHUTDOWN);

        // close socket when the node on channel close socket
        // channel.close();
    }

    private void handleStreamPacket(StreamPacket packet, Channel channel) {
        ChannelContext context = getChannelContext(channel);
        context.getStreamChannelManager().messageReceived(packet);
    }

    @Override
    public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        final Channel channel = e.getChannel();
        logger.debug("channelOpen channel:{}", channel);

        super.channelOpen(ctx, e);
    }

    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        final Channel channel = e.getChannel();
        logger.info("channelConnected channel:{}", channel);

        if (released) {
            logger.warn("already released. channel:{}", channel);
            channel.write(new ServerClosePacket()).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    future.getChannel().close();
                }
            });
            return;
        }
        prepareChannel(channel);

        ChannelContext channelContext = getChannelContext(channel);
        boolean isIgnore = isIgnoreAddress(channel);
        if (!isIgnore) {
            channelContext.changeStateToRunWithoutHandshake();
        }

        super.channelConnected(ctx, e);
    }

    @Override
    public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        final Channel channel = e.getChannel();
        logger.info("channelDisconnected channel:{}", channel);

        final ChannelContext channelContext = getChannelContext(channel);
        if (isExpectedShutdown(channelContext)) {
            channelContext.changeStateToShutdown();
        } else {
            boolean isIgnore = isIgnoreAddress(channel);
            if (!isIgnore) {
                channelContext.changeStateToUnexpectedShutdown();
            }
        }

        channelContext.closeAllStreamChannel();
        this.channelGroup.remove(channel);
        super.channelDisconnected(ctx, e);
    }

    // ChannelClose event may also happen when the other party close socket
    // first and Disconnected occurs
    // Should consider that.
    @Override
    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        final Channel channel = e.getChannel();
        logger.debug("channelClosed channel:{}", channel);

        super.channelClosed(ctx, e);
    }

    private boolean isExpectedShutdown(ChannelContext channelContext) {
        PinpointServerSocketStateCode currentState = channelContext.getCurrentStateCode();
        if (PinpointServerSocketStateCode.BEING_SHUTDOWN == currentState || released) {
            return true;
        }
        return false;
    }

    private boolean isIgnoreAddress(Channel channel) {
        if (ignoreAddressList == null) {
            return false;
        }
        final InetSocketAddress remoteAddress = (InetSocketAddress) channel.getRemoteAddress();
        if (remoteAddress == null) {
            return false;
        }
        InetAddress address = remoteAddress.getAddress();
        for (InetAddress ignore : ignoreAddressList) {
            if (ignore.equals(address)) {
                return true;
            }
        }
        return false;
    }

    private ChannelContext getChannelContext(Channel channel) {
        return (ChannelContext) channel.getAttachment();
    }

    private void prepareChannel(Channel channel) {
        SocketChannel socketChannel = new SocketChannel(channel, DEFAULT_TIMEOUTMILLIS, requestManagerTimer);
        StreamChannelManager streamChannelManager = new StreamChannelManager(channel, IDGenerator.createEvenIdGenerator(), serverStreamChannelMessageListener);

        ChannelContext channelContext = new ChannelContext(socketChannel, streamChannelManager, channelStateChangeEventHandler);

        channel.setAttachment(channelContext);

        channelGroup.add(channel);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        logger.error("Unexpected Exception happened. event:{}", e, e.getCause());
        Channel channel = e.getChannel();
        channel.close();
    }

    public void bind(String host, int port) throws PinpointSocketException {
        if (released) {
            return;
        }

        InetSocketAddress address = new InetSocketAddress(host, port);
        this.serverChannel = bootstrap.bind(address);
        sendPing();
    }

    private void sendPing() {
        logger.debug("sendPing");
        final TimerTask pintTask = new TimerTask() {
            @Override
            public void run(Timeout timeout) throws Exception {
                if (timeout.isCancelled()) {
                    newPingTimeout(this);
                    return;
                }

                final ChannelGroupFuture write = channelGroup.write(PingPacket.PING_PACKET);
                if (logger.isWarnEnabled()) {
                    write.addListener(new ChannelGroupFutureListener() {
                        private final ChannelFutureListener listener = new WriteFailFutureListener(logger, "ping write fail", "ping write success");

                        @Override
                        public void operationComplete(ChannelGroupFuture future) throws Exception {

                            if (logger.isWarnEnabled()) {
                                for (ChannelFuture channelFuture : future) {
                                    channelFuture.addListener(listener);
                                }
                            }
                        }
                    });
                }
                newPingTimeout(this);
            }
        };
        newPingTimeout(pintTask);
    }

    private void newPingTimeout(TimerTask pintTask) {
        try {
            logger.debug("newPingTimeout");
            pingTimer.newTimeout(pintTask, 1000 * 60 * 5, TimeUnit.MILLISECONDS);
        } catch (IllegalStateException e) {
            // stop in case of timer stopped
            logger.debug("timer stopped. Caused:{}", e.getMessage());
        }
    }

    public void close() {
        synchronized (this) {
            if (released) {
                return;
            }
            released = true;
        }
        sendServerClosedPacket();

        pingTimer.stop();

        if (serverChannel != null) {
            ChannelFuture close = serverChannel.close();
            close.awaitUninterruptibly(3000, TimeUnit.MILLISECONDS);
            serverChannel = null;
        }
        if (bootstrap != null) {
            bootstrap.releaseExternalResources();
            bootstrap = null;
        }

        // clear the request first and remove timer
        requestManagerTimer.stop();
    }

    private void sendServerClosedPacket() {
        logger.info("sendServerClosedPacket start");
        final ChannelGroupFuture write = this.channelGroup.write(new ServerClosePacket());
        write.awaitUninterruptibly(5000, TimeUnit.MILLISECONDS);
        if (logger.isWarnEnabled()) {
            write.addListener(new ChannelGroupFutureListener() {
                private final ChannelFutureListener listener = new WriteFailFutureListener(logger, "serverClosePacket write fail", "serverClosePacket write success");

                @Override
                public void operationComplete(ChannelGroupFuture future) throws Exception {
                    for (ChannelFuture channelFuture : future) {
                        channelFuture.addListener(listener);
                    }
                }
            });
        }
        logger.info("sendServerClosedPacket end");
    }

    public List<ChannelContext> getDuplexCommunicationChannelContext() {
        List<ChannelContext> channelContextList = new ArrayList<ChannelContext>();

        for (Channel channel : channelGroup) {
            ChannelContext context = getChannelContext(channel);

            if (context.getCurrentStateCode() == PinpointServerSocketStateCode.RUN_DUPLEX) {
                channelContextList.add(context);
            }
        }

        return channelContextList;
    }

}
