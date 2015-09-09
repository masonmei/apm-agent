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

package com.baidu.oped.apm.profiler.modifier;

import java.util.HashMap;
import java.util.Map;

import com.baidu.oped.apm.bootstrap.Agent;
import com.baidu.oped.apm.bootstrap.config.ProfilerConfig;
import com.baidu.oped.apm.bootstrap.instrument.ByteCodeInstrumentor;
import com.baidu.oped.apm.profiler.ClassFileRetransformer;
import com.baidu.oped.apm.profiler.modifier.arcus.ArcusClientModifier;
import com.baidu.oped.apm.profiler.modifier.arcus.BaseOperationModifier;
import com.baidu.oped.apm.profiler.modifier.arcus.CacheManagerModifier;
import com.baidu.oped.apm.profiler.modifier.arcus.CollectionFutureModifier;
import com.baidu.oped.apm.profiler.modifier.arcus.GetFutureModifier;
import com.baidu.oped.apm.profiler.modifier.arcus.ImmediateFutureModifier;
import com.baidu.oped.apm.profiler.modifier.arcus.MemcachedClientModifier;
import com.baidu.oped.apm.profiler.modifier.arcus.OperationFutureModifier;
import com.baidu.oped.apm.profiler.modifier.connector.asynchttpclient.AsyncHttpClientModifier;
import com.baidu.oped.apm.profiler.modifier.connector.httpclient3.DefaultHttpMethodRetryHandlerModifier;
import com.baidu.oped.apm.profiler.modifier.connector.httpclient3.HttpClientModifier;
import com.baidu.oped.apm.profiler.modifier.connector.httpclient4.BasicFutureModifier;
import com.baidu.oped.apm.profiler.modifier.connector.httpclient4.ClosableHttpAsyncClientModifier;
import com.baidu.oped.apm.profiler.modifier.connector.httpclient4.ClosableHttpClientModifier;
import com.baidu.oped.apm.profiler.modifier.connector.httpclient4.DefaultHttpRequestRetryHandlerModifier;
import com.baidu.oped.apm.profiler.modifier.connector.httpclient4.HttpClient4Modifier;
import com.baidu.oped.apm.profiler.modifier.connector.jdkhttpconnector.HttpURLConnectionModifier;
import com.baidu.oped.apm.profiler.modifier.db.cubrid.CubridConnectionModifier;
import com.baidu.oped.apm.profiler.modifier.db.cubrid.CubridDriverModifier;
import com.baidu.oped.apm.profiler.modifier.db.cubrid.CubridPreparedStatementModifier;
import com.baidu.oped.apm.profiler.modifier.db.cubrid.CubridResultSetModifier;
import com.baidu.oped.apm.profiler.modifier.db.cubrid.CubridStatementModifier;
import com.baidu.oped.apm.profiler.modifier.db.dbcp.DBCPBasicDataSourceModifier;
import com.baidu.oped.apm.profiler.modifier.db.dbcp.DBCPPoolGuardConnectionWrapperModifier;
import com.baidu.oped.apm.profiler.modifier.db.jtds.Jdbc2ConnectionModifier;
import com.baidu.oped.apm.profiler.modifier.db.jtds.Jdbc4_1ConnectionModifier;
import com.baidu.oped.apm.profiler.modifier.db.jtds.JtdsDriverModifier;
import com.baidu.oped.apm.profiler.modifier.db.jtds.JtdsPreparedStatementModifier;
import com.baidu.oped.apm.profiler.modifier.db.jtds.JtdsResultSetModifier;
import com.baidu.oped.apm.profiler.modifier.db.jtds.JtdsStatementModifier;
import com.baidu.oped.apm.profiler.modifier.db.mysql.MySQLConnectionImplModifier;
import com.baidu.oped.apm.profiler.modifier.db.mysql.MySQLConnectionModifier;
import com.baidu.oped.apm.profiler.modifier.db.mysql.MySQLNonRegisteringDriverModifier;
import com.baidu.oped.apm.profiler.modifier.db.mysql.MySQLPreparedStatementJDBC4Modifier;
import com.baidu.oped.apm.profiler.modifier.db.mysql.MySQLPreparedStatementModifier;
import com.baidu.oped.apm.profiler.modifier.db.mysql.MySQLStatementModifier;
import com.baidu.oped.apm.profiler.modifier.db.oracle.OracleDriverModifier;
import com.baidu.oped.apm.profiler.modifier.db.oracle.OraclePreparedStatementModifier;
import com.baidu.oped.apm.profiler.modifier.db.oracle.OraclePreparedStatementWrapperModifier;
import com.baidu.oped.apm.profiler.modifier.db.oracle.OracleStatementModifier;
import com.baidu.oped.apm.profiler.modifier.db.oracle.OracleStatementWrapperModifier;
import com.baidu.oped.apm.profiler.modifier.db.oracle.PhysicalConnectionModifier;
import com.baidu.oped.apm.profiler.modifier.log.log4j.LoggingEventOfLog4jModifier;
import com.baidu.oped.apm.profiler.modifier.log.logback.LoggingEventOfLogbackModifier;
import com.baidu.oped.apm.profiler.modifier.method.MethodModifier;
import com.baidu.oped.apm.profiler.modifier.orm.ibatis.SqlMapClientImplModifier;
import com.baidu.oped.apm.profiler.modifier.orm.ibatis.SqlMapSessionImplModifier;
import com.baidu.oped.apm.profiler.modifier.orm.mybatis.DefaultSqlSessionModifier;
import com.baidu.oped.apm.profiler.modifier.orm.mybatis.SqlSessionTemplateModifier;
import com.baidu.oped.apm.profiler.modifier.redis.BinaryJedisModifier;
import com.baidu.oped.apm.profiler.modifier.redis.JedisClientModifier;
import com.baidu.oped.apm.profiler.modifier.redis.JedisModifier;
import com.baidu.oped.apm.profiler.modifier.redis.JedisMultiKeyPipelineBaseModifier;
import com.baidu.oped.apm.profiler.modifier.redis.JedisPipelineBaseModifier;
import com.baidu.oped.apm.profiler.modifier.redis.JedisPipelineModifier;
import com.baidu.oped.apm.profiler.modifier.servlet.HttpServletModifier;
import com.baidu.oped.apm.profiler.modifier.servlet.SpringFrameworkServletModifier;
import com.baidu.oped.apm.profiler.modifier.spring.beans.AbstractAutowireCapableBeanFactoryModifier;
import com.baidu.oped.apm.profiler.modifier.spring.orm.ibatis.SqlMapClientTemplateModifier;
import com.baidu.oped.apm.profiler.modifier.tomcat.RequestFacadeModifier;
import com.baidu.oped.apm.profiler.modifier.tomcat.StandardHostValveInvokeModifier;
import com.baidu.oped.apm.profiler.modifier.tomcat.StandardServiceModifier;
import com.baidu.oped.apm.profiler.modifier.tomcat.TomcatConnectorModifier;
import com.baidu.oped.apm.profiler.modifier.tomcat.WebappLoaderModifier;

