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

package com.baidu.oped.apm.profiler.modifier.db.mysql;

import com.baidu.oped.apm.bootstrap.Agent;
import com.baidu.oped.apm.bootstrap.config.ProfilerConfig;
import com.baidu.oped.apm.bootstrap.instrument.ByteCodeInstrumentor;
import com.baidu.oped.apm.bootstrap.instrument.InstrumentClass;
import com.baidu.oped.apm.bootstrap.instrument.InstrumentException;
import com.baidu.oped.apm.bootstrap.interceptor.Interceptor;
import com.baidu.oped.apm.bootstrap.interceptor.tracevalue.DatabaseInfoTraceValue;
import com.baidu.oped.apm.profiler.modifier.AbstractModifier;
import com.baidu.oped.apm.profiler.modifier.db.interceptor.*;
import com.baidu.oped.apm.profiler.modifier.db.mysql.interceptor.MySQLConnectionCreateInterceptor;

import java.security.ProtectionDomain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author emeroad
 */
public class MySQLConnectionImplModifier extends AbstractModifier {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public MySQLConnectionImplModifier(ByteCodeInstrumentor byteCodeInstrumentor, Agent agent) {
        super(byteCodeInstrumentor, agent);
    }

    public String getTargetClass() {
        return "com/mysql/jdbc/ConnectionImpl";
    }

    public byte[] modify(ClassLoader classLoader, String javassistClassName, ProtectionDomain protectedDomain, byte[] classFileBuffer) {
        if (logger.isInfoEnabled()) {
            logger.info("Modifing. {}", javassistClassName);
        }
        try {
            InstrumentClass mysqlConnection = byteCodeInstrumentor.getClass(classLoader, javassistClassName, classFileBuffer);


            mysqlConnection.addTraceValue(DatabaseInfoTraceValue.class);

            
            // If you want to make this common intercepter class, it has to be loaded to system.
//            Interceptor createConnection  = new ConnectionCreateInterceptor();
//            String[] params = new String[] {
//                "java.lang.String", "int", "java.util.Properties", "java.lang.String", "java.lang.String"
//            };
//            mysqlConnection.addInterceptor("getInstance", params, createConnection);
            Interceptor connectionUrlBindInterceptor = new MySQLConnectionCreateInterceptor();
            mysqlConnection.addConstructorInterceptor(new String[]{"java.lang.String", "int",
                    "java.util.Properties", "java.lang.String", "java.lang.String" }, connectionUrlBindInterceptor);


            Interceptor closeConnection = new ConnectionCloseInterceptor();
            mysqlConnection.addScopeInterceptor("close", null, closeConnection, MYSQLScope.SCOPE_NAME);


            Interceptor statementCreateInterceptor1 = new StatementCreateInterceptor();
            mysqlConnection.addScopeInterceptor("createStatement", null, statementCreateInterceptor1, MYSQLScope.SCOPE_NAME);

            Interceptor statementCreateInterceptor2 = new StatementCreateInterceptor();
            mysqlConnection.addScopeInterceptor("createStatement", new String[]{"int", "int"}, statementCreateInterceptor2, MYSQLScope.SCOPE_NAME);

            Interceptor statementCreateInterceptor3 = new StatementCreateInterceptor();
            mysqlConnection.addScopeInterceptor("createStatement", new String[]{"int", "int", "int"}, statementCreateInterceptor3, MYSQLScope.SCOPE_NAME);


            Interceptor preparedStatementCreateInterceptor1 = new PreparedStatementCreateInterceptor();
            mysqlConnection.addScopeInterceptor("prepareStatement", new String[]{"java.lang.String"}, preparedStatementCreateInterceptor1, MYSQLScope.SCOPE_NAME);

            Interceptor preparedStatementCreateInterceptor2 = new PreparedStatementCreateInterceptor();
            mysqlConnection.addScopeInterceptor("prepareStatement", new String[]{"java.lang.String", "int"}, preparedStatementCreateInterceptor2, MYSQLScope.SCOPE_NAME);

            Interceptor preparedStatementCreateInterceptor3 = new PreparedStatementCreateInterceptor();
            mysqlConnection.addScopeInterceptor("prepareStatement", new String[]{"java.lang.String", "int[]"}, preparedStatementCreateInterceptor3, MYSQLScope.SCOPE_NAME);

            Interceptor preparedStatementCreateInterceptor4 = new PreparedStatementCreateInterceptor();
            mysqlConnection.addScopeInterceptor("prepareStatement", new String[]{"java.lang.String", "java.lang.String[]"}, preparedStatementCreateInterceptor4, MYSQLScope.SCOPE_NAME);

            Interceptor preparedStatementCreateInterceptor5 = new PreparedStatementCreateInterceptor();
            mysqlConnection.addScopeInterceptor("prepareStatement", new String[]{"java.lang.String", "int", "int"}, preparedStatementCreateInterceptor5, MYSQLScope.SCOPE_NAME);

            Interceptor preparedStatementCreateInterceptor6 = new PreparedStatementCreateInterceptor();
            mysqlConnection.addScopeInterceptor("prepareStatement", new String[]{"java.lang.String", "int", "int", "int"}, preparedStatementCreateInterceptor6, MYSQLScope.SCOPE_NAME);

            final ProfilerConfig profilerConfig = agent.getProfilerConfig();
            if (profilerConfig.isJdbcProfileMySqlSetAutoCommit()) {
                Interceptor setAutocommit = new TransactionSetAutoCommitInterceptor();
                mysqlConnection.addScopeInterceptor("setAutoCommit", new String[]{"boolean"}, setAutocommit, MYSQLScope.SCOPE_NAME);
            }
            if (profilerConfig.isJdbcProfileMySqlCommit()) {
                Interceptor commit = new TransactionCommitInterceptor();
                mysqlConnection.addScopeInterceptor("commit", null, commit, MYSQLScope.SCOPE_NAME);
            }
            if (profilerConfig.isJdbcProfileMySqlRollback()) {
                Interceptor rollback = new TransactionRollbackInterceptor();
                mysqlConnection.addScopeInterceptor("rollback", null, rollback, MYSQLScope.SCOPE_NAME);
            }
            if (this.logger.isInfoEnabled()) {
                this.logger.info("{} class is converted.", javassistClassName);
            }

            return mysqlConnection.toBytecode();
        } catch (InstrumentException e) {
            if (logger.isWarnEnabled()) {
                logger.warn("{} modify fail. Cause:{}", this.getClass().getSimpleName(), e.getMessage(), e);
            }
            return null;
        }
    }


}
