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

package com.baidu.oped.apm.profiler.monitor.codahale.cpu;

import static com.baidu.oped.apm.profiler.monitor.codahale.MetricMonitorValues.*;

import java.util.Map;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.baidu.oped.apm.profiler.monitor.codahale.MetricMonitorValues;
import com.baidu.oped.apm.profiler.monitor.codahale.cpu.metric.CpuLoadMetricSet;
import com.baidu.oped.apm.thrift.dto.TCpuLoad;

/**
 * @author hyungil.jeong
 */
public class CpuLoadCollector {

    private final Gauge<Double> jvmCpuLoadGauge;
    private final Gauge<Double> systemCpuLoadGauge;

    @SuppressWarnings("unchecked")
    public CpuLoadCollector(CpuLoadMetricSet cpuLoadMetricSet) {
        if (cpuLoadMetricSet == null) {
            throw new NullPointerException("cpuLoadMetricSet must not be null");
        }
        Map<String, Metric> metrics = cpuLoadMetricSet.getMetrics();
        this.jvmCpuLoadGauge = (Gauge<Double>)MetricMonitorValues.getMetric(metrics, CPU_LOAD_JVM, DOUBLE_ZERO);
        this.systemCpuLoadGauge = (Gauge<Double>)MetricMonitorValues.getMetric(metrics, CPU_LOAD_SYSTEM, DOUBLE_ZERO);
    }

    public TCpuLoad collectCpuLoad() {
        Double jvmCpuLoad = this.jvmCpuLoadGauge.getValue();
        Double systemCpuLoad = this.systemCpuLoadGauge.getValue();
        if (notCollected(jvmCpuLoad) && notCollected(systemCpuLoad)) {
            return null;
        }
        TCpuLoad cpuLoad = new TCpuLoad();
        if (!notCollected(jvmCpuLoad)) {
            cpuLoad.setJvmCpuLoad(jvmCpuLoad);
        }
        if (!notCollected(systemCpuLoad)) {
            cpuLoad.setSystemCpuLoad(systemCpuLoad);
        }
        return cpuLoad;
    }

    private boolean notCollected(double cpuLoad) {
        return cpuLoad < 0;
    }
}
