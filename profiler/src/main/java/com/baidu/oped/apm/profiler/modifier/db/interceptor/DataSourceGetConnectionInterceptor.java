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

import com.baidu.oped.apm.bootstrap.context.RecordableTrace;
import com.baidu.oped.apm.bootstrap.interceptor.*;
import com.baidu.oped.apm.common.ServiceType;

/**
 * Maybe we should trace get of Datasource.
 * @author emeroad
 */
public class DataSourceGetConnectionInterceptor extends SpanEventSimpleAroundInterceptor {

//    private final DepthScope scope = JDBCScope.SCOPE;

    public DataSourceGetConnectionInterceptor() {
        super(DataSourceGetConnectionInterceptor.class);
    }

    @Override
    public void doInBeforeTrace(RecordableTrace trace, final Object target, Object[] args) {
        trace.markBeforeTime();
    }

    @Override
    public void doInAfterTrace(RecordableTrace trace, Object target, Object[] args, Object result, Throwable throwable) {
        trace.recordServiceType(ServiceType.DBCP);
        if (args == null) {
//          getConnection() without any arguments
            trace.recordApi(getMethodDescriptor());
        } else if(args.length == 2) {
//          skip args[1] because it's a password.
            trace.recordApi(getMethodDescriptor(), args[0], 0);
        }
        trace.recordException(throwable);

        trace.markAfterTime();
    }


}
