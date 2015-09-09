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

package com.baidu.oped.apm.profiler.context;

import com.baidu.oped.apm.bootstrap.context.TraceId;
import com.baidu.oped.apm.common.util.TransactionIdUtils;
import com.baidu.oped.apm.thrift.dto.TIntStringValue;
import com.baidu.oped.apm.thrift.dto.TSpan;

/**
 * Span represent RPC
 *
 * @author netspider
 * @author emeroad
 */
public class Span extends TSpan {
    public Span() {
    }

    public void recordTraceId(final TraceId traceId) {
        if (traceId == null) {
            throw new NullPointerException("traceId must not be null");
        }
        final String agentId = this.getAgentId();
        if (agentId == null) {
            throw new NullPointerException("agentId must not be null");
        }

        final String transactionAgentId = traceId.getAgentId();
        if (!agentId.equals(transactionAgentId)) {
            this.setTransactionId(TransactionIdUtils.formatBytes(transactionAgentId, traceId.getAgentStartTime(), traceId.getTransactionSequence()));
        } else {
            this.setTransactionId(TransactionIdUtils.formatBytes(null, traceId.getAgentStartTime(), traceId.getTransactionSequence()));
        }

        this.setSpanId(traceId.getSpanId());
        final long parentSpanId = traceId.getParentSpanId();
        if (traceId.getParentSpanId() != SpanId.NULL) {
            this.setParentSpanId(parentSpanId);
        }
        this.setFlag(traceId.getFlags());
    }

    public void markBeforeTime() {
        this.setStartTime(System.currentTimeMillis());
    }

    public void markAfterTime() {
        if (!isSetStartTime()) {
            throw new PinpointTraceException("startTime is not set");
        }
        final int after = (int)(System.currentTimeMillis() - this.getStartTime());

        // TODO  have to change int to long
        if (after != 0) {
            this.setElapsed(after);
        }
    }

    public long getAfterTime() {
        if (!isSetStartTime()) {
            throw new PinpointTraceException("startTime is not set");
        }
        return this.getStartTime() + this.getElapsed();
    }


    public void addAnnotation(Annotation annotation) {
        this.addToAnnotations(annotation);
    }

    public void setExceptionInfo(int exceptionClassId, String exceptionMessage) {
        final TIntStringValue exceptionInfo = new TIntStringValue(exceptionClassId);
        if (exceptionMessage != null && !exceptionMessage.isEmpty()) {
            exceptionInfo.setStringValue(exceptionMessage);
        }
        super.setExceptionInfo(exceptionInfo);
    }

    public boolean isSetErrCode() {
        return isSetErr();
    }

    public int getErrCode() {
        return getErr();
    }

    public void setErrCode(int exception) {
        super.setErr(exception);
    }


}
