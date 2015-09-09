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

package com.baidu.oped.apm.profiler.modifier.db.cubrid;

import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.List;

import com.baidu.oped.apm.bootstrap.Agent;
import com.baidu.oped.apm.bootstrap.instrument.ByteCodeInstrumentor;
import com.baidu.oped.apm.bootstrap.instrument.InstrumentClass;
import com.baidu.oped.apm.bootstrap.instrument.InstrumentException;
import com.baidu.oped.apm.bootstrap.instrument.NotFoundInstrumentException;
import com.baidu.oped.apm.bootstrap.instrument.Scope;
import com.baidu.oped.apm.bootstrap.interceptor.Interceptor;
import com.baidu.oped.apm.bootstrap.interceptor.tracevalue.BindValueTraceValue;
import com.baidu.oped.apm.bootstrap.interceptor.tracevalue.DatabaseInfoTraceValue;
import com.baidu.oped.apm.bootstrap.interceptor.tracevalue.ParsingResultTraceValue;
import com.baidu.oped.apm.profiler.interceptor.ScopeDelegateStaticInterceptor;
import com.baidu.oped.apm.profiler.modifier.AbstractModifier;
import com.baidu.oped.apm.profiler.modifier.db.interceptor.*;
import com.baidu.oped.apm.profiler.util.JavaAssistUtils;
import com.baidu.oped.apm.profiler.util.PreparedStatementUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author emeroad
 */
public class CubridPreparedStatementModifier extends AbstractModifier {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());


    public CubridPreparedStatementModifier(ByteCodeInstrumentor byteCodeInstrumentor, Agent agent) {
        super(byteCodeInstrumentor, agent);
    }

    public String getTargetClass() {
        return "cubrid/jdbc/driver/CUBRIDPreparedStatement";
    }

    public byte[] modify(ClassLoader classLoader, String javassistClassName, ProtectionDomain protectedDomain, byte[] classFileBuffer) {
        if (logger.isInfoEnabled()) {
            logger.info("Modifing. {}", javassistClassName);
        }
        try {
            InstrumentClass preparedStatementClass = byteCodeInstrumentor.getClass(classLoader, javassistClassName, classFileBuffer);

            Interceptor executeInterceptor = new PreparedStatementExecuteQueryInterceptor();
            preparedStatementClass.addScopeInterceptor("execute", null, executeInterceptor, CubridScope.SCOPE_NAME);

            Interceptor executeQueryInterceptor = new PreparedStatementExecuteQueryInterceptor();
            preparedStatementClass.addScopeInterceptor("executeQuery", null, executeQueryInterceptor, CubridScope.SCOPE_NAME);

            Interceptor executeUpdateInterceptor = new PreparedStatementExecuteQueryInterceptor();
            preparedStatementClass.addScopeInterceptor("executeUpdate", null, executeUpdateInterceptor, CubridScope.SCOPE_NAME);

            preparedStatementClass.addTraceValue(DatabaseInfoTraceValue.class);
            preparedStatementClass.addTraceValue(ParsingResultTraceValue.class);
            preparedStatementClass.addTraceValue(BindValueTraceValue.class, "new java.util.HashMap();");

            bindVariableIntercept(preparedStatementClass, classLoader, protectedDomain);

            return preparedStatementClass.toBytecode();
        } catch (InstrumentException e) {
            if (logger.isWarnEnabled()) {
                logger.warn("{} modify fail. Cause:{}", this.getClass().getSimpleName(), e.getMessage(), e);
            }
            return null;
        }
    }

    private void bindVariableIntercept(InstrumentClass preparedStatement, ClassLoader classLoader, ProtectionDomain protectedDomain) throws InstrumentException {
        List<Method> bindMethod = PreparedStatementUtils.findBindVariableSetMethod();
        final Scope scope = byteCodeInstrumentor.getScope(CubridScope.SCOPE_NAME);
        Interceptor interceptor = new ScopeDelegateStaticInterceptor(new PreparedStatementBindVariableInterceptor(), scope);
        int interceptorId = -1;
        for (Method method : bindMethod) {
            String methodName = method.getName();
            String[] parameterType = JavaAssistUtils.getParameterType(method.getParameterTypes());
            try {
                if (interceptorId == -1) {
                    interceptorId = preparedStatement.addInterceptor(methodName, parameterType, interceptor);
                } else {
                    preparedStatement.reuseInterceptor(methodName, parameterType, interceptorId);
                }
            } catch (NotFoundInstrumentException e) {
                // Cannot find bind variable setter method. This is not an error. Just some log will be enough.
                if (logger.isDebugEnabled()) {
                    logger.debug("bindVariable api not found. method:{} param:{} Cause:{}", methodName, Arrays.toString(parameterType), e.getMessage());
                }
            }
        }
    }
}
