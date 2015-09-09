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

package com.baidu.oped.apm.profiler.modifier.redis;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Test;

import com.baidu.oped.apm.common.bo.SpanEventBo;
import com.baidu.oped.apm.test.junit4.BasePinpointTest;

import redis.clients.jedis.Client;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

public class JedisModifierTest extends BasePinpointTest {
    private static final String HOST = "localhost";
    private static final int PORT = 6379;
    
    @Test
    public void jedis() {
        JedisMock jedis = new JedisMock("localhost", 6379);
        try {
            jedis.get("foo");
        } finally {
            if(jedis != null) {
                jedis.close();
            }
        }
        final List<SpanEventBo> events = getCurrentSpanEvents();
        assertEquals(1, events.size());
        
        final SpanEventBo eventBo = events.get(0);
        assertEquals(HOST + ":" + PORT, eventBo.getEndPoint());
        assertEquals("REDIS", eventBo.getDestinationId());
        
    }
    
    @Test
    public void binaryJedis() {
        JedisMock jedis = new JedisMock("localhost", 6379);
        try {
            jedis.get("foo".getBytes());
        } finally {
            if(jedis != null) {
                jedis.close();
            }
        }
        final List<SpanEventBo> events = getCurrentSpanEvents();
        assertEquals(1, events.size());
        
        final SpanEventBo eventBo = events.get(0);
        assertEquals(HOST + ":" + PORT, eventBo.getEndPoint());
        assertEquals("REDIS", eventBo.getDestinationId());
    }

    
    @Test
    public void pipeline() {
        JedisMock jedis = new JedisMock("localhost", 6379);
        try {
            Pipeline pipeline = jedis.pipelined();
            pipeline.get("foo");
        } finally {
            if(jedis != null) {
                jedis.close();
            }
        }
        
        final List<SpanEventBo> events = getCurrentSpanEvents();
        assertEquals(1, events.size());
    }
    
    
    public class JedisMock extends Jedis {
        public JedisMock(String host, int port) {
            super(host, port);

            client = mock(Client.class);

            // for 'get' command
            when(client.isInMulti()).thenReturn(false);
            when(client.getBulkReply()).thenReturn("bar");
            when(client.getBinaryBulkReply()).thenReturn("bar".getBytes());
        }
    }
}
