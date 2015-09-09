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

package com.baidu.oped.apm.test;

import org.apache.thrift.TBase;

import com.baidu.oped.apm.profiler.context.Span;
import com.baidu.oped.apm.profiler.context.SpanEvent;
import com.baidu.oped.apm.profiler.context.storage.Storage;

/**
 * @author hyungil.jeong
 */
public final class HoldingSpanStorage implements Storage {

    private final PeekableDataSender<? extends TBase<?, ?>> dataSender;

    public HoldingSpanStorage(PeekableDataSender<? extends TBase<?, ?>> dataSender) {
        this.dataSender = dataSender;
    }

    @Override
    public void store(SpanEvent spanEvent) {
        if (spanEvent == null) {
            throw new NullPointerException("spanEvent must not be null");
        }
        this.dataSender.send(spanEvent);
    }

    @Override
    public void store(Span span) {
        if (span == null) {
            throw new NullPointerException("span must not be null");
        }
        this.dataSender.send(span);
    }

}