/**
 * @author emeroad
 * @author netspider
 * @author hyungil.jeong
 * @author Minwoo Jung
 * @authoer jaehong.kim
 *  - add redis
 */
public class DefaultModifierRegistry implements ModifierRegistry {

    // No concurrent issue because only one thread put entries to the map and get operations are started after the map is completely build.
    // Set the map size big intentionally to keep hash collision low.
    private final Map<String, AbstractModifier> registry = new HashMap<String, AbstractModifier>(512);

    private final ByteCodeInstrumentor byteCodeInstrumentor;
    private final ProfilerConfig profilerConfig;
    private final Agent agent;
    private final ClassFileRetransformer retransformer;

    public DefaultModifierRegistry(Agent agent, ByteCodeInstrumentor byteCodeInstrumentor, ClassFileRetransformer retransformer) {
        this.agent = agent;
        this.byteCodeInstrumentor = byteCodeInstrumentor;
        this.retransformer = retransformer;
        this.profilerConfig = agent.getProfilerConfig();
    }

    @Override
    public AbstractModifier findModifier(String className) {
        return registry.get(className);
    }

    public void addModifier(AbstractModifier modifier) {
        AbstractModifier old = registry.put(modifier.getTargetClass(), modifier);
        if (old != null) {
            throw new IllegalStateException("Modifier already exist new:" + modifier.getClass() + " old:" + old.getTargetClass());
        }
    }
    
    public void addMethodModifier() {
        MethodModifier methodModifier = new MethodModifier(byteCodeInstrumentor, agent);
        addModifier(methodModifier);
    }

