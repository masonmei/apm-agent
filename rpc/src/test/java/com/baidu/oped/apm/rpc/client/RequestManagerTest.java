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

import com.baidu.oped.apm.rpc.DefaultFuture;
import com.baidu.oped.apm.rpc.Future;
import com.baidu.oped.apm.rpc.client.RequestManager;
import com.baidu.oped.apm.rpc.packet.RequestPacket;

import org.jboss.netty.util.HashedWheelTimer;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * @author emeroad
 */
public class RequestManagerTest {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());


    @Test
    public void testRegisterRequest() throws Exception {
        HashedWheelTimer timer = getTimer();
        RequestManager requestManager = new RequestManager(timer);
        try {
            RequestPacket packet = new RequestPacket(new byte[0]);
            Future future = requestManager.register(packet, 50);
            Thread.sleep(200);

            Assert.assertTrue(future.isReady());
            Assert.assertFalse(future.isSuccess());
            Assert.assertTrue(future.getCause().getMessage().contains("timeout"));
            logger.debug(future.getCause().getMessage());
        } finally {
            requestManager.close();
            timer.stop();
        }
    }

    @Test
    public void testRemoveMessageFuture() throws Exception {
        HashedWheelTimer timer = getTimer();
        RequestManager requestManager = new RequestManager(timer);
        try {
            RequestPacket packet = new RequestPacket(1, new byte[0]);
            DefaultFuture future = requestManager.register(packet, 2000);

            future.setFailure(new RuntimeException());

            Future nullFuture = requestManager.removeMessageFuture(packet.getRequestId());
            Assert.assertNull(nullFuture);


        } finally {
            requestManager.close();
            timer.stop();
        }

    }

    private HashedWheelTimer getTimer() {
        return new HashedWheelTimer(10, TimeUnit.MICROSECONDS);
    }

    //    @Test
    public void testTimerStartTiming() throws InterruptedException {
        HashedWheelTimer timer = new HashedWheelTimer(1000, TimeUnit.MILLISECONDS);
        timer.start();
        timer.stop();
    }

    @Test
    public void testClose() throws Exception {

    }
}
