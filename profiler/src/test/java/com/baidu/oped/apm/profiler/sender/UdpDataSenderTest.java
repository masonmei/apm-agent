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

package com.baidu.oped.apm.profiler.sender;

import com.baidu.oped.apm.profiler.logging.Slf4jLoggerBinderInitializer;
import com.baidu.oped.apm.profiler.sender.UdpDataSender;
import com.baidu.oped.apm.thrift.dto.TAgentInfo;

import org.junit.Assert;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.thrift.TBase;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author emeroad
 */
public class UdpDataSenderTest {
    @BeforeClass
    public static void before() {
        Slf4jLoggerBinderInitializer.beforeClass();
    }

    @AfterClass
    public static void after() {
        Slf4jLoggerBinderInitializer.afterClass();
    }



    @Test
    public void sendAndFlushChck() throws InterruptedException {
        UdpDataSender sender = new UdpDataSender("localhost", 9009, "test", 128, 1000, 1024*64*100);

        TAgentInfo agentInfo = new TAgentInfo();
        sender.send(agentInfo);
        sender.send(agentInfo);
        sender.send(agentInfo);
        sender.send(agentInfo);
        sender.send(agentInfo);
        sender.send(agentInfo);
        sender.send(agentInfo);
        sender.send(agentInfo);
        sender.send(agentInfo);
        sender.send(agentInfo);
        sender.send(agentInfo);
        sender.send(agentInfo);
        sender.send(agentInfo);
        sender.stop();
    }

    @Test
    public void sendAndLarge() throws InterruptedException {
        String random = RandomStringUtils.randomAlphabetic(UdpDataSender.UDP_MAX_PACKET_LENGTH);
        TAgentInfo agentInfo = new TAgentInfo();
        agentInfo.setAgentId(random);
        boolean limit = sendMessage_getLimit(agentInfo);
        Assert.assertTrue("limit overflow",limit);

        boolean noLimit = sendMessage_getLimit(new TAgentInfo());
        Assert.assertFalse("success", noLimit);


    }

    private boolean sendMessage_getLimit(TBase tbase) throws InterruptedException {
        final AtomicBoolean limitCounter = new AtomicBoolean(false);
        final CountDownLatch latch = new CountDownLatch(1);

        UdpDataSender sender = new UdpDataSender("localhost", 9009, "test", 128, 1000, 1024*64*100) {
            @Override
            protected boolean isLimit(int interBufferSize) {
                boolean limit = super.isLimit(interBufferSize);
                limitCounter.set(limit);
                latch.countDown();
                return limit;
            }
        };
        try {
            sender.send(tbase);
            latch.await(5000, TimeUnit.MILLISECONDS);
        } finally {
            sender.stop();
        }
        return limitCounter.get();
    }

}
