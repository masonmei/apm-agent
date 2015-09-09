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

package com.baidu.oped.apm.rpc.stream;

import com.baidu.oped.apm.rpc.PinpointSocketException;
import com.baidu.oped.apm.rpc.packet.stream.BasicStreamPacket;
import com.baidu.oped.apm.rpc.packet.stream.StreamPingPacket;
import com.baidu.oped.apm.rpc.packet.stream.StreamPongPacket;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author koo.taejin
 */
public abstract class StreamChannel {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Channel channel;
    private final int streamChannelId;
    private final StreamChannelManager streamChannelManager;

    private final StreamChannelState state;
    private final CountDownLatch openLatch = new CountDownLatch(1);

    public StreamChannel(Channel channel, int streamId, StreamChannelManager streamChannelManager) {
        this.channel = channel;
        this.streamChannelId = streamId;
        this.streamChannelManager = streamChannelManager;

        this.state = new StreamChannelState();
    }

    boolean changeStateRun() {
        try {
            boolean result = state.changeStateRun();

            logger.info(makeStateChangeMessage(StreamChannelStateCode.RUN, result));
            return result;
        } finally {
            openLatch.countDown();
        }
    }

    boolean changeStateClose() {
        try {
            if (checkState(StreamChannelStateCode.CLOSED)) {
                return true;
            }

            boolean result = state.changeStateClose();

            logger.info(makeStateChangeMessage(StreamChannelStateCode.CLOSED, result));
            return result;
        } finally {
            openLatch.countDown();
        }
    }

    public boolean awaitOpen() {
        try {
            openLatch.await();
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return false;
    }

    public boolean awaitOpen(long timeoutMillis) {
        try {
            return openLatch.await(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return false;
    }

    public StreamChannelStateCode getCurrentState() {
        return state.getCurrentState();
    }

    public ChannelFuture sendPing(int requestId) {
        assertState(StreamChannelStateCode.RUN);

        StreamPingPacket packet = new StreamPingPacket(streamChannelId, requestId);
        return this.channel.write(packet);
    }

    public ChannelFuture sendPong(int requestId) {
        assertState(StreamChannelStateCode.RUN);

        StreamPongPacket packet = new StreamPongPacket(streamChannelId, requestId);
        return this.channel.write(packet);
    }

    public void close() {
        this.streamChannelManager.clearResourceAndSendClose(getStreamId(), BasicStreamPacket.CHANNEL_CLOSE);
    }

    public Channel getChannel() {
        return channel;
    }

    public int getStreamId() {
        return streamChannelId;
    }

    protected StreamChannelState getState() {
        return state;
    }

    protected String makeStateChangeMessage(StreamChannelStateCode change, boolean result) {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getClass().getSimpleName());

        sb.append(" change state to ");
        sb.append(change.name());
        sb.append("(");
        sb.append(result ? "SUCCESS" : "FAIL");
        sb.append(")");

        sb.append("[Channel:");
        sb.append(channel);

        sb.append(", StreamId:");
        sb.append(getStreamId());

        sb.append(".");

        return sb.toString();
    }

    public boolean isServer() {
        if (this instanceof ServerStreamChannel) {
            return true;
        }

        return false;
    }

    void assertState(StreamChannelStateCode stateCode) {
        final StreamChannelStateCode currentCode = getCurrentState();
        if (!checkState(currentCode, stateCode)) {
            throw new PinpointSocketException("expected:<" + stateCode + "> but was:<" + currentCode + ">;");
        }
    }

    boolean checkState(StreamChannelStateCode expectedCode) {
        return checkState(getCurrentState(), expectedCode);
    }

    boolean checkState(StreamChannelStateCode currentCode, StreamChannelStateCode expectedCode) {
        if (currentCode == expectedCode) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getClass().getSimpleName());

        sb.append("[Channel:");
        sb.append(channel);

        sb.append(", StreamId:");
        sb.append(getStreamId());

        sb.append(", State:");
        sb.append(getCurrentState());

        sb.append("].");

        // TODO fix -> super.toString();
        return sb.toString();
    }

}
