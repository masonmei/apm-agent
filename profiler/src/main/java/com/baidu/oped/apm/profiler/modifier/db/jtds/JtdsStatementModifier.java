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
import com.baidu.oped.apm.bootstrap.instrument.ByteCodeInstrumentor;
import com.baidu.oped.apm.bootstrap.instrument.InstrumentClass;
import com.baidu.oped.apm.bootstrap.instrument.InstrumentException;
import com.baidu.oped.apm.bootstrap.interceptor.Interceptor;
import com.baidu.oped.apm.bootstrap.interceptor.tracevalue.DatabaseInfoTraceValue;
import com.baidu.oped.apm.profiler.modifier.AbstractModifier;
import com.baidu.oped.apm.profiler.modifier.db.interceptor.StatementExecuteQueryInterceptor;
import com.baidu.oped.apm.profiler.modifier.db.interceptor.StatementExecuteUpdateInterceptor;

import java.security.ProtectionDomain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JtdsStatementModifier extends AbstractModifier {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public JtdsStatementModifier(ByteCodeInstrumentor byteCodeInstrumentor, Agent agent) {
        super(byteCodeInstrumentor, agent);
    }

    public String getTargetClass() {
        return "net/sourceforge/jtds/jdbc/JtdsStatement";
    }


    public byte[] modify(ClassLoader classLoader, String javassistClassName, ProtectionDomain protectedDomain, byte[] classFileBuffer) {
        if (logger.isInfoEnabled()) {
            logger.info("Modifing. {}", javassistClassName);
        }

        try {
            InstrumentClass statementClass = byteCodeInstrumentor.getClass(classLoader, javassistClassName, classFileBuffer);
            Interceptor executeQuery = new StatementExecuteQueryInterceptor();
            statementClass.addScopeInterceptor("executeQuery", new String[]{"java.lang.String"}, executeQuery, JtdsScope.SCOPE_NAME);

            Interceptor executeUpdateInterceptor1 = new StatementExecuteUpdateInterceptor();
            statementClass.addScopeInterceptor("executeUpdate", new String[]{"java.lang.String"}, executeUpdateInterceptor1, JtdsScope.SCOPE_NAME);


            Interceptor executeUpdateInterceptor2 = new StatementExecuteUpdateInterceptor();
            statementClass.addScopeInterceptor("executeUpdate", new String[]{"java.lang.String", "int"}, executeUpdateInterceptor2, JtdsScope.SCOPE_NAME);

            Interceptor executeInterceptor1 = new StatementExecuteUpdateInterceptor();
            statementClass.addScopeInterceptor("execute", new String[]{"java.lang.String"}, executeInterceptor1, JtdsScope.SCOPE_NAME);

            Interceptor executeInterceptor2 = new StatementExecuteUpdateInterceptor();
            statementClass.addScopeInterceptor("execute", new String[]{"java.lang.String", "int"}, executeInterceptor2, JtdsScope.SCOPE_NAME);

            statementClass.addTraceValue(DatabaseInfoTraceValue.class);
            return statementClass.toBytecode();
        } catch (InstrumentException e) {
            if (logger.isWarnEnabled()) {
                logger.warn("{} modify fail. Cause:{}", this.getClass().getSimpleName(), e.getMessage(), e);
            }
            return null;
        }
    }

}
