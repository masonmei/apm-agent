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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.ibatis.session.SqlSession;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import com.baidu.oped.apm.common.AnnotationKey;
import com.baidu.oped.apm.common.bo.AnnotationBo;
import com.baidu.oped.apm.common.bo.SpanEventBo;
import com.baidu.oped.apm.test.junit4.BasePinpointTest;

/**
 * @author Hyun Jeong
 */
public abstract class MyBatisClientModifierTest extends BasePinpointTest {

    public static final int NOT_CACHED = 0;

    protected abstract SqlSession getSqlSession();

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @After
    public void cleanUp() throws Exception {
        getSqlSession().close();
    }

    @Test
    public void nullParameterShouldNotBeTraced() throws Exception {
        // When
        getSqlSession().insert(null);
        // Then
        final List<SpanEventBo> spanEvents = getCurrentSpanEvents();
        assertThat(spanEvents.size(), is(1));

        // Check Method
        final SpanEventBo insertSpanEventBo = spanEvents.get(0);
        assertThat(insertSpanEventBo.getApiId(), not(NOT_CACHED));

        // Check Parameter
        assertNull(insertSpanEventBo.getAnnotationBoList());
    }

    @Test
    public void selectOneShouldBeTraced() throws Exception {
        // When
        getSqlSession().selectOne("selectOne");
        getSqlSession().selectOne("selectOne", null);
        // Then
        assertNOperations(2);
    }

    @Test
    public void selectListShouldBeTraced() throws Exception {
        // When
        getSqlSession().selectList("selectList");
        getSqlSession().selectList("selectList", null);
        getSqlSession().selectList("selectList", null, null);
        // Then
        assertNOperations(3);
    }

    @Test
    public void selectMapShouldBeTraced() throws Exception {
        // Given
        // When
        getSqlSession().selectMap("selectMap", null);
        getSqlSession().selectMap("selectMap", null, null);
        getSqlSession().selectMap("selectMap", null, null, null);
        // Then
        assertNOperations(3);
    }

    @Test
    public void selectShouldBeTraced() throws Exception {
        // When
        getSqlSession().select("select", null);
        getSqlSession().select("select", null, null);
        getSqlSession().select("select", null, null, null);
        // Then
        assertNOperations(3);
    }

    @Test
    public void insertShouldBeTraced() throws Exception {
        // When
        getSqlSession().insert("insert");
        getSqlSession().insert("insert", new Object());
        // Then
        assertNOperations(2);
    }

    @Test
    public void updateShouldBeTraced() throws Exception {
        // When
        getSqlSession().update("update");
        getSqlSession().update("update", new Object());
        // Then
        assertNOperations(2);
    }

    @Test
    public void deleteShouldBeTraced() throws Exception {
        // When
        getSqlSession().delete("delete");
        getSqlSession().delete("delete", new Object());
        // Then
        assertNOperations(2);
    }

    @Ignore // Changed to trace only query operations
    @Test
    public void commitShouldBeTraced() throws Exception {
        // When
        getSqlSession().commit();
        getSqlSession().commit(true);
        // Then
        final List<SpanEventBo> spanEvents = getCurrentSpanEvents();
        assertThat(spanEvents.size(), is(2));

        // Check MethodInfo
        final SpanEventBo commitWith0ArgSpanEvent = spanEvents.get(0);
        final SpanEventBo commitWith1ArgSpanEvent = spanEvents.get(1);
        assertThat(commitWith0ArgSpanEvent.getApiId(), not(NOT_CACHED));
        assertThat(commitWith1ArgSpanEvent.getApiId(), not(NOT_CACHED));
        assertThat(commitWith0ArgSpanEvent.getApiId(), not(commitWith1ArgSpanEvent.getApiId()));

        // Check Parameter
        assertNull(commitWith0ArgSpanEvent.getAnnotationBoList());
        assertThat(commitWith1ArgSpanEvent.getAnnotationBoList().get(0).getKey(), is(AnnotationKey.CACHE_ARGS0.getCode()));
    }

    @Ignore // Changed to trace only query operations
    @Test
    public void rollbackShouldBeTraced() throws Exception {
        // When
        getSqlSession().rollback();
        getSqlSession().rollback(true);
        // Then
        final List<SpanEventBo> spanEvents = getCurrentSpanEvents();
        assertThat(spanEvents.size(), is(2));

        // Check MethodInfo
        final SpanEventBo rollbackWith0ArgSpanEvent = spanEvents.get(0);
        final SpanEventBo rollbackWith1ArgSpanEvent = spanEvents.get(1);
        assertThat(rollbackWith0ArgSpanEvent.getApiId(), not(NOT_CACHED));
        assertThat(rollbackWith1ArgSpanEvent.getApiId(), not(NOT_CACHED));
        assertThat(rollbackWith0ArgSpanEvent.getApiId(), not(rollbackWith1ArgSpanEvent.getApiId()));

        // Check Parameter
        assertNull(rollbackWith0ArgSpanEvent.getAnnotationBoList());
        assertThat(rollbackWith1ArgSpanEvent.getAnnotationBoList().get(0).getKey(), is(AnnotationKey.CACHE_ARGS0.getCode()));
    }

