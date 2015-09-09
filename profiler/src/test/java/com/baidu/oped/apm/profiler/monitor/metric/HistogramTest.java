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

package com.baidu.oped.apm.profiler.monitor.metric;

import com.baidu.oped.apm.common.HistogramSchema;
import com.baidu.oped.apm.common.ServiceType;
import com.baidu.oped.apm.profiler.monitor.metric.HistogramSnapshot;
import com.baidu.oped.apm.profiler.monitor.metric.LongAdderHistogram;

import org.junit.Assert;
import org.junit.Test;


public class HistogramTest {

    @Test
    public void testAddResponseTime() throws Exception {
        HistogramSchema schema = ServiceType.TOMCAT.getHistogramSchema();
        LongAdderHistogram histogram = new LongAdderHistogram(ServiceType.TOMCAT);
        histogram.addResponseTime(1000);

        histogram.addResponseTime(3000);
        histogram.addResponseTime(3000);

        histogram.addResponseTime(5000);
        histogram.addResponseTime(5000);
        histogram.addResponseTime(5000);

        histogram.addResponseTime(6000);
        histogram.addResponseTime(6000);
        histogram.addResponseTime(6000);
        histogram.addResponseTime(6000);

        histogram.addResponseTime(schema.getErrorSlot().getSlotTime());
        histogram.addResponseTime(schema.getErrorSlot().getSlotTime());
        histogram.addResponseTime(schema.getErrorSlot().getSlotTime());
        histogram.addResponseTime(schema.getErrorSlot().getSlotTime());
        histogram.addResponseTime(schema.getErrorSlot().getSlotTime());


        HistogramSnapshot snapshot = histogram.createSnapshot();
        Assert.assertEquals(snapshot.getFastCount(), 1);
        Assert.assertEquals(snapshot.getNormalCount(), 2);
        Assert.assertEquals(snapshot.getSlowCount(), 3);
        Assert.assertEquals(snapshot.getVerySlowCount(), 4);
        Assert.assertEquals(snapshot.getErrorCount(), 5);
    }

}