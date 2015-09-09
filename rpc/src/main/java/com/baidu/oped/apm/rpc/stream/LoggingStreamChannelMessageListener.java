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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baidu.oped.apm.rpc.packet.stream.StreamClosePacket;
import com.baidu.oped.apm.rpc.packet.stream.StreamCreatePacket;
import com.baidu.oped.apm.rpc.packet.stream.StreamResponsePacket;

/**
 * @author koo.taejin
 */
public class LoggingStreamChannelMessageListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingStreamChannelMessageListener.class);

    public static final ServerStreamChannelMessageListener SERVER_LISTENER = new Server();
    public static final ClientStreamChannelMessageListener CLIENT_LISTENER = new Client();

    static class Server implements ServerStreamChannelMessageListener {

        @Override
        public short handleStreamCreate(ServerStreamChannelContext streamChannelContext, StreamCreatePacket packet) {
            LOGGER.info("handleStreamCreate StreamChannel:{}, Packet:{}", streamChannelContext, packet);
            return 0;
        }

        @Override
        public void handleStreamClose(ServerStreamChannelContext streamChannelContext, StreamClosePacket packet) {
            LOGGER.info("handleStreamClose StreamChannel:{}, Packet:{}", streamChannelContext, packet);
        }

    }

    static class Client implements ClientStreamChannelMessageListener {

        @Override
        public void handleStreamData(ClientStreamChannelContext streamChannelContext, StreamResponsePacket packet) {
            LOGGER.info("handleStreamData StreamChannel:{}, Packet:{}", streamChannelContext, packet);
        }

        @Override
        public void handleStreamClose(ClientStreamChannelContext streamChannelContext, StreamClosePacket packet) {
            LOGGER.info("handleStreamClose StreamChannel:{}, Packet:{}", streamChannelContext, packet);
        }

    }
}
