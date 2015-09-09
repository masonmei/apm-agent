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

package com.baidu.oped.apm.profiler.receiver;

import org.apache.thrift.TBase;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.jboss.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baidu.oped.apm.common.Version;
import com.baidu.oped.apm.rpc.client.MessageListener;
import com.baidu.oped.apm.rpc.packet.RequestPacket;
import com.baidu.oped.apm.rpc.packet.ResponsePacket;
import com.baidu.oped.apm.rpc.packet.SendPacket;
import com.baidu.oped.apm.rpc.packet.stream.StreamClosePacket;
import com.baidu.oped.apm.rpc.packet.stream.StreamCreateFailPacket;
import com.baidu.oped.apm.rpc.packet.stream.StreamCreatePacket;
import com.baidu.oped.apm.rpc.stream.ServerStreamChannelContext;
import com.baidu.oped.apm.rpc.stream.ServerStreamChannelMessageListener;
import com.baidu.oped.apm.rpc.util.AssertUtils;
import com.baidu.oped.apm.thrift.dto.TResult;
import com.baidu.oped.apm.thrift.io.DeserializerFactory;
import com.baidu.oped.apm.thrift.io.HeaderTBaseDeserializer;
import com.baidu.oped.apm.thrift.io.HeaderTBaseDeserializerFactory;
import com.baidu.oped.apm.thrift.io.HeaderTBaseSerializer;
import com.baidu.oped.apm.thrift.io.HeaderTBaseSerializerFactory;
import com.baidu.oped.apm.thrift.io.SerializerFactory;
import com.baidu.oped.apm.thrift.io.TCommandRegistry;
import com.baidu.oped.apm.thrift.io.TCommandTypeVersion;
import com.baidu.oped.apm.thrift.io.ThreadLocalHeaderTBaseDeserializerFactory;
import com.baidu.oped.apm.thrift.io.ThreadLocalHeaderTBaseSerializerFactory;
import com.baidu.oped.apm.thrift.util.SerializationUtils;

public class CommandDispatcher implements MessageListener, ServerStreamChannelMessageListener  {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final ProfilerCommandServiceRegistry commandServiceRegistry = new ProfilerCommandServiceRegistry();
    
    private final SerializerFactory<HeaderTBaseSerializer> serializerFactory;
    private final DeserializerFactory<HeaderTBaseDeserializer> deserializerFactory;

    public CommandDispatcher() {
        this(Version.VERSION);
    }

    public CommandDispatcher(String pinpointVersion) {
        this(pinpointVersion, HeaderTBaseSerializerFactory.DEFAULT_UDP_STREAM_MAX_SIZE);
    }

    public CommandDispatcher(String pinpointVersion, int serializationMaxSize) {
        TProtocolFactory protocolFactory = new TCompactProtocol.Factory();
        TCommandRegistry commandTbaseRegistry = new TCommandRegistry(TCommandTypeVersion.getVersion(pinpointVersion));
        
        SerializerFactory<HeaderTBaseSerializer> serializerFactory = new HeaderTBaseSerializerFactory(true, serializationMaxSize, protocolFactory, commandTbaseRegistry);
        this.serializerFactory = wrappedThreadLocalSerializerFactory(serializerFactory);
        AssertUtils.assertNotNull(this.serializerFactory);

        DeserializerFactory<HeaderTBaseDeserializer> deserializerFactory = new HeaderTBaseDeserializerFactory(protocolFactory, commandTbaseRegistry);
        this.deserializerFactory = wrappedThreadLocalDeserializerFactory(deserializerFactory);
        AssertUtils.assertNotNull(this.deserializerFactory);
    }

    @Override
    public void handleSend(SendPacket sendPacket, Channel channel) {
        logger.info("MessageReceive {} {}", sendPacket, channel);
    }

    @Override
    public void handleRequest(RequestPacket requestPacket, Channel channel) {
        logger.info("MessageReceive {} {}", requestPacket, channel);

        final TBase<?, ?> request = SerializationUtils.deserialize(requestPacket.getPayload(), deserializerFactory, null);
        
        TBase response;
        if (request == null) {

            final TResult tResult = new TResult(false);
            tResult.setMessage("Unsupported Type.");
            
            response = tResult;
        } else {
            final ProfilerRequestCommandService service = commandServiceRegistry.getRequestService(request);
            if (service == null) {
                TResult tResult = new TResult(false);
                tResult.setMessage("Unsupported Listener.");

                response = tResult;
            } else {
                response = service.requestCommandService(request);
            }
        }
        
        final byte[] payload = SerializationUtils.serialize(response, serializerFactory, null);
        if (payload != null) {
            channel.write(new ResponsePacket(requestPacket.getRequestId(), payload));
        }
    }

    @Override
    public short handleStreamCreate(ServerStreamChannelContext streamChannelContext, StreamCreatePacket packet) {
        logger.info("MessageReceived handleStreamCreate {} {}", packet, streamChannelContext);

        final TBase<?, ?> request = SerializationUtils.deserialize(packet.getPayload(), deserializerFactory, null);
        
        final ProfilerStreamCommandService service = commandServiceRegistry.getStreamService(request);
        if (service == null) {
            return StreamCreateFailPacket.PACKET_UNSUPPORT;
        }
        
        service.streamCommandService(request, streamChannelContext);
        
        return StreamCreatePacket.SUCCESS;
    }

    @Override
    public void handleStreamClose(ServerStreamChannelContext streamChannelContext, StreamClosePacket packet) {
    }

    public boolean registerCommandService(ProfilerCommandService commandService) {
        if (commandService == null) {
            throw new NullPointerException("commandService must not be null");
        }
        return this.commandServiceRegistry.addService(commandService);
    }

    private SerializerFactory<HeaderTBaseSerializer> wrappedThreadLocalSerializerFactory(SerializerFactory<HeaderTBaseSerializer> serializerFactory) {
        return new ThreadLocalHeaderTBaseSerializerFactory<HeaderTBaseSerializer>(serializerFactory);
    }

    private DeserializerFactory<HeaderTBaseDeserializer> wrappedThreadLocalDeserializerFactory(DeserializerFactory<HeaderTBaseDeserializer> deserializerFactory) {
        return new ThreadLocalHeaderTBaseDeserializerFactory<HeaderTBaseDeserializer>(deserializerFactory);
    }

}
