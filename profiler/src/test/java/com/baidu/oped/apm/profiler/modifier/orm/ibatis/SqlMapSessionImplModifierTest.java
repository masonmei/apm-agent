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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.hamcrest.CoreMatchers.*;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.ibatis.sqlmap.client.SqlMapSession;
import com.ibatis.sqlmap.engine.impl.SqlMapClientImpl;
import com.ibatis.sqlmap.engine.impl.SqlMapExecutorDelegate;
import com.ibatis.sqlmap.engine.impl.SqlMapSessionImpl;
import com.ibatis.sqlmap.engine.scope.SessionScope;
import com.baidu.oped.apm.common.AnnotationKey;
import com.baidu.oped.apm.common.bo.AnnotationBo;
import com.baidu.oped.apm.common.bo.SpanEventBo;
import com.baidu.oped.apm.test.junit4.BasePinpointTest;

/**
 * @author Hyun Jeong
 */
public class SqlMapSessionImplModifierTest extends BasePinpointTest {

    public class MockSqlMapExecutorDelegate extends SqlMapExecutorDelegate {
        @Override
        public SessionScope beginSessionScope() {
            return mockSessionScope;
        }
    }

    private SqlMapClientImpl sqlMapClient;

    @Mock
    private MockSqlMapExecutorDelegate mockSqlMapExecutorDelegate;
    @Mock
    private SessionScope mockSessionScope;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(this.mockSqlMapExecutorDelegate.beginSessionScope()).thenReturn(this.mockSessionScope);
        this.sqlMapClient = new SqlMapClientImpl(this.mockSqlMapExecutorDelegate);
    }

    @After
    public void cleanUp() throws Exception {
        this.sqlMapClient = null;
    }

    @Test
    public void exceptionsThrownShouldBeTraced() throws Exception {
        // Given
        final String exceptionInsertId = "insertShouldThrowNPE";
        when(this.mockSqlMapExecutorDelegate.insert(mockSessionScope, exceptionInsertId, null)).thenThrow(new NullPointerException());
        SqlMapSession sqlMapSession = new SqlMapSessionImpl(this.sqlMapClient);
        // When
        try {
            sqlMapSession.insert(exceptionInsertId);
            fail("sqlMapSession.insert() should throw NullPointerException");
        } catch (NullPointerException e) {
            // Then
            final List<SpanEventBo> spanEvents = getCurrentSpanEvents();
            assertThat(spanEvents.size(), is(1));
            final SpanEventBo exceptionSpanEventBo = spanEvents.get(0);
            assertThat(exceptionSpanEventBo.hasException(), is(true));
            assertThat(exceptionSpanEventBo.getExceptionId(), not(0));
        }
    }

    @Test
    public void nullParametersShouldNotBeTraced() throws Exception {
        // Given
        SqlMapSession sqlMapSession = new SqlMapSessionImpl(this.sqlMapClient);
        // When
        sqlMapSession.insert(null);
        sqlMapSession.queryForList(null);
        // Then
        final List<SpanEventBo> spanEvents = getCurrentSpanEvents();
        assertThat(spanEvents.size(), is(2));

        // Check Method
        final SpanEventBo insertSpanEventBo = spanEvents.get(0);
        final SpanEventBo queryForListSpanEventBo = spanEvents.get(1);
        assertThat(insertSpanEventBo.getApiId(), not(0));
        assertThat(queryForListSpanEventBo.getApiId(), not(0));
        assertThat(insertSpanEventBo.getApiId(), not(queryForListSpanEventBo.getApiId()));

        // Check Parameter
        assertNull(insertSpanEventBo.getAnnotationBoList());
        assertNull(queryForListSpanEventBo.getAnnotationBoList());
    }

    @Test
    public void sameApiCallsShouldHaveTheSameApiId() throws Exception {
        // Given
        SqlMapSession sqlMapSession = new SqlMapSessionImpl(this.sqlMapClient);
        // When
        sqlMapSession.insert("insertA");
        sqlMapSession.insert("insertB");
        // Then
        final List<SpanEventBo> spanEvents = getCurrentSpanEvents();
        assertThat(spanEvents.size(), is(2));

        // Check Method
        final SpanEventBo insertASpanEventBo = spanEvents.get(0);
        final SpanEventBo insertBSpanEventBo = spanEvents.get(1);
        assertThat(insertASpanEventBo.getApiId(), not(0));
        assertThat(insertBSpanEventBo.getApiId(), not(0));
        assertThat(insertASpanEventBo.getApiId(), is(insertBSpanEventBo.getApiId()));

    }

    @Test
    public void insertShouldBeTraced() throws Exception {
        // Given
        SqlMapSession sqlMapSession = new SqlMapSessionImpl(this.sqlMapClient);
        // When
        sqlMapSession.insert("insertId");
        sqlMapSession.insert("insertId", new Object());
        // Then
        final List<SpanEventBo> spanEvents = getCurrentSpanEvents();
        assertThat(spanEvents.size(), is(2));

        // Check Method
        final SpanEventBo insertWith1ArgSpanEventBo = spanEvents.get(0);
        final SpanEventBo insertWith2ArgSpanEventBo = spanEvents.get(1);
        assertThat(insertWith1ArgSpanEventBo.getApiId(), not(0));
        assertThat(insertWith2ArgSpanEventBo.getApiId(), not(0));
        assertThat(insertWith1ArgSpanEventBo.getApiId(), not(insertWith2ArgSpanEventBo.getApiId()));

        // Check Parameter
        final List<AnnotationBo> insertWith1ArgAnnotations = insertWith1ArgSpanEventBo.getAnnotationBoList();
        assertThat(insertWith1ArgAnnotations.size(), is(1));
        final AnnotationBo insertWith1ArgParameterAnnotation = insertWith1ArgAnnotations.get(0);
        assertThat(insertWith1ArgParameterAnnotation.getKey(), is(AnnotationKey.CACHE_ARGS0.getCode()));

        final List<AnnotationBo> insertWith2ArgAnnotations = insertWith2ArgSpanEventBo.getAnnotationBoList();
        assertThat(insertWith2ArgAnnotations.size(), is(1));
        final AnnotationBo insertWith2ArgAnnotation = insertWith2ArgAnnotations.get(0);
        assertThat(insertWith2ArgAnnotation.getKey(), is(AnnotationKey.CACHE_ARGS0.getCode()));

    }

    @Test
    public void deleteShouldBeTraced() throws Exception {
        // Given
        SqlMapSession sqlMapSession = new SqlMapSessionImpl(this.sqlMapClient);
        // When
        sqlMapSession.delete("deleteId");
        sqlMapSession.delete("deleteId", new Object());
        // Then
        final List<SpanEventBo> spanEvents = getCurrentSpanEvents();
        assertThat(spanEvents.size(), is(2));

        // Check Method
        final SpanEventBo deleteWith1ArgSpanEvent = spanEvents.get(0);
        final SpanEventBo deleteWith2ArgSpanEvent = spanEvents.get(1);
        assertThat(deleteWith1ArgSpanEvent.getApiId(), not(0));
        assertThat(deleteWith2ArgSpanEvent.getApiId(), not(0));
        assertThat(deleteWith1ArgSpanEvent.getApiId(), not(deleteWith2ArgSpanEvent.getApiId()));

        // Check Parameter
        final List<AnnotationBo> deleteWith1ArgAnnotations = deleteWith1ArgSpanEvent.getAnnotationBoList();
        assertThat(deleteWith1ArgAnnotations.size(), is(1));
        final AnnotationBo deleteWith1ArgParameterAnnotation = deleteWith1ArgAnnotations.get(0);
        assertThat(deleteWith1ArgParameterAnnotation.getKey(), is(AnnotationKey.CACHE_ARGS0.getCode()));

        final List<AnnotationBo> deleteWith2ArgAnnotations = deleteWith2ArgSpanEvent.getAnnotationBoList();
        assertThat(deleteWith2ArgAnnotations.size(), is(1));
        final AnnotationBo deleteWith2ArgAnnotation = deleteWith2ArgAnnotations.get(0);
        assertThat(deleteWith2ArgAnnotation.getKey(), is(AnnotationKey.CACHE_ARGS0.getCode()));
    }

    @Test
    public void updateShouldBeTraced() throws Exception {
        // Given
        SqlMapSession sqlMapSession = new SqlMapSessionImpl(this.sqlMapClient);
        // When
        sqlMapSession.update("updateId");
        sqlMapSession.update("updateId", new Object());
        // Then
        final List<SpanEventBo> spanEvents = getCurrentSpanEvents();
        assertThat(spanEvents.size(), is(2));

        // Check Method
        final SpanEventBo updateWith1ArgSpanEvent = spanEvents.get(0);
        final SpanEventBo updateWith2ArgSpanEvent = spanEvents.get(1);
        assertThat(updateWith1ArgSpanEvent.getApiId(), not(0));
        assertThat(updateWith2ArgSpanEvent.getApiId(), not(0));
        assertThat(updateWith1ArgSpanEvent.getApiId(), not(updateWith2ArgSpanEvent.getApiId()));

        // Check Parameter
        final List<AnnotationBo> updateWith1ArgAnnotations = updateWith1ArgSpanEvent.getAnnotationBoList();
        assertThat(updateWith1ArgAnnotations.size(), is(1));
        final AnnotationBo updateWith1ArgParameterAnnotation = updateWith1ArgAnnotations.get(0);
        assertThat(updateWith1ArgParameterAnnotation.getKey(), is(AnnotationKey.CACHE_ARGS0.getCode()));

        final List<AnnotationBo> updateWith2ArgAnnotations = updateWith2ArgSpanEvent.getAnnotationBoList();
        assertThat(updateWith2ArgAnnotations.size(), is(1));
        final AnnotationBo updateWith2ArgAnnotation = updateWith2ArgAnnotations.get(0);
        assertThat(updateWith2ArgAnnotation.getKey(), is(AnnotationKey.CACHE_ARGS0.getCode()));

    }

    @Test
    public void queryForListShouldBeTraced() throws Exception {
        // Given
        SqlMapSession sqlMapSession = new SqlMapSessionImpl(this.sqlMapClient);
        // When
        sqlMapSession.queryForList("abc");
        // Then
        final List<SpanEventBo> spanEvents = getCurrentSpanEvents();
        assertThat(spanEvents.size(), is(1));

        // Check Method
        final SpanEventBo apiCallSpanEventBo = spanEvents.get(0);
        assertThat(apiCallSpanEventBo.getApiId(), not(0));

        // Check Parameter
        final List<AnnotationBo> annotationBoList = apiCallSpanEventBo.getAnnotationBoList();
        assertThat(annotationBoList.size(), is(1));

        final AnnotationBo parameterAnnotationBo = annotationBoList.get(0);
        assertThat(parameterAnnotationBo.getKey(), is(AnnotationKey.CACHE_ARGS0.getCode()));
    }

    @Test
    public void queryForObjectShouldBeTraced() throws Exception {
        // Given
        SqlMapSession sqlMapSession = new SqlMapSessionImpl(this.sqlMapClient);
        // When
        sqlMapSession.queryForObject("abrgrgfdaghertah", new Object());
        // Then
        final List<SpanEventBo> spanEvents = getCurrentSpanEvents();
        assertThat(spanEvents.size(), is(1));

        // Check Method
        final SpanEventBo apiCallSpanEventBo = spanEvents.get(0);
        assertThat(apiCallSpanEventBo.getApiId(), not(0));

        // Check Parameter
        final List<AnnotationBo> annotationBoList = apiCallSpanEventBo.getAnnotationBoList();
        assertThat(annotationBoList.size(), is(1));

        final AnnotationBo parameterAnnotationBo = annotationBoList.get(0);
        assertThat(parameterAnnotationBo.getKey(), is(AnnotationKey.CACHE_ARGS0.getCode()));
    }

    @Ignore // Changed to trace only query operations
    @Test
    public void transactionsShouldBeTraced() throws Exception {
        // Given
        SqlMapSession sqlMapSession = new SqlMapSessionImpl(this.sqlMapClient);
        // When
        sqlMapSession.startTransaction();
        sqlMapSession.commitTransaction();
        sqlMapSession.endTransaction();
        // Then
        final List<SpanEventBo> spanEvents = getCurrentSpanEvents();
        assertThat(spanEvents.size(), is(3));

        // Check Method
        final SpanEventBo startTransactionSpanEventBo = spanEvents.get(0);
        final SpanEventBo commitTransactionSpanEventBo = spanEvents.get(1);
        final SpanEventBo endTransactionSpanEventBo = spanEvents.get(2);

        assertThat(startTransactionSpanEventBo.getApiId(), not(0));
        assertThat(commitTransactionSpanEventBo.getApiId(), not(0));
        assertThat(endTransactionSpanEventBo.getApiId(), not(0));

        assertThat(startTransactionSpanEventBo.getApiId(), not(commitTransactionSpanEventBo.getApiId()));
        assertThat(commitTransactionSpanEventBo.getApiId(), not(endTransactionSpanEventBo.getApiId()));
        assertThat(endTransactionSpanEventBo.getApiId(), not(startTransactionSpanEventBo.getApiId()));

        // Check Parameter
        assertNull(startTransactionSpanEventBo.getAnnotationBoList());
        assertNull(commitTransactionSpanEventBo.getAnnotationBoList());
        assertNull(endTransactionSpanEventBo.getAnnotationBoList());
    }

    @Ignore // Changed to trace only query operations
    @Test
    public void closeShouldBeTraced() throws Exception {
        // Given
        SqlMapSession sqlMapSession = new SqlMapSessionImpl(this.sqlMapClient);
        // When
        sqlMapSession.close();
        // Then
        final List<SpanEventBo> spanEvents = getCurrentSpanEvents();
        assertThat(spanEvents.size(), is(1));

        // Check Method
        final SpanEventBo closeSpanEventBo = spanEvents.get(0);
        assertThat(closeSpanEventBo.getApiId(), not(0));

        // Check Parameter
        assertNull(closeSpanEventBo.getAnnotationBoList());
    }
}
