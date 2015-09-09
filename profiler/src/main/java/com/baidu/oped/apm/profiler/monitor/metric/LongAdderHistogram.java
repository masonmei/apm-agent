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
import com.baidu.oped.apm.common.HistogramSlot;
import com.baidu.oped.apm.common.ServiceType;
import com.baidu.oped.apm.common.SlotType;
import com.baidu.oped.apm.profiler.util.jdk.LongAdder;

/**
 * @author emeroad
 */
public class LongAdderHistogram implements Histogram {
    // We could use LongAdder only for fastCounter and AtomicLong for the others.
    private final LongAdder fastCounter = new LongAdder();
    private final LongAdder normalCounter = new LongAdder();
    private final LongAdder slowCounter = new LongAdder();
    private final LongAdder verySlowCounter = new LongAdder();

    private final LongAdder errorCounter = new LongAdder();

    private final short serviceType;
    private final HistogramSchema histogramSchema;

    public LongAdderHistogram(ServiceType serviceType) {
        this(serviceType.getCode(), serviceType.getHistogramSchema());
    }

    public LongAdderHistogram(short serviceType, HistogramSchema histogramSchema) {
        this.serviceType = serviceType;
        this.histogramSchema = histogramSchema;
    }

    public short getServiceType() {
        return serviceType;
    }

    public void addResponseTime(int millis) {
        final HistogramSlot histogramSlot = histogramSchema.findHistogramSlot(millis);
        final SlotType slotType = histogramSlot.getSlotType();
        switch (slotType) {
            case FAST:
                fastCounter.increment();
                return;
            case NORMAL:
                normalCounter.increment();
                return;
            case SLOW:
                slowCounter.increment();
                return;
            case VERY_SLOW:
                verySlowCounter.increment();
                return;
            case ERROR:
                errorCounter.increment();
                return;
            default:
                throw new IllegalArgumentException("slot Type notFound:" + slotType);
        }
    }


    public HistogramSnapshot createSnapshot() {
        long fast = fastCounter.sum();
        long normal = normalCounter.sum();
        long slow = slowCounter.sum();
        long verySlow = verySlowCounter.sum();
        long error = errorCounter.sum();

        return new HistogramSnapshot(this.serviceType, fast, normal, slow, verySlow, error);
    }

    @Override
    public String toString() {
        return "LongAdderHistogram{" +
                "fastCounter=" + fastCounter +
                ", normalCounter=" + normalCounter +
                ", slowCounter=" + slowCounter +
                ", verySlowCounter=" + verySlowCounter +
                ", errorCounter=" + errorCounter +
                ", serviceType=" + serviceType +
                '}';
    }
}
