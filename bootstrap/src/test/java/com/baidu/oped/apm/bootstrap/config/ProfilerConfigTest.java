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

package com.baidu.oped.apm.bootstrap.config;

import java.io.IOException;
import java.util.Properties;

import org.junit.Assert;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baidu.oped.apm.bootstrap.config.Filter;
import com.baidu.oped.apm.bootstrap.config.ProfilerConfig;

/**
 * @author emeroad
 */
public class ProfilerConfigTest {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());


    @Test
    public void defaultProfilableClassFilter() throws IOException {
        ProfilerConfig profilerConfig = new ProfilerConfig();
        Filter<String> profilableClassFilter = profilerConfig.getProfilableClassFilter();
        Assert.assertFalse(profilableClassFilter.filter("net/spider/king/wang/Jjang"));
    }

    @Test
    public void readProperty() throws IOException {
        String path = ProfilerConfig.class.getResource("/com/baidu/oped/apm/bootstrap/config/test.property").getPath();
        logger.debug("path:{}", path);

        ProfilerConfig profilerConfig = ProfilerConfig.load(path);
    }

    @Test
    public void testPlaceHolder() throws IOException {
        Properties properties = new Properties();
        properties.setProperty("profiler.collector.span.ip", "${test1}");
        properties.setProperty("profiler.collector.stat.ip", "${test1}");
        properties.setProperty("profiler.collector.tcp.ip", "${test2}");
        // placeHolderValue
        properties.setProperty("test1", "placeHolder1");
        properties.setProperty("test2", "placeHolder2");


        ProfilerConfig profilerConfig = new ProfilerConfig(properties);

        Assert.assertEquals(profilerConfig.getCollectorSpanServerIp(), "placeHolder1");
        Assert.assertEquals(profilerConfig.getCollectorStatServerIp(), "placeHolder1");
        Assert.assertEquals(profilerConfig.getCollectorTcpServerIp(), "placeHolder2");
    }


    @Test
    public void ioBuffering_test() throws IOException {
        String path = ProfilerConfig.class.getResource("/com/baidu/oped/apm/bootstrap/config/test.property").getPath();
        logger.debug("path:{}", path);

        ProfilerConfig profilerConfig = ProfilerConfig.load(path);

        Assert.assertEquals(profilerConfig.isIoBufferingEnable(), false);
        Assert.assertEquals(profilerConfig.getIoBufferingBufferSize(), 30);
    }

    @Test
    public void ioBuffering_default() throws IOException {
        String path = ProfilerConfig.class.getResource("/com/baidu/oped/apm/bootstrap/config/default.property").getPath();
        logger.debug("path:{}", path);

        ProfilerConfig profilerConfig = ProfilerConfig.load(path);

        Assert.assertEquals(profilerConfig.isIoBufferingEnable(), true);
        Assert.assertEquals(profilerConfig.getIoBufferingBufferSize(), 10);
    }
    
    @Test
    public void tcpCommandAcceptrConfigTest1() throws IOException {
        String path = ProfilerConfig.class.getResource("/com/baidu/oped/apm/bootstrap/config/test.property").getPath();
        logger.debug("path:{}", path);

        ProfilerConfig profilerConfig = ProfilerConfig.load(path);
        
        Assert.assertFalse(profilerConfig.isTcpDataSenderCommandAcceptEnable());
    }
    
    @Test
    public void tcpCommandAcceptrConfigTest2() throws IOException {
        String path = ProfilerConfig.class.getResource("/com/baidu/oped/apm/bootstrap/config/test2.property").getPath();
        logger.debug("path:{}", path);

        ProfilerConfig profilerConfig = ProfilerConfig.load(path);
        
        Assert.assertTrue(profilerConfig.isTcpDataSenderCommandAcceptEnable());
    }
    

}
