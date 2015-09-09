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

package com.baidu.oped.apm.profiler.monitor.codahale;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.JvmAttributeGaugeSet;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;
import com.baidu.oped.apm.profiler.monitor.CounterMonitor;
import com.baidu.oped.apm.profiler.monitor.EventRateMonitor;
import com.baidu.oped.apm.profiler.monitor.HistogramMonitor;
import com.baidu.oped.apm.profiler.monitor.MonitorName;
import com.baidu.oped.apm.profiler.monitor.MonitorRegistry;
import com.baidu.oped.apm.profiler.monitor.codahale.cpu.CpuLoadMetricSetSelector;
import com.baidu.oped.apm.profiler.monitor.codahale.cpu.metric.CpuLoadMetricSet;

/**
 * Use <a href="http://metrics.codahale.com/">Codahale</a> {@link MetricRegistry} for monitoring.
 * We have refered MointorRegistry MonitorRegistry of Netty.
 *
 * @author emeroad
 * @author harebox
 */
public class MetricMonitorRegistry implements MonitorRegistry {

    private final MetricRegistry delegate;

    public MetricMonitorRegistry() {
        this(new MetricRegistry());
    }

    public MetricMonitorRegistry(MetricRegistry registry) {
        if (registry == null) {
            throw new NullPointerException("registry is null");
        }
        this.delegate = registry;
    }

    public HistogramMonitor newHistogramMonitor(MonitorName monitorName) {
        final Histogram histogram = this.delegate.histogram(monitorName.getName());
        return new MetricHistogramMonitor(histogram);
    }

    public EventRateMonitor newEventRateMonitor(MonitorName monitorName) {
        if (monitorName == null) {
            throw new NullPointerException("monitorName must not be null");
        }
        final Meter meter = this.delegate.meter(monitorName.getName());
        return new MetricEventRateMonitor(meter);
    }

    public CounterMonitor newCounterMonitor(MonitorName monitorName) {
        if (monitorName == null) {
            throw new NullPointerException("monitorName must not be null");
        }
        final Counter counter = this.delegate.counter(monitorName.getName());
        return new MetricCounterMonitor(counter);
    }

    public MemoryUsageGaugeSet registerJvmMemoryMonitor(MonitorName monitorName) {
        if (monitorName == null) {
            throw new NullPointerException("monitorName must not be null");
        }
        return this.delegate.register(monitorName.getName(), new MemoryUsageGaugeSet());
    }

    public JvmAttributeGaugeSet registerJvmAttributeMonitor(MonitorName monitorName) {
        if (monitorName == null) {
            throw new NullPointerException("monitorName must not be null");
        }
        return this.delegate.register(monitorName.getName(), new JvmAttributeGaugeSet());
    }

    public GarbageCollectorMetricSet registerJvmGcMonitor(MonitorName monitorName) {
        if (monitorName == null) {
            throw new NullPointerException("monitorName must not be null");
        }
        return this.delegate.register(monitorName.getName(), new GarbageCollectorMetricSet());
    }

    public CpuLoadMetricSet registerCpuLoadMonitor(MonitorName monitorName) {
        if (monitorName == null) {
            throw new NullPointerException("monitorName must not be null");
        }
        return this.delegate.register(monitorName.getName(), CpuLoadMetricSetSelector.getCpuLoadMetricSet());
    }

    public ThreadStatesGaugeSet registerJvmThreadStatesMonitor(MonitorName monitorName) {
        if (monitorName == null) {
            throw new NullPointerException("monitorName must not be null");
        }
        return this.delegate.register(monitorName.getName(), new ThreadStatesGaugeSet());
    }

    public MetricRegistry getRegistry() {
        return this.delegate;
    }

    public String toString() {
        return "MetricMonitorRegistry(delegate=" + this.delegate + ")";
    }

}
