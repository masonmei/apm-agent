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

package com.baidu.oped.apm.profiler.monitor.codahale.gc;

import com.baidu.oped.apm.profiler.monitor.codahale.AgentStatCollectorFactory;
import com.baidu.oped.apm.profiler.monitor.codahale.gc.GarbageCollector;
import com.baidu.oped.apm.thrift.dto.TJvmGc;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GarbageCollectorFactoryTest {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Test
    public void test() {
        GarbageCollector collector = new AgentStatCollectorFactory().getGarbageCollector();

        logger.debug("collector.getType():{}", collector);
        TJvmGc collect1 = collector.collect();
        logger.debug("collector.collect():{}", collect1);
        TJvmGc collect2 = collector.collect();
        logger.debug("collector.collect():{}", collect2);
    }
}
