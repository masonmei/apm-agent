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

package com.baidu.oped.apm.profiler.modifier.orm.mybatis;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;

import java.util.List;

import javax.sql.DataSource;

import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.transaction.TransactionFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mybatis.spring.SqlSessionTemplate;

import com.baidu.oped.apm.common.bo.SpanEventBo;

/**
 * @author Hyun Jeong
 */
public class SqlSessionTemplateModifierTest extends MyBatisClientModifierTest {

    private static final ExecutorType executorType = ExecutorType.SIMPLE;

    private SqlSessionTemplate sqlSessionTemplate;

    @Mock
    private SqlSessionFactory sqlSessionFactory;

    @Mock
    private SqlSession sqlSession;

    @Override
    protected SqlSession getSqlSession() {
        return this.sqlSessionTemplate;
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        setUpSqlSessionFactory();
        setUpSqlSession();
        this.sqlSessionTemplate = new SqlSessionTemplate(this.sqlSessionFactory, executorType);
    }

    private void setUpSqlSessionFactory() throws Exception {
        Configuration configuration = mock(Configuration.class);
        TransactionFactory transactionFactory = mock(TransactionFactory.class);
        DataSource dataSource = mock(DataSource.class);
        Environment environment = new Environment("test", transactionFactory, dataSource);
        when(this.sqlSessionFactory.getConfiguration()).thenReturn(configuration);
        when(configuration.getEnvironment()).thenReturn(environment);
    }

    private void setUpSqlSession() throws Exception {
        when(this.sqlSessionFactory.openSession(executorType)).thenReturn(this.sqlSession);
    }

    @Override
    @After
    public void cleanUp() throws Exception {
        // Should not manually close SqlSessionTemplate
    }

    @Ignore // Changed to trace only query operations
    @Override
    @Test
    public void commitShouldBeTraced() throws Exception {
        try {
            super.commitShouldBeTraced();
            fail("SqlSessionTemplate cannot manually call commit.");
        } catch (UnsupportedOperationException e) {
            final List<SpanEventBo> spanEvents = getCurrentSpanEvents();
            assertThat(spanEvents.size(), is(1));
            final SpanEventBo commitSpanEventBo = spanEvents.get(0);
            assertThat(commitSpanEventBo.hasException(), is(true));
            assertThat(commitSpanEventBo.getExceptionId(), not(NOT_CACHED));
        }
    }

    @Ignore // Changed to trace only query operations
    @Override
    @Test
    public void rollbackShouldBeTraced() throws Exception {
        try {
            super.rollbackShouldBeTraced();
            fail("SqlSessionTemplate cannot manually call rollback.");
        } catch (UnsupportedOperationException e) {
            final List<SpanEventBo> spanEvents = getCurrentSpanEvents();
            assertThat(spanEvents.size(), is(1));
            final SpanEventBo rollbackSpanEventBo = spanEvents.get(0);
            assertThat(rollbackSpanEventBo.hasException(), is(true));
            assertThat(rollbackSpanEventBo.getExceptionId(), not(NOT_CACHED));
        }
    }

    @Ignore // Changed to trace only query operations
    @Override
    @Test
    public void closeShouldBeTraced() throws Exception {
        try {
            super.closeShouldBeTraced();
        } catch (UnsupportedOperationException e) {
            final List<SpanEventBo> spanEvents = getCurrentSpanEvents();
            assertThat(spanEvents.size(), is(1));
            final SpanEventBo closeSpanEventBo = spanEvents.get(0);
            assertThat(closeSpanEventBo.hasException(), is(true));
            assertThat(closeSpanEventBo.getExceptionId(), not(NOT_CACHED));
        }
    }


}