    public void addConnectorModifier() {
        if (profilerConfig.isApacheHttpClient4Profile()) {
            //apache http client 4
            HttpClient4Modifier httpClient4Modifier = new HttpClient4Modifier(byteCodeInstrumentor, agent);
            addModifier(httpClient4Modifier);
            
            //apache http client 4 retry
            addModifier(new DefaultHttpRequestRetryHandlerModifier(byteCodeInstrumentor, agent));
        }
        if (profilerConfig.isApacheHttpClient3Profile()) {
            //apache http client 3
            HttpClientModifier httpClientModifier = new HttpClientModifier(byteCodeInstrumentor, agent);
            addModifier(httpClientModifier);
    
            //apache http client 3 retry
            addModifier(new DefaultHttpMethodRetryHandlerModifier(byteCodeInstrumentor, agent));
        }

        // JDK HTTPUrlConnector
        if (profilerConfig.isJdkHttpURLConnectionProfile()) {
            HttpURLConnectionModifier httpURLConnectionModifier = new HttpURLConnectionModifier(byteCodeInstrumentor, agent);
            addModifier(httpURLConnectionModifier);
        }

        // ning async http client
        addModifier(new AsyncHttpClientModifier(byteCodeInstrumentor, agent));

        // apache nio http client
        // addModifier(new InternalHttpAsyncClientModifier(byteCodeInstrumentor, agent));
        addModifier(new ClosableHttpAsyncClientModifier(byteCodeInstrumentor, agent));
        addModifier(new ClosableHttpClientModifier(byteCodeInstrumentor, agent));
        addModifier(new BasicFutureModifier(byteCodeInstrumentor, agent));
    }

    public void addArcusModifier() {
        final boolean arcus = profilerConfig.isArucs();
        boolean memcached;
        if (arcus) {
            // memcached is true if arcus is true.
            memcached = true;
        } else {
            memcached = profilerConfig.isMemcached();
        }

        if (memcached) {
            BaseOperationModifier baseOperationModifier = new BaseOperationModifier(byteCodeInstrumentor, agent);
            addModifier(baseOperationModifier);

            MemcachedClientModifier memcachedClientModifier = new MemcachedClientModifier(byteCodeInstrumentor, agent);
            addModifier(memcachedClientModifier);

//            Not working properly. commented out for now.
//            FrontCacheMemcachedClientModifier frontCacheMemcachedClientModifier = new FrontCacheMemcachedClientModifier(byteCodeInstrumentor, agent);
//            addModifier(frontCacheMemcachedClientModifier);

            if (arcus) {
                ArcusClientModifier arcusClientModifier = new ArcusClientModifier(byteCodeInstrumentor, agent);
                addModifier(arcusClientModifier);
                // Future of Arcus
                CollectionFutureModifier collectionFutureModifier = new CollectionFutureModifier(byteCodeInstrumentor, agent);
                addModifier(collectionFutureModifier);
            }

            // future modifier start ---------------------------------------------------

            GetFutureModifier getFutureModifier = new GetFutureModifier(byteCodeInstrumentor, agent);
            addModifier(getFutureModifier);

            ImmediateFutureModifier immediateFutureModifier = new ImmediateFutureModifier(byteCodeInstrumentor, agent);
            addModifier(immediateFutureModifier);

            OperationFutureModifier operationFutureModifier = new OperationFutureModifier(byteCodeInstrumentor, agent);
            addModifier(operationFutureModifier);

//            Not working properly. commented out for now.
//            FrontCacheGetFutureModifier frontCacheGetFutureModifier = new FrontCacheGetFutureModifier(byteCodeInstrumentor, agent);
//            addModifier(frontCacheGetFutureModifier);

            // future modifier end ---------------------------------------------------

            CacheManagerModifier cacheManagerModifier = new CacheManagerModifier(byteCodeInstrumentor, agent);
            addModifier(cacheManagerModifier);
        }
    }

    public void addTomcatModifier() {
        StandardHostValveInvokeModifier standardHostValveInvokeModifier = new StandardHostValveInvokeModifier(byteCodeInstrumentor, agent);
        addModifier(standardHostValveInvokeModifier);

        HttpServletModifier httpServletModifier = new HttpServletModifier(byteCodeInstrumentor, agent);
        addModifier(httpServletModifier);

        SpringFrameworkServletModifier springServletModifier = new SpringFrameworkServletModifier(byteCodeInstrumentor, agent);
        addModifier(springServletModifier);

        AbstractModifier tomcatStandardServiceModifier = new StandardServiceModifier(byteCodeInstrumentor, agent);
        addModifier(tomcatStandardServiceModifier);

        AbstractModifier tomcatConnectorModifier = new TomcatConnectorModifier(byteCodeInstrumentor, agent);
        addModifier(tomcatConnectorModifier);
        
        AbstractModifier tomcatWebappLoaderModifier = new WebappLoaderModifier(byteCodeInstrumentor, agent);
        addModifier(tomcatWebappLoaderModifier);

        if (profilerConfig.isTomcatHidePinpointHeader()) {
            AbstractModifier requestFacadeModifier = new RequestFacadeModifier(byteCodeInstrumentor, agent);
            addModifier(requestFacadeModifier);
        }
    }

