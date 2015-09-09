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

package com.baidu.oped.apm.rpc.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Map;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baidu.oped.apm.rpc.control.ProtocolException;
import com.baidu.oped.apm.rpc.packet.ControlHandshakePacket;
import com.baidu.oped.apm.rpc.packet.ControlHandshakeResponsePacket;
import com.baidu.oped.apm.rpc.packet.HandshakeResponseCode;
import com.baidu.oped.apm.rpc.packet.HandshakeResponseType;
import com.baidu.oped.apm.rpc.packet.RequestPacket;
import com.baidu.oped.apm.rpc.packet.ResponsePacket;
import com.baidu.oped.apm.rpc.packet.SendPacket;
import com.baidu.oped.apm.rpc.util.ControlMessageEncodingUtils;
import com.baidu.oped.apm.rpc.util.MapUtils;
import com.baidu.oped.apm.rpc.util.PinpointRPCTestUtils;

/**
 * @author koo.taejin
 */
public class ControlPacketServerTest {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static int bindPort;
    
    @BeforeClass
    public static void setUp() throws IOException {
        bindPort = PinpointRPCTestUtils.findAvailablePort();
    }

    // Test for being possible to send messages in case of failure of registering packet ( return code : 2, lack of parameter)
    @Test
    public void registerAgentTest1() throws Exception {
        PinpointServerSocket serverSocket = PinpointRPCTestUtils.createServerSocket(bindPort, new SimpleListener());

        Socket socket = null;
        try {
            socket = new Socket("127.0.0.1", bindPort);

            sendAndReceiveSimplePacket(socket);

            int code= sendAndReceiveRegisterPacket(socket);
            Assert.assertEquals(2, code);

            sendAndReceiveSimplePacket(socket);
        } finally {
            PinpointRPCTestUtils.close(socket);
            PinpointRPCTestUtils.close(serverSocket);
        }
    }

    // Test for being possible to send messages in case of success of registering packet ( return code : 0)
    @Test
    public void registerAgentTest2() throws Exception {
        PinpointServerSocket serverSocket = PinpointRPCTestUtils.createServerSocket(bindPort, new SimpleListener());

        Socket socket = null;
        try {
            socket = new Socket("127.0.0.1", bindPort);

            sendAndReceiveSimplePacket(socket);

            int code= sendAndReceiveRegisterPacket(socket, PinpointRPCTestUtils.getParams());
            Assert.assertEquals(0, code);

            sendAndReceiveSimplePacket(socket);
        } finally {
            PinpointRPCTestUtils.close(socket);
            PinpointRPCTestUtils.close(serverSocket);
        }
    }

    // when failure of registering and retrying to register, confirm to return same code ( return code : 2
    @Test
    public void registerAgentTest3() throws Exception {
        PinpointServerSocket serverSocket = PinpointRPCTestUtils.createServerSocket(bindPort, new SimpleListener());

        Socket socket = null;
        try {
            socket = new Socket("127.0.0.1", bindPort);
            int code = sendAndReceiveRegisterPacket(socket);
            Assert.assertEquals(2, code);

            code = sendAndReceiveRegisterPacket(socket);
            Assert.assertEquals(2, code);

            sendAndReceiveSimplePacket(socket);
        } finally {
            PinpointRPCTestUtils.close(socket);
            PinpointRPCTestUtils.close(serverSocket);
        }
    }

    // after success of registering, when success message are sent repeatedly.
    // test 1) confirm to return success code, 2) confirm to return already success code.
    @Test
    public void registerAgentTest4() throws Exception {
        PinpointServerSocket serverSocket = PinpointRPCTestUtils.createServerSocket(bindPort, new SimpleListener());

        Socket socket = null;
        try {
            socket = new Socket("127.0.0.1", bindPort);
            sendAndReceiveSimplePacket(socket);

            int code = sendAndReceiveRegisterPacket(socket, PinpointRPCTestUtils.getParams());
            Assert.assertEquals(0, code);

            sendAndReceiveSimplePacket(socket);

            code = sendAndReceiveRegisterPacket(socket, PinpointRPCTestUtils.getParams());
            Assert.assertEquals(1, code);

            sendAndReceiveSimplePacket(socket);
        } finally {
            PinpointRPCTestUtils.close(socket);
            PinpointRPCTestUtils.close(serverSocket);
        }
    }


