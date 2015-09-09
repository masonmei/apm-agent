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
import com.baidu.oped.apm.rpc.PinpointSocketException;
import com.baidu.oped.apm.rpc.ResponseMessage;
import com.baidu.oped.apm.rpc.stream.ClientStreamChannelContext;
import com.baidu.oped.apm.rpc.stream.ClientStreamChannelMessageListener;
import com.baidu.oped.apm.rpc.stream.StreamChannelContext;

import java.net.SocketAddress;

/**
 * @author emeroad
 * @author netspider
 */
public class ReconnectStateSocketHandler implements SocketHandler {


    @Override
    public void setConnectSocketAddress(SocketAddress connectSocketAddress) {
    }

    @Override
    public void open() {
        throw new IllegalStateException();
    }

    @Override
    public void initReconnect() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setPinpointSocket(PinpointSocket pinpointSocket) {
    }

    @Override
    public void sendSync(byte[] bytes) {
        throw newReconnectException();
    }

    @Override
    public Future sendAsync(byte[] bytes) {
        return reconnectFailureFuture();
    }

    private DefaultFuture<ResponseMessage> reconnectFailureFuture() {
        DefaultFuture<ResponseMessage> reconnect = new DefaultFuture<ResponseMessage>();
        reconnect.setFailure(newReconnectException());
        return reconnect;
    }

    @Override
    public void close() {
    }

    @Override
    public void send(byte[] bytes) {
    }

    private PinpointSocketException newReconnectException() {
        return new PinpointSocketException("reconnecting...");
    }

    @Override
    public Future<ResponseMessage> request(byte[] bytes) {
        return reconnectFailureFuture();
    }

    @Override
    public ClientStreamChannelContext createStreamChannel(byte[] payload, ClientStreamChannelMessageListener clientStreamChannelMessageListener) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public StreamChannelContext findStreamChannel(int streamChannelId) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void sendPing() {
    }

    @Override
    public boolean isConnected() {
        return false;
    }

    @Override
    public boolean isSupportServerMode() {
        return false;
    }

    @Override
    public void doHandshake() {
//        throw new UnsupportedOperationException();
    }

}