    @Ignore // Changed to trace only query operations
    @Test
    public void flushStatementsShouldBeTraced() throws Exception {
        // When
        getSqlSession().flushStatements();
        // Then
        final List<SpanEventBo> spanEvents = getCurrentSpanEvents();
        assertThat(spanEvents.size(), is(1));

        // Check MethodInfo
        final SpanEventBo flushStatementsSpanEvent = spanEvents.get(0);
        assertThat(flushStatementsSpanEvent.getApiId(), not(NOT_CACHED));

        // Check Parameter
        assertNull(flushStatementsSpanEvent.getAnnotationBoList());
    }

    @Ignore // Changed to trace only query operations
    @Test
    public void closeShouldBeTraced() throws Exception {
        // When
        getSqlSession().close();
        // Then
        final List<SpanEventBo> spanEvents = getCurrentSpanEvents();
        assertThat(spanEvents.size(), is(1));

        // Check MethodInfo
        final SpanEventBo closeSpanEvent = spanEvents.get(0);
        assertThat(closeSpanEvent.getApiId(), not(NOT_CACHED));

        // Check Parameter
        assertNull(closeSpanEvent.getAnnotationBoList());
    }

    @Ignore // Changed to trace only query operations
    @Test
    public void getConfigurationShouldBeTraced() throws Exception {
        // When
        getSqlSession().getConfiguration();
        // Then
        final List<SpanEventBo> spanEvents = getCurrentSpanEvents();
        assertThat(spanEvents.size(), is(1));

        // Check MethodInfo
        final SpanEventBo getConfigurationSpanEvent = spanEvents.get(0);
        assertThat(getConfigurationSpanEvent.getApiId(), not(NOT_CACHED));

        // Check Parameter
        assertNull(getConfigurationSpanEvent.getAnnotationBoList());
    }

    @Ignore // Changed to trace only query operations
    @Test
    public void getMapperShouldBeTraced() throws Exception {
        // Given
        class SomeBean {}
        // When
        getSqlSession().getMapper(SomeBean.class);
        // Then
        final List<SpanEventBo> spanEvents = getCurrentSpanEvents();
        assertThat(spanEvents.size(), is(1));

        // Check MethodInfo
        final SpanEventBo getConnectionSpanEvent = spanEvents.get(0);
        assertThat(getConnectionSpanEvent.getApiId(), not(NOT_CACHED));

        // Check Parameter
        assertThat(getConnectionSpanEvent.getAnnotationBoList().get(0).getKey(), is(AnnotationKey.CACHE_ARGS0.getCode()));
    }

    @Ignore // Changed to trace only query operations
    @Test
    public void getConnectionShouldBeTraced() throws Exception {
        // When
        getSqlSession().getConnection();
        // Then
        final List<SpanEventBo> spanEvents = getCurrentSpanEvents();
        assertThat(spanEvents.size(), is(1));

        // Check MethodInfo
        final SpanEventBo getConnectionSpanEvent = spanEvents.get(0);
        assertThat(getConnectionSpanEvent.getApiId(), not(NOT_CACHED));

        // Check Parameter
        assertNull(getConnectionSpanEvent.getAnnotationBoList());
    }

    private void assertNOperations(int numOperations) {
        final List<SpanEventBo> spanEvents = getCurrentSpanEvents();
        assertThat(spanEvents.size(), is(numOperations));

        final Set<Integer> uniqueApiIds = new HashSet<Integer>();
        for (int n = 0; n < numOperations; ++n) {
            final SpanEventBo apiSpanEvent = spanEvents.get(n);
            uniqueApiIds.add(apiSpanEvent.getApiId());
            // Check MethodInfo
            assertThat(apiSpanEvent.getApiId(), not(NOT_CACHED));
            // Check Parameter
            final List<AnnotationBo> apiAnnotations = apiSpanEvent.getAnnotationBoList();
            assertThat(apiAnnotations.size(), is(1));
            final AnnotationBo apiParameterAnnotation = apiAnnotations.get(0);
            assertThat(apiParameterAnnotation.getKey(), is(AnnotationKey.CACHE_ARGS0.getCode()));
        }
        assertThat(uniqueApiIds.size(), is(numOperations));
    }

}