    public void addJdbcModifier() {
        // TODO Can we check if JDBC driver exists here?

        if (!profilerConfig.isJdbcProfile()) {
            return;
        }

        if (profilerConfig.isJdbcProfileMySql()) {
            addMySqlDriver();
        }

        if (profilerConfig.isJdbcProfileJtds()) {
            addJtdsDriver();
        }

        if (profilerConfig.isJdbcProfileOracle()) {
            addOracleDriver();
        }
        if (profilerConfig.isJdbcProfileCubrid()) {
            addCubridDriver();
        }

        if (profilerConfig.isJdbcProfileDbcp()) {
            addDbcpDriver();
        }
    }

    private void addMySqlDriver() {
        // TODO In some MySQL drivers Connection is an interface and in the others it's a class. Is this OK?

        AbstractModifier mysqlNonRegisteringDriverModifier = new MySQLNonRegisteringDriverModifier(byteCodeInstrumentor, agent);
        addModifier(mysqlNonRegisteringDriverModifier);

        // From MySQL driver 5.1.x, backward compatibility is broken.
        // Driver returns not com.mysql.jdbc.Connection but com.mysql.jdbc.JDBC4Connection which extends com.mysql.jdbc.ConnectionImpl from 5.1.x
        AbstractModifier mysqlConnectionImplModifier = new MySQLConnectionImplModifier(byteCodeInstrumentor, agent);
        addModifier(mysqlConnectionImplModifier);

        AbstractModifier mysqlConnectionModifier = new MySQLConnectionModifier(byteCodeInstrumentor, agent);
        addModifier(mysqlConnectionModifier);

        AbstractModifier mysqlStatementModifier = new MySQLStatementModifier(byteCodeInstrumentor, agent);
        addModifier(mysqlStatementModifier);

        AbstractModifier mysqlPreparedStatementModifier = new MySQLPreparedStatementModifier(byteCodeInstrumentor, agent);
        addModifier(mysqlPreparedStatementModifier);

        MySQLPreparedStatementJDBC4Modifier myqlPreparedStatementJDBC4Modifier = new MySQLPreparedStatementJDBC4Modifier(byteCodeInstrumentor, agent);
        addModifier(myqlPreparedStatementJDBC4Modifier);

//      TODO Need to create result set fetch counter
//        Modifier mysqlResultSetModifier = new MySQLResultSetModifier(byteCodeInstrumentor, agent);
//        addModifier(mysqlResultSetModifier);
    }

    private void addJtdsDriver() {
        JtdsDriverModifier jtdsDriverModifier = new JtdsDriverModifier(byteCodeInstrumentor, agent);
        addModifier(jtdsDriverModifier);

        AbstractModifier jdbc2ConnectionModifier = new Jdbc2ConnectionModifier(byteCodeInstrumentor, agent);
        addModifier(jdbc2ConnectionModifier);

        AbstractModifier jdbc4_1ConnectionModifier = new Jdbc4_1ConnectionModifier(byteCodeInstrumentor, agent);
        addModifier(jdbc4_1ConnectionModifier);

        AbstractModifier mssqlStatementModifier = new JtdsStatementModifier(byteCodeInstrumentor, agent);
        addModifier(mssqlStatementModifier);

        AbstractModifier mssqlPreparedStatementModifier = new JtdsPreparedStatementModifier(byteCodeInstrumentor, agent);
        addModifier(mssqlPreparedStatementModifier);

        AbstractModifier mssqlResultSetModifier = new JtdsResultSetModifier(byteCodeInstrumentor, agent);
        addModifier(mssqlResultSetModifier);

    }

