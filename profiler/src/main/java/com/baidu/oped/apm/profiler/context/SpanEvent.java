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

import com.baidu.oped.apm.thrift.dto.TIntStringValue;
import com.baidu.oped.apm.thrift.dto.TSpanEvent;

/**
 * Span represent RPC
 *
 * @author netspider
 * @author emeroad
 */
public class SpanEvent extends TSpanEvent {

    private final Span span;

    public SpanEvent(Span span) {
        if (span == null) {
            throw new NullPointerException("span must not be null");
        }
        this.span = span;
    }

    public Span getSpan() {
        return span;
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


    public void markStartTime() {
//        spanEvent.setStartElapsed((int) (startTime - parentSpanStartTime));
        final int startElapsed = (int)(System.currentTimeMillis() - span.getStartTime());
        
        // If startElapsed is 0, logic without mark is useless. Don't do that.
        // The first SpaneEvent of a Sapn could result in 0. Not likely afterwards.
        this.setStartElapsed(startElapsed);
    }

    public long getStartTime() {
        return span.getStartTime() + getStartElapsed();
    }

    public void markAfterTime() {
        if (!isSetStartElapsed()) {
            throw new PinpointTraceException("startTime is not set");
        }
        final int endElapsed = (int)(System.currentTimeMillis() - getStartTime());
        if (endElapsed != 0) {
            this.setEndElapsed(endElapsed);
        }
    }

    public long getAfterTime() {
        if (!isSetStartElapsed()) {
            throw new PinpointTraceException("startTime is not set");
        }
        return span.getStartTime() + getStartElapsed() + getEndElapsed();
    }


}
