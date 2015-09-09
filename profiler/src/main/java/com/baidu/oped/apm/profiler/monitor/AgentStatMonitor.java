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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.baidu.oped.apm.common.util.PinpointThreadFactory;
import com.baidu.oped.apm.profiler.monitor.codahale.AgentStatCollectorFactory;
import com.baidu.oped.apm.profiler.monitor.codahale.cpu.CpuLoadCollector;
import com.baidu.oped.apm.profiler.monitor.codahale.gc.GarbageCollector;
import com.baidu.oped.apm.profiler.sender.DataSender;
import com.baidu.oped.apm.thrift.dto.TAgentStat;
import com.baidu.oped.apm.thrift.dto.TAgentStatBatch;
import com.baidu.oped.apm.thrift.dto.TCpuLoad;
import com.baidu.oped.apm.thrift.dto.TJvmGc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AgentStat monitor
 * 
 * @author harebox
 * @author hyungil.jeong
 */
public class AgentStatMonitor {

    private static final long DEFAULT_COLLECTION_INTERVAL_MS = 1000 * 5;
    private static final int DEFAULT_NUM_COLLECTIONS_PER_SEND = 6;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final boolean isTrace = logger.isTraceEnabled();
    private final long collectionIntervalMs;
    private final int numCollectionsPerBatch;

    private final ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1, new PinpointThreadFactory("Pinpoint-stat-monitor", true));

    private final DataSender dataSender;
    private final String agentId;
    private final AgentStatCollectorFactory agentStatCollectorFactory;
    private final long agentStartTime;

    public AgentStatMonitor(DataSender dataSender, String agentId, long startTime) {
        this(dataSender, agentId, startTime, DEFAULT_COLLECTION_INTERVAL_MS, DEFAULT_NUM_COLLECTIONS_PER_SEND);
    }

    public AgentStatMonitor(DataSender dataSender, String agentId, long startTime, long collectionInterval, int numCollectionsPerBatch) {
        if (dataSender == null) {
            throw new NullPointerException("dataSender must not be null");
        }
        if (agentId == null) {
            throw new NullPointerException("agentId must not be null");
        }
        this.dataSender = dataSender;
        this.agentId = agentId;
        this.agentStartTime = startTime;
        this.collectionIntervalMs = collectionInterval;
        this.numCollectionsPerBatch = numCollectionsPerBatch;
        this.agentStatCollectorFactory = new AgentStatCollectorFactory();
    }

    public void start() {
        long wait = 0;
        CollectJob job = new CollectJob(this.numCollectionsPerBatch);
        executor.scheduleAtFixedRate(job, wait, this.collectionIntervalMs, TimeUnit.MILLISECONDS);
        logger.info("AgentStat monitor started");
    }

    public void stop() {
        executor.shutdown();
        try {
            executor.awaitTermination(3000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        logger.info("AgentStat monitor stopped");
    }

    private class CollectJob implements Runnable {

        private final GarbageCollector garbageCollector;
        private final CpuLoadCollector cpuLoadCollector;
        // Will be used by single thread.
        // I don't think this object would run with multi threads.
        private final int numStatsPerBatch;
        private int collectCount = 0;
        private List<TAgentStat> agentStats;

        private CollectJob(int numStatsPerBatch) {
            this.garbageCollector = agentStatCollectorFactory.getGarbageCollector();
            this.cpuLoadCollector = agentStatCollectorFactory.getCpuLoadCollector();
            this.numStatsPerBatch = numStatsPerBatch;
            this.agentStats = new ArrayList<TAgentStat>(this.numStatsPerBatch);
        }

        public void run() {
            try {
                final TAgentStat agentStat = collectAgentStat();
                this.agentStats.add(agentStat);
                if (++this.collectCount >= this.numStatsPerBatch) {
                    sendAgentStats();
                    this.collectCount = 0;
                }
            } catch (Exception ex) {
                logger.warn("AgentStat collect failed. Caused:{}", ex.getMessage(), ex);
            }
        }

        private TAgentStat collectAgentStat() {
            final TAgentStat agentStat = new TAgentStat();
            agentStat.setTimestamp(System.currentTimeMillis());
            final TJvmGc gc = garbageCollector.collect();
            agentStat.setGc(gc);
            final TCpuLoad cpuLoad = cpuLoadCollector.collectCpuLoad();
            agentStat.setCpuLoad(cpuLoad);
            if (isTrace) {
                logger.trace("collect agentStat:{}", agentStat);
            }
            return agentStat;
        }

        private void sendAgentStats() {
            // prepare TAgentStat object.
            // TODO multi thread issue.
            // If we reuse TAgentStat, there could be concurrency issue because data sender runs in a different thread.
            final TAgentStatBatch agentStatBatch = new TAgentStatBatch();
            agentStatBatch.setAgentId(agentId);
            agentStatBatch.setStartTimestamp(agentStartTime);
            agentStatBatch.setAgentStats(this.agentStats);
            // If we reuse agentStats list, there could be concurrency issue because data sender runs in a different thread.
            // So create new list.
            this.agentStats = new ArrayList<TAgentStat>(this.numStatsPerBatch);
            if (isTrace) {
                logger.trace("collect agentStat:{}", agentStatBatch);
            }
            dataSender.send(agentStatBatch);
        }
    }

}
