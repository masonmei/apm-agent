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

import org.junit.Assert;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baidu.oped.apm.profiler.context.DefaultTraceId;
import com.baidu.oped.apm.profiler.context.Span;
import com.baidu.oped.apm.profiler.context.SpanEvent;

/**
 * @author emeroad
 */
public class SpanEventTest {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Test
    public void testMarkStartTime() throws Exception {
        final DefaultTraceId traceId = new DefaultTraceId("agentTime", 0, 0);
        Span span = new Span();
        span.setAgentId("agentId");
        span.recordTraceId(traceId);
        span.markBeforeTime();
        Thread.sleep(10);
        span.markAfterTime();
        logger.debug("span:{}", span);

        final SpanEvent spanEvent = new SpanEvent(span);
        spanEvent.markStartTime();
        Thread.sleep(10);
        spanEvent.markAfterTime();
        logger.debug("spanEvent:{}", spanEvent);

        Assert.assertEquals("startTime", span.getStartTime() + spanEvent.getStartElapsed(), spanEvent.getStartTime());
        Assert.assertEquals("endTime", span.getStartTime() + spanEvent.getStartElapsed() + spanEvent.getEndElapsed(), spanEvent.getAfterTime());
    }

    @Test
    public void testGetStartTime() throws Exception {

    }

    @Test
    public void testMarkEndTime() throws Exception {

    }

    @Test
    public void testGetEndTime() throws Exception {

    }
}