    private void addOracleDriver() {
        AbstractModifier oracleDriverModifier = new OracleDriverModifier(byteCodeInstrumentor, agent);
        addModifier(oracleDriverModifier);

        // TODO Intercepting PhysicalConnection makes view ugly.
        // We'd better intercept top-level classes T4C, T2C and OCI each to makes view more readable.
        AbstractModifier oracleConnectionModifier = new PhysicalConnectionModifier(byteCodeInstrumentor, agent);
        addModifier(oracleConnectionModifier);

        AbstractModifier oraclePreparedStatementWrapperModifier = new OraclePreparedStatementWrapperModifier(byteCodeInstrumentor, agent);
        addModifier(oraclePreparedStatementWrapperModifier);
        AbstractModifier oraclePreparedStatementModifier = new OraclePreparedStatementModifier(byteCodeInstrumentor, agent);
        addModifier(oraclePreparedStatementModifier);

        AbstractModifier oracleStatementWrapperModifier = new OracleStatementWrapperModifier(byteCodeInstrumentor, agent);
        addModifier(oracleStatementWrapperModifier);
        AbstractModifier oracleStatementModifier = new OracleStatementModifier(byteCodeInstrumentor, agent);
        addModifier(oracleStatementModifier);
        //
        // Modifier oracleResultSetModifier = new OracleResultSetModifier(byteCodeInstrumentor, agent);
        // addModifier(oracleResultSetModifier);
    }

    private void addCubridDriver() {
        // TODO Cubrid doesn't have connection impl too. Check it out.
        addModifier(new CubridConnectionModifier(byteCodeInstrumentor, agent));
        addModifier(new CubridDriverModifier(byteCodeInstrumentor, agent));
        addModifier(new CubridStatementModifier(byteCodeInstrumentor, agent));
        addModifier(new CubridPreparedStatementModifier(byteCodeInstrumentor, agent));
        addModifier(new CubridResultSetModifier(byteCodeInstrumentor, agent));
//        addModifier(new CubridUStatementModifier(byteCodeInstrumentor, agent));
    }

    private void addDbcpDriver() {

        // TODO Cubrid doesn't have connection impl too. Check it out.
        AbstractModifier dbcpBasicDataSourceModifier = new DBCPBasicDataSourceModifier(byteCodeInstrumentor, agent);
        addModifier(dbcpBasicDataSourceModifier);

        if (profilerConfig.isJdbcProfileDbcpConnectionClose()) {
            AbstractModifier dbcpPoolModifier = new DBCPPoolGuardConnectionWrapperModifier(byteCodeInstrumentor, agent);
            addModifier(dbcpPoolModifier);
        }
    }

    /**
     * Support ORM(iBatis, myBatis, etc.)
     */
    public void addOrmModifier() {
        addIBatisSupport();
        addMyBatisSupport();
    }

    private void addIBatisSupport() {
        if (profilerConfig.isIBatisEnabled()) {
            addModifier(new SqlMapSessionImplModifier(byteCodeInstrumentor, agent));
            addModifier(new SqlMapClientImplModifier(byteCodeInstrumentor, agent));
            addModifier(new SqlMapClientTemplateModifier(byteCodeInstrumentor, agent));
        }
    }

    private void addMyBatisSupport() {
        if (profilerConfig.isMyBatisEnabled()) {
            addModifier(new DefaultSqlSessionModifier(byteCodeInstrumentor, agent));
            addModifier(new SqlSessionTemplateModifier(byteCodeInstrumentor, agent));
        }
    }

    public void addSpringBeansModifier() {
        if (profilerConfig.isSpringBeansEnabled()) {
            addModifier(AbstractAutowireCapableBeanFactoryModifier.of(byteCodeInstrumentor, agent, retransformer));
        }
    }
    
    public void addRedisModifier() {
        if(profilerConfig.isRedisEnabled()) {
            addModifier(new BinaryJedisModifier(byteCodeInstrumentor, agent));
            addModifier(new JedisModifier(byteCodeInstrumentor, agent));
        }

        if(profilerConfig.isRedisPipelineEnabled()) {
            addModifier(new JedisClientModifier(byteCodeInstrumentor, agent));
            addModifier(new JedisPipelineBaseModifier(byteCodeInstrumentor, agent));
            addModifier(new JedisMultiKeyPipelineBaseModifier(byteCodeInstrumentor, agent));
            addModifier(new JedisPipelineModifier(byteCodeInstrumentor, agent));
        }
    }
    
    public void addLog4jModifier() {
        if (profilerConfig.isLog4jLoggingTransactionInfo()) {
            addModifier(new LoggingEventOfLog4jModifier(byteCodeInstrumentor, agent));
        }
    }

    public void addLogbackModifier() {
        if (profilerConfig.isLogbackLoggingTransactionInfo()) {
            addModifier(new LoggingEventOfLogbackModifier(byteCodeInstrumentor, agent));
        }
    }
}
