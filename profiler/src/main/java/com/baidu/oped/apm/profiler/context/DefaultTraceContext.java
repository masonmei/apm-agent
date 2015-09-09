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


import com.baidu.oped.apm.bootstrap.config.ProfilerConfig;
import com.baidu.oped.apm.bootstrap.context.*;
import com.baidu.oped.apm.bootstrap.interceptor.MethodDescriptor;
import com.baidu.oped.apm.bootstrap.sampler.Sampler;
import com.baidu.oped.apm.common.HistogramSchema;
import com.baidu.oped.apm.common.ServiceType;
import com.baidu.oped.apm.common.util.ParsingResult;
import com.baidu.oped.apm.profiler.AgentInformation;
import com.baidu.oped.apm.profiler.context.storage.LogStorageFactory;
import com.baidu.oped.apm.profiler.context.storage.StorageFactory;
import com.baidu.oped.apm.profiler.metadata.LRUCache;
import com.baidu.oped.apm.profiler.metadata.Result;
import com.baidu.oped.apm.profiler.metadata.SimpleCache;
import com.baidu.oped.apm.profiler.modifier.db.DefaultDatabaseInfo;
import com.baidu.oped.apm.profiler.modifier.db.JDBCUrlParser;
import com.baidu.oped.apm.profiler.monitor.metric.ContextMetric;
import com.baidu.oped.apm.profiler.monitor.metric.MetricRegistry;
import com.baidu.oped.apm.profiler.sampler.TrueSampler;
import com.baidu.oped.apm.profiler.sender.EnhancedDataSender;
import com.baidu.oped.apm.profiler.util.RuntimeMXBeanUtils;
import com.baidu.oped.apm.thrift.dto.TApiMetaData;
import com.baidu.oped.apm.thrift.dto.TSqlMetaData;
import com.baidu.oped.apm.thrift.dto.TStringMetaData;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author emeroad
 * @author hyungil.jeong
 */
public class DefaultTraceContext implements TraceContext {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final boolean isDebug = logger.isDebugEnabled();

    private final TraceFactory traceFactory;

    private final ActiveThreadCounter activeThreadCounter = new ActiveThreadCounter();


//    private GlobalCallTrace globalCallTrace = new GlobalCallTrace();

    private AgentInformation agentInformation;

    private EnhancedDataSender priorityDataSender;

    private final ServiceType contextServiceType;

    private final MetricRegistry metricRegistry;

    private final CachingSqlNormalizer cachingSqlNormalizer;

    private final SimpleCache<String> apiCache = new SimpleCache<String>();
    private final SimpleCache<String> stringCache = new SimpleCache<String>();

    private final JDBCUrlParser jdbcUrlParser = new JDBCUrlParser();

    private ProfilerConfig profilerConfig;
    
    private final ServerMetaDataHolder serverMetaDataHolder;

    // for test
    public DefaultTraceContext() {
        this(LRUCache.DEFAULT_CACHE_SIZE, ServiceType.STAND_ALONE.getCode(), new LogStorageFactory(), new TrueSampler(), new DefaultServerMetaDataHolder(RuntimeMXBeanUtils.getVmArgs()));
    }

    public DefaultTraceContext(final int sqlCacheSize, final short contextServiceType, StorageFactory storageFactory, Sampler sampler, ServerMetaDataHolder serverMetaDataHolder) {
        if (storageFactory == null) {
            throw new NullPointerException("storageFactory must not be null");
        }
        if (sampler == null) {
            throw new NullPointerException("sampler must not be null");
        }
        this.cachingSqlNormalizer = new DefaultCachingSqlNormalizer(sqlCacheSize);
        this.contextServiceType = ServiceType.findServiceType(contextServiceType);
        this.metricRegistry = new MetricRegistry(this.contextServiceType);

        this.traceFactory = new ThreadLocalTraceFactory(this, metricRegistry, storageFactory, sampler);
        
        this.serverMetaDataHolder = serverMetaDataHolder;
    }

    /**
     * Return trace only if current transaction can be sampled.
     * @return
     */
    public Trace currentTraceObject() {
        return traceFactory.currentTraceObject();
    }

    public Trace currentRpcTraceObject() {
        return traceFactory.currentTraceObject();
    }

    /**
     * Return trace without sampling check.
     * @return
     */
    @Override
    public Trace currentRawTraceObject() {
        return traceFactory.currentRawTraceObject();
    }

    @Override
    public Trace disableSampling() {
        // return null; is bug.  #93
        return traceFactory.disableSampling();
    }

    public void setProfilerConfig(final ProfilerConfig profilerConfig) {
        if (profilerConfig == null) {
            throw new NullPointerException("profilerConfig must not be null");
        }
        this.profilerConfig = profilerConfig;
    }

    @Override
    public ProfilerConfig getProfilerConfig() {
        return profilerConfig;
    }

    // Will be invoked when current transaction is picked as sampling target at remote.
    public Trace continueTraceObject(final TraceId traceID) {
        return traceFactory.continueTraceObject(traceID);
    }

    public Trace newTraceObject() {
        return traceFactory.newTraceObject();
    }


    @Override
    public void detachTraceObject() {
        this.traceFactory.detachTraceObject();
    }


    //@Override
    public ActiveThreadCounter getActiveThreadCounter() {
        return activeThreadCounter;
    }