    private int sendAndReceiveRegisterPacket(Socket socket) throws ProtocolException, IOException {
        return sendAndReceiveRegisterPacket(socket, Collections.EMPTY_MAP);
    }

    private int sendAndReceiveRegisterPacket(Socket socket, Map properties) throws ProtocolException, IOException {
        sendRegisterPacket(socket.getOutputStream(), properties);
        ControlHandshakeResponsePacket packet = receiveRegisterConfirmPacket(socket.getInputStream());
        Map<Object, Object> result = (Map<Object, Object>) ControlMessageEncodingUtils.decode(packet.getPayload());

        return MapUtils.getInteger(result, "code", -1);
    }

    private void sendAndReceiveSimplePacket(Socket socket) throws ProtocolException, IOException {
        sendSimpleRequestPacket(socket.getOutputStream());
        ResponsePacket responsePacket = readSimpleResponsePacket(socket.getInputStream());
        Assert.assertNotNull(responsePacket);
    }

    private void sendRegisterPacket(OutputStream outputStream, Map properties) throws ProtocolException, IOException {
        byte[] payload = ControlMessageEncodingUtils.encode(properties);
        ControlHandshakePacket packet = new ControlHandshakePacket(1, payload);

        ByteBuffer bb = packet.toBuffer().toByteBuffer(0, packet.toBuffer().writerIndex());
        sendData(outputStream, bb.array());
    }

    private void sendSimpleRequestPacket(OutputStream outputStream) throws ProtocolException, IOException {
        RequestPacket packet = new RequestPacket(new byte[0]);
        packet.setRequestId(10);

        ByteBuffer bb = packet.toBuffer().toByteBuffer(0, packet.toBuffer().writerIndex());
        sendData(outputStream, bb.array());
    }

    private void sendData(OutputStream outputStream, byte[] payload) throws IOException {
        outputStream.write(payload);
        outputStream.flush();
    }

    private ControlHandshakeResponsePacket receiveRegisterConfirmPacket(InputStream inputStream) throws ProtocolException, IOException {

        byte[] payload = readData(inputStream);
        ChannelBuffer cb = ChannelBuffers.wrappedBuffer(payload);

        short packetType = cb.readShort();

        ControlHandshakeResponsePacket packet = ControlHandshakeResponsePacket.readBuffer(packetType, cb);
        return packet;
    }

    private ResponsePacket readSimpleResponsePacket(InputStream inputStream) throws ProtocolException, IOException {
        byte[] payload = readData(inputStream);
        ChannelBuffer cb = ChannelBuffers.wrappedBuffer(payload);

        short packetType = cb.readShort();

        ResponsePacket packet = ResponsePacket.readBuffer(packetType, cb);
        return packet;
    }

    private byte[] readData(InputStream inputStream) throws IOException {
        int availableSize = 0;

        for (int i = 0; i < 3; i++) {
            availableSize = inputStream.available();

            if (availableSize > 0) {
                break;
            }

            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        byte[] payload = new byte[availableSize];
        inputStream.read(payload);

        return payload;
    }

    class SimpleListener implements ServerMessageListener {
        @Override
        public void handleSend(SendPacket sendPacket, SocketChannel channel) {

        }

        @Override
        public void handleRequest(RequestPacket requestPacket, SocketChannel channel) {
            logger.info("handlerRequest {} {}", requestPacket, channel);
            channel.sendResponseMessage(requestPacket, requestPacket.getPayload());
        }

        @Override
        public HandshakeResponseCode handleHandshake(Map properties) {
            if (properties == null) {
                return HandshakeResponseType.ProtocolError.PROTOCOL_ERROR;
            }

            boolean hasAllType = AgentHandshakePropertyType.hasAllType(properties);
            if (!hasAllType) {
                return HandshakeResponseType.PropertyError.PROPERTY_ERROR;
            }

            return HandshakeResponseType.Success.DUPLEX_COMMUNICATION;
        }
    }

}
