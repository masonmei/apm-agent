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

package com.baidu.oped.apm.profiler.modifier.db.jtds;

import com.baidu.oped.apm.bootstrap.Agent;
import com.baidu.oped.apm.bootstrap.config.ProfilerConfig;
import com.baidu.oped.apm.bootstrap.instrument.ByteCodeInstrumentor;
import com.baidu.oped.apm.bootstrap.instrument.InstrumentClass;
import com.baidu.oped.apm.bootstrap.instrument.InstrumentException;
import com.baidu.oped.apm.bootstrap.interceptor.Interceptor;
import com.baidu.oped.apm.bootstrap.interceptor.tracevalue.DatabaseInfoTraceValue;
import com.baidu.oped.apm.profiler.modifier.AbstractModifier;
import com.baidu.oped.apm.profiler.modifier.db.interceptor.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.ProtectionDomain;

public abstract class JtdsConnectionModifier extends AbstractModifier {

    private static final String SCOPE_NAME = JtdsScope.SCOPE_NAME;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public JtdsConnectionModifier(ByteCodeInstrumentor byteCodeInstrumentor, Agent agent) {
        super(byteCodeInstrumentor, agent);
    }


    public byte[] modify(ClassLoader classLoader, String javassistClassName, ProtectionDomain protectedDomain, byte[] classFileBuffer) {
        if (logger.isInfoEnabled()) {
            logger.info("Modifing. {}", javassistClassName);
        }
        try {
            InstrumentClass jtdsConnection = byteCodeInstrumentor.getClass(classLoader, javassistClassName, classFileBuffer);


            jtdsConnection.addTraceValue(DatabaseInfoTraceValue.class);


            Interceptor closeConnection = new ConnectionCloseInterceptor();
            jtdsConnection.addScopeInterceptor("close", null, closeConnection, SCOPE_NAME);


            Interceptor statementCreateInterceptor1 = new StatementCreateInterceptor();
            jtdsConnection.addScopeInterceptor("createStatement", null, statementCreateInterceptor1, SCOPE_NAME);

            Interceptor statementCreateInterceptor2 = new StatementCreateInterceptor();
            jtdsConnection.addScopeInterceptor("createStatement", new String[]{"int", "int"}, statementCreateInterceptor2, SCOPE_NAME);

            Interceptor statementCreateInterceptor3 = new StatementCreateInterceptor();
            jtdsConnection.addScopeInterceptor("createStatement", new String[]{"int", "int", "int"}, statementCreateInterceptor3, SCOPE_NAME);


            Interceptor preparedStatementCreateInterceptor1 = new PreparedStatementCreateInterceptor();
            jtdsConnection.addScopeInterceptor("prepareStatement", new String[]{"java.lang.String"}, preparedStatementCreateInterceptor1, SCOPE_NAME);

            Interceptor preparedStatementCreateInterceptor2 = new PreparedStatementCreateInterceptor();
            jtdsConnection.addScopeInterceptor("prepareStatement", new String[]{"java.lang.String", "int"}, preparedStatementCreateInterceptor2, SCOPE_NAME);

            Interceptor preparedStatementCreateInterceptor3 = new PreparedStatementCreateInterceptor();
            jtdsConnection.addScopeInterceptor("prepareStatement", new String[]{"java.lang.String", "int[]"}, preparedStatementCreateInterceptor3, SCOPE_NAME);

            Interceptor preparedStatementCreateInterceptor4 = new PreparedStatementCreateInterceptor();
            jtdsConnection.addScopeInterceptor("prepareStatement", new String[]{"java.lang.String", "java.lang.String[]"}, preparedStatementCreateInterceptor4, SCOPE_NAME);

            Interceptor preparedStatementCreateInterceptor5 = new PreparedStatementCreateInterceptor();
            jtdsConnection.addScopeInterceptor("prepareStatement", new String[]{"java.lang.String", "int", "int"}, preparedStatementCreateInterceptor5, SCOPE_NAME);

            Interceptor preparedStatementCreateInterceptor6 = new PreparedStatementCreateInterceptor();
            jtdsConnection.addScopeInterceptor("prepareStatement", new String[]{"java.lang.String", "int", "int", "int"}, preparedStatementCreateInterceptor6, SCOPE_NAME);

            final ProfilerConfig profilerConfig = agent.getProfilerConfig();
            if (profilerConfig.isJdbcProfileJtdsSetAutoCommit()) {
                Interceptor setAutocommit = new TransactionSetAutoCommitInterceptor();
                jtdsConnection.addScopeInterceptor("setAutoCommit", new String[]{"boolean"}, setAutocommit, SCOPE_NAME);
            }
            if (profilerConfig.isJdbcProfileJtdsCommit()) {
                Interceptor commit = new TransactionCommitInterceptor();
                jtdsConnection.addScopeInterceptor("commit", null, commit, SCOPE_NAME);
            }
            if (profilerConfig.isJdbcProfileJtdsRollback()) {
                Interceptor rollback = new TransactionRollbackInterceptor();
                jtdsConnection.addScopeInterceptor("rollback", null, rollback, SCOPE_NAME);
            }

            if (this.logger.isInfoEnabled()) {
                this.logger.info("{} class is converted.", javassistClassName);
            }

            return jtdsConnection.toBytecode();
        } catch (InstrumentException e) {
            if (logger.isWarnEnabled()) {
                logger.warn("{} modify fail. Cause:{}", this.getClass().getSimpleName(), e.getMessage(), e);
            }
            return null;
        }
    }



}
