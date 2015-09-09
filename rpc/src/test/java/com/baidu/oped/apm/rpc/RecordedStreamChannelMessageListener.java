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

package com.baidu.oped.apm.rpc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baidu.oped.apm.rpc.packet.stream.StreamClosePacket;
import com.baidu.oped.apm.rpc.packet.stream.StreamResponsePacket;
import com.baidu.oped.apm.rpc.stream.ClientStreamChannelContext;
import com.baidu.oped.apm.rpc.stream.ClientStreamChannelMessageListener;

/**
 * @author emeroad
 * @author koo.taejin
 */
public class RecordedStreamChannelMessageListener implements ClientStreamChannelMessageListener {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final CountDownLatch latch;

    private final List<byte[]> receivedMessageList = Collections.synchronizedList(new ArrayList<byte[]>());

    public RecordedStreamChannelMessageListener(int receiveMessageCount) {
        this.latch = new CountDownLatch(receiveMessageCount);
    }

    @Override
    public void handleStreamData(ClientStreamChannelContext streamChannelContext, StreamResponsePacket packet) {
        logger.info("handleStreamData {}, {}", streamChannelContext, packet);
        receivedMessageList.add(packet.getPayload());
        latch.countDown();
    }

    @Override
    public void handleStreamClose(ClientStreamChannelContext streamChannelContext, StreamClosePacket packet) {
        logger.info("handleClose {}, {}", streamChannelContext, packet);
        receivedMessageList.add(packet.getPayload());
        latch.countDown();
    }

    public CountDownLatch getLatch() {
        return latch;
    }

    public List<byte[]> getReceivedMessage() {
        return receivedMessageList;
    }

}

