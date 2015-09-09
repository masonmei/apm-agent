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

package com.baidu.oped.apm.profiler.modifier.db.interceptor;

import com.baidu.oped.apm.bootstrap.context.DatabaseInfo;
import com.baidu.oped.apm.bootstrap.context.RecordableTrace;
import com.baidu.oped.apm.bootstrap.interceptor.*;
import com.baidu.oped.apm.bootstrap.interceptor.tracevalue.DatabaseInfoTraceValueUtils;

/**
 * @author emeroad
 */
public class TransactionSetAutoCommitInterceptor extends SpanEventSimpleAroundInterceptor {


    public TransactionSetAutoCommitInterceptor() {
        super(TransactionSetAutoCommitInterceptor.class);
    }


    @Override
    public void doInBeforeTrace(RecordableTrace trace, Object target, Object[] args) {
        trace.markBeforeTime();
    }

    @Override
    protected void doInAfterTrace(RecordableTrace trace, Object target, Object[] args, Object result, Throwable throwable) {
        DatabaseInfo databaseInfo = DatabaseInfoTraceValueUtils.__getTraceDatabaseInfo(target, UnKnownDatabaseInfo.INSTANCE);

        trace.recordServiceType(databaseInfo.getType());
        trace.recordEndPoint(databaseInfo.getMultipleHost());
        trace.recordDestinationId(databaseInfo.getDatabaseId());


        trace.recordApi(getMethodDescriptor(), args);
        trace.recordException(throwable);

        trace.markAfterTime();
    }

}
