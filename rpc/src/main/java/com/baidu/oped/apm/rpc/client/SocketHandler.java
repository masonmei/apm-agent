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

import java.net.SocketAddress;

import com.baidu.oped.apm.rpc.Future;
import com.baidu.oped.apm.rpc.ResponseMessage;
import com.baidu.oped.apm.rpc.stream.ClientStreamChannelContext;
import com.baidu.oped.apm.rpc.stream.ClientStreamChannelMessageListener;
import com.baidu.oped.apm.rpc.stream.StreamChannelContext;

/**
 * @author emeroad
 * @author netspider
 */
public interface SocketHandler {

    void setConnectSocketAddress(SocketAddress address);

    void open();

    void initReconnect();

    void setPinpointSocket(PinpointSocket pinpointSocket);

    void sendSync(byte[] bytes);

    Future sendAsync(byte[] bytes);

    void close();

    void send(byte[] bytes);

    Future<ResponseMessage> request(byte[] bytes);

    ClientStreamChannelContext createStreamChannel(byte[] payload, ClientStreamChannelMessageListener clientStreamChannelMessageListener);

    StreamChannelContext findStreamChannel(int streamChannelId);
    
    void sendPing();

    boolean isConnected();

    boolean isSupportServerMode();

    void doHandshake();

}
