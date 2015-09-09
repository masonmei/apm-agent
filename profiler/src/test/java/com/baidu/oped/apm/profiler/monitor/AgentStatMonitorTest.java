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

package com.baidu.oped.apm.profiler.monitor;

import static org.junit.Assert.*;

import com.baidu.oped.apm.profiler.monitor.AgentStatMonitor;
import com.baidu.oped.apm.test.PeekableDataSender;
import com.baidu.oped.apm.thrift.dto.TAgentStatBatch;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author hyungil.jeong
 */
public class AgentStatMonitorTest {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private PeekableDataSender<TAgentStatBatch> peekableDataSender;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        this.peekableDataSender = new PeekableDataSender<TAgentStatBatch>();
    }

    @Test
    public void testAgentStatMonitor() throws InterruptedException {
        // Given
        final long collectionIntervalMs = 1000 * 1;
        final int numCollectionsPerBatch = 2;
        final int minNumBatchToTest = 2;
        final long totalTestDurationMs = collectionIntervalMs * numCollectionsPerBatch * minNumBatchToTest;
        // When
        System.setProperty("pinpoint.log", "test.");
        AgentStatMonitor monitor = new AgentStatMonitor(this.peekableDataSender, "agentId", System.currentTimeMillis(), collectionIntervalMs,
                numCollectionsPerBatch);
        monitor.start();
        Thread.sleep(totalTestDurationMs);
        monitor.stop();
        // Then
        assertTrue(peekableDataSender.size() >= minNumBatchToTest);
        for (TAgentStatBatch agentStatBatch : peekableDataSender) {
            logger.debug("agentStatBatch:{}", agentStatBatch);
            assertTrue(agentStatBatch.getAgentStats().size() <= numCollectionsPerBatch);
        }
    }

}
