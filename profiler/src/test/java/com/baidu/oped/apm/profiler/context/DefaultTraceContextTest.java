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

package com.baidu.oped.apm.profiler.context;

import com.baidu.oped.apm.bootstrap.context.Trace;
import com.baidu.oped.apm.bootstrap.context.TraceId;
import com.baidu.oped.apm.common.ServiceType;
import com.baidu.oped.apm.common.Version;
import com.baidu.oped.apm.common.util.TransactionId;
import com.baidu.oped.apm.common.util.TransactionIdUtils;
import com.baidu.oped.apm.profiler.AgentInformation;
import com.baidu.oped.apm.profiler.context.DefaultTraceContext;
import com.baidu.oped.apm.profiler.context.DefaultTraceId;
import com.baidu.oped.apm.profiler.util.RuntimeMXBeanUtils;

import org.junit.Assert;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author emeroad
 */
public class DefaultTraceContextTest {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Test
    public void parseTest() {
        String agent= "test";
        long agentStartTime = System.currentTimeMillis();
        long agentTransactionCount = 10;
        TraceId traceID = new DefaultTraceId(agent, agentStartTime, agentTransactionCount);

        String id = traceID.getTransactionId();
        logger.info("id={}", id);

        TransactionId transactionid = TransactionIdUtils.parseTransactionId(id);

        Assert.assertEquals(transactionid.getAgentId(), agent);
        Assert.assertEquals(transactionid.getAgentStartTime(), agentStartTime);
        Assert.assertEquals(transactionid.getTransactionSequence(), agentTransactionCount);

    }

    @Test
    public void disableTrace() {
        DefaultTraceContext traceContext = new DefaultTraceContext();
        Trace trace = traceContext.disableSampling();
        Assert.assertNotNull(trace);
        Assert.assertFalse(trace.canSampled());

        traceContext.detachTraceObject();

    }

    @Test
    public void threadLocalBindTest() {
        AgentInformation agentInformation = new AgentInformation("d", "d", System.currentTimeMillis(), 123, "machineName", "127.0.0.1", ServiceType.TEST_STAND_ALONE.getCode(), Version.VERSION);

        DefaultTraceContext traceContext1 = new DefaultTraceContext();
        traceContext1.setAgentInformation(agentInformation);
        Assert.assertNotNull(traceContext1.newTraceObject());

        DefaultTraceContext traceContext2 = new DefaultTraceContext();
        traceContext2.setAgentInformation(agentInformation);
        Trace notExist = traceContext2.currentRawTraceObject();
        Assert.assertNull(notExist);

        Assert.assertNotNull(traceContext1.currentRawTraceObject());
        traceContext1.detachTraceObject();
        Assert.assertNull(traceContext1.currentRawTraceObject());


    }
}
