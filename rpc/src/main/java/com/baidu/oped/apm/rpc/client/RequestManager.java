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

import com.baidu.oped.apm.rpc.*;
import com.baidu.oped.apm.rpc.packet.RequestPacket;
import com.baidu.oped.apm.rpc.packet.ResponsePacket;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.util.Timeout;
import org.jboss.netty.util.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author emeroad
 */
public class RequestManager {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final AtomicInteger requestId = new AtomicInteger(1);

    private final ConcurrentMap<Integer, DefaultFuture<ResponseMessage>> requestMap = new ConcurrentHashMap<Integer, DefaultFuture<ResponseMessage>>();
    // Have to move Timer into factory?
    private final Timer timer;



    public RequestManager(Timer timer) {
        if (timer == null) {
            throw new NullPointerException("timer must not be null");
        }
        this.timer = timer;
    }


    private FailureEventHandler createFailureEventHandler(final int requestId) {
        FailureEventHandler failureEventHandler = new FailureEventHandler() {
            @Override
            public boolean fireFailure() {
                DefaultFuture<ResponseMessage> future = removeMessageFuture(requestId);
                if (future != null) {
                    // removed perfectly.
                    return true;
                }
                return false;
            }
        };
        return failureEventHandler;
    }


    private void addTimeoutTask(long timeoutMillis, DefaultFuture future) {
        if (future == null) {
            throw new NullPointerException("future");
        }
        try {
            Timeout timeout = timer.newTimeout(future, timeoutMillis, TimeUnit.MILLISECONDS);
            future.setTimeout(timeout);
        } catch (IllegalStateException e) {
            // this case is that timer has been shutdown. That maybe just means that socket has been closed.
            future.setFailure(new PinpointSocketException("socket closed")) ;
        }
    }

    private int getNextRequestId() {
        return this.requestId.getAndIncrement();
    }


    public void messageReceived(ResponsePacket responsePacket, Channel channel) {
        final int requestId = responsePacket.getRequestId();
        final DefaultFuture<ResponseMessage> future = removeMessageFuture(requestId);
        if (future == null) {
            logger.warn("future not found:{}, channel:{}", responsePacket, channel);
            return;
        } else {
            logger.debug("responsePacket arrived packet:{}, channel:{}", responsePacket, channel);
        }

        ResponseMessage response = new ResponseMessage();
        response.setMessage(responsePacket.getPayload());
        future.setResult(response);
    }

    public DefaultFuture<ResponseMessage> removeMessageFuture(int requestId) {
        return this.requestMap.remove(requestId);
    }

    public void messageReceived(RequestPacket requestPacket, Channel channel) {
        logger.error("unexpectedMessage received:{} address:{}", requestPacket, channel.getRemoteAddress());
    }



    public ChannelWriteFailListenableFuture<ResponseMessage> register(RequestPacket requestPacket, long timeoutMillis) {
        // shutdown check
        final int requestId = getNextRequestId();
        requestPacket.setRequestId(requestId);

        final ChannelWriteFailListenableFuture<ResponseMessage> future = new ChannelWriteFailListenableFuture<ResponseMessage>(timeoutMillis);

        final DefaultFuture old = this.requestMap.put(requestId, future);
        if (old != null) {
            throw new PinpointSocketException("unexpected error. old future exist:" + old + " id:" + requestId);
        }

        // when future fails, put a handle in order to remove a failed future in the requestMap.
        FailureEventHandler removeTable = createFailureEventHandler(requestId);
        future.setFailureEventHandler(removeTable);

        addTimeoutTask(timeoutMillis, future);
        return future;
    }


    public void close() {
        logger.debug("close()");
        final PinpointSocketException closed = new PinpointSocketException("socket closed");

        // Could you handle race conditions of "close" more precisely?
//        final Timer timer = this.timer;
//        if (timer != null) {
//            Set<Timeout> stop = timer.stop();
//            for (Timeout timeout : stop) {
//                DefaultFuture future = (DefaultFuture)timeout.getTask();
//                future.setFailure(closed);
//            }
//        }
        int requestFailCount = 0;
        for (Map.Entry<Integer, DefaultFuture<ResponseMessage>> entry : requestMap.entrySet()) {
            if(entry.getValue().setFailure(closed)) {
                requestFailCount++;
            }
        }
        this.requestMap.clear();
        if (requestFailCount > 0) {
            logger.info("requestManager failCount:{}", requestFailCount);
        }

    }

}

