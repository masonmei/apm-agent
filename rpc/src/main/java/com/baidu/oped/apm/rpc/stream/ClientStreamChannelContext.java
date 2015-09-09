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

import com.baidu.oped.apm.rpc.util.AssertUtils;

/**
 * @author koo.taejin
 */
public class ClientStreamChannelContext extends StreamChannelContext {

    private final ClientStreamChannel clientStreamChannel;
    private final ClientStreamChannelMessageListener clientStreamChannelMessageListener;

    public ClientStreamChannelContext(ClientStreamChannel clientStreamChannel, ClientStreamChannelMessageListener clientStreamChannelMessageListener) {
        AssertUtils.assertNotNull(clientStreamChannel);
        AssertUtils.assertNotNull(clientStreamChannelMessageListener);

        this.clientStreamChannel = clientStreamChannel;
        this.clientStreamChannelMessageListener = clientStreamChannelMessageListener;
    }

    @Override
    public ClientStreamChannel getStreamChannel() {
        return clientStreamChannel;
    }

    public ClientStreamChannelMessageListener getClientStreamChannelMessageListener() {
        return clientStreamChannelMessageListener;
    }

}
