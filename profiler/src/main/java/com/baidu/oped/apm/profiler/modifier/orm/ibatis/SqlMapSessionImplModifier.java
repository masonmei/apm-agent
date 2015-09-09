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

package com.baidu.oped.apm.profiler.modifier.orm.ibatis;

import org.slf4j.LoggerFactory;

import com.baidu.oped.apm.bootstrap.Agent;
import com.baidu.oped.apm.bootstrap.instrument.ByteCodeInstrumentor;
import com.baidu.oped.apm.bootstrap.instrument.MethodFilter;
import com.baidu.oped.apm.profiler.modifier.orm.ibatis.filter.SqlMapSessionMethodFilter;

/**
 * iBatis SqlMapSessionImpl Modifier
 * <p/>
 * Hooks onto <i>com.ibatis.sqlmap.engine.SqlMapSessionImpl</i>
 * <p/>
 * 
 * @author Hyun Jeong
 */
public final class SqlMapSessionImplModifier extends IbatisClientModifier {

    private static final MethodFilter sqlMapSessionMethodFilter = new SqlMapSessionMethodFilter();

    public static final String TARGET_CLASS_NAME = "com/ibatis/sqlmap/engine/impl/SqlMapSessionImpl";

    public SqlMapSessionImplModifier(ByteCodeInstrumentor byteCodeInstrumentor, Agent agent) {
        super(byteCodeInstrumentor, agent);
        this.logger = LoggerFactory.getLogger(this.getClass());
    }

    @Override
    public String getTargetClass() {
        return TARGET_CLASS_NAME;
    }


    @Override
    protected MethodFilter getIbatisApiMethodFilter() {
        return sqlMapSessionMethodFilter;
    }

}