    public AgentInformation getAgentInformation() {
        return agentInformation;
    }

    @Override
    public String getAgentId() {
        return this.agentInformation.getAgentId();
    }

    @Override
    public String getApplicationName() {
        return this.agentInformation.getApplicationName();
    }

    @Override
    public long getAgentStartTime() {
        return this.agentInformation.getStartTime();
    }

    @Override
    public short getServerTypeCode() {
        return this.agentInformation.getServerType();
    }

    @Override
    public String getServerType() {
        return this.agentInformation.getServerServiceType().getDesc();
    }


    @Override
    public int cacheApi(final MethodDescriptor methodDescriptor) {
        final String fullName = methodDescriptor.getFullName();
        final Result result = this.apiCache.put(fullName);
        if (result.isNewValue()) {
            methodDescriptor.setApiId(result.getId());

            final TApiMetaData apiMetadata = new TApiMetaData();
            apiMetadata.setAgentId(getAgentId());
            apiMetadata.setAgentStartTime(getAgentStartTime());

            apiMetadata.setApiId(result.getId());
            apiMetadata.setApiInfo(methodDescriptor.getApiDescriptor());
            apiMetadata.setLine(methodDescriptor.getLineNumber());

            this.priorityDataSender.request(apiMetadata);
        }
        return result.getId();
    }

    @Override
    public int cacheString(final String value) {
        if (value == null) {
            return 0;
        }
        final Result result = this.stringCache.put(value);
        if (result.isNewValue()) {
            final TStringMetaData stringMetaData = new TStringMetaData();
            stringMetaData.setAgentId(getAgentId());
            stringMetaData.setAgentStartTime(getAgentStartTime());

            stringMetaData.setStringId(result.getId());
            stringMetaData.setStringValue(value);
            this.priorityDataSender.request(stringMetaData);
        }
        return result.getId();
    }

    @Override
    public TraceId createTraceId(final String transactionId, final long parentSpanID, final long spanID, final short flags) {
        if (transactionId == null) {
            throw new NullPointerException("transactionId must not be null");
        }
        // TODO Should handle exception when parsing failed.
        return DefaultTraceId.parse(transactionId, parentSpanID, spanID, flags);
    }


    @Override
    public ParsingResult parseSql(final String sql) {
        // lazy sql normalization
        return this.cachingSqlNormalizer.wrapSql(sql);
    }

    @Override
    public boolean cacheSql(ParsingResult parsingResult) {
        if (parsingResult == null) {
            return false;
        }
        // lazy sql parsing
        boolean isNewValue = this.cachingSqlNormalizer.normalizedSql(parsingResult);
        if (isNewValue) {
            if (isDebug) {
                // TODO logging hit ratio could help debugging
                logger.debug("NewSQLParsingResult:{}", parsingResult);
            }

            // isNewValue means that the value is newly cached.
            // So the sql could be new one. We have to send sql metadata to collector.
            final TSqlMetaData sqlMetaData = new TSqlMetaData();
            sqlMetaData.setAgentId(getAgentId());
            sqlMetaData.setAgentStartTime(getAgentStartTime());

            sqlMetaData.setSqlId(parsingResult.getId());
            sqlMetaData.setSql(parsingResult.getSql());

            this.priorityDataSender.request(sqlMetaData);
        }
        return isNewValue;
    }

    @Override
     public DatabaseInfo parseJdbcUrl(final String url) {
        return this.jdbcUrlParser.parse(url);
    }

    @Override
    public DatabaseInfo createDatabaseInfo(ServiceType type, ServiceType executeQueryType, String url, int port, String databaseId) {
        List<String> host = new ArrayList<String>();
        host.add(url + ":" + port);
        DatabaseInfo databaseInfo = new DefaultDatabaseInfo(type, executeQueryType, url, url, host, databaseId);
        return databaseInfo;
    }



    public void setPriorityDataSender(final EnhancedDataSender priorityDataSender) {
        this.priorityDataSender = priorityDataSender;
    }


    public void setAgentInformation(final AgentInformation agentInformation) {
        if (agentInformation == null) {
            throw new NullPointerException("agentInformation must not be null");
        }
        this.agentInformation = agentInformation;
    }

    @Override
    public Metric getRpcMetric(ServiceType serviceType) {
        if (serviceType == null) {
            throw new NullPointerException("serviceType must not be null");
        }

        return this.metricRegistry.getRpcMetric(serviceType);
    }


    public void recordContextMetricIsError() {
        recordContextMetric(HistogramSchema.ERROR_SLOT_TIME);
    }

    public void recordContextMetric(int elapsedTime) {
        final ContextMetric contextMetric = this.metricRegistry.getResponseMetric();
        contextMetric.addResponseTime(elapsedTime);
    }

    public void recordAcceptResponseTime(String parentApplicationName, short parentApplicationType, int elapsedTime) {
        final ContextMetric contextMetric = this.metricRegistry.getResponseMetric();
        contextMetric.addAcceptHistogram(parentApplicationName, parentApplicationType, elapsedTime);
    }

    public void recordUserAcceptResponseTime(int elapsedTime) {
        final ContextMetric contextMetric = this.metricRegistry.getResponseMetric();
        contextMetric.addUserAcceptHistogram(elapsedTime);
    }

    @Override
    public ServerMetaDataHolder getServerMetaDataHolder() {
        return this.serverMetaDataHolder;
    }
}
