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

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;

import com.baidu.oped.apm.rpc.packet.stream.StreamCreateSuccessPacket;
import com.baidu.oped.apm.rpc.packet.stream.StreamResponsePacket;

/**
 * @author koo.taejin
 */
public class ServerStreamChannel extends StreamChannel {

    public ServerStreamChannel(Channel channel, int streamId, StreamChannelManager streamChannelManager) {
        super(channel, streamId, streamChannelManager);
    }

    public ChannelFuture sendData(byte[] payload) {
        assertState(StreamChannelStateCode.RUN);

        StreamResponsePacket dataPacket = new StreamResponsePacket(getStreamId(), payload);
        return this.getChannel().write(dataPacket);
    }

    public ChannelFuture sendCreateSuccess() {
        assertState(StreamChannelStateCode.RUN);

        StreamCreateSuccessPacket packet = new StreamCreateSuccessPacket(getStreamId());
        return this.getChannel().write(packet);
    }

    boolean changeStateOpenArrived() {
        boolean result = getState().changeStateOpenArrived();

        logger.info(makeStateChangeMessage(StreamChannelStateCode.OPEN_ARRIVED, result));
        return result;
    }

}
