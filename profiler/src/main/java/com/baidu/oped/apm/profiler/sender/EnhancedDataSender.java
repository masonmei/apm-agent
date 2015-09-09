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

import com.baidu.oped.apm.rpc.FutureListener;
import com.baidu.oped.apm.rpc.ResponseMessage;
import com.baidu.oped.apm.rpc.client.PinpointSocketReconnectEventListener;

import org.apache.thrift.TBase;

/**
 * @author emeroad
 */
public interface EnhancedDataSender extends DataSender {

    boolean request(TBase<?, ?> data);
    boolean request(TBase<?, ?> data, int retry);
    boolean request(TBase<?, ?> data, FutureListener<ResponseMessage> listener);

    boolean addReconnectEventListener(PinpointSocketReconnectEventListener eventListener);
    boolean removeReconnectEventListener(PinpointSocketReconnectEventListener eventListener);

}
