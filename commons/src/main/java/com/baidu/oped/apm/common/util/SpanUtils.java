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

package com.baidu.oped.apm.common.util;

import static com.baidu.oped.apm.common.PinpointConstants.*;

import com.baidu.oped.apm.common.buffer.AutomaticBuffer;
import com.baidu.oped.apm.common.buffer.Buffer;
import com.baidu.oped.apm.thrift.dto.TSpan;
import com.baidu.oped.apm.thrift.dto.TSpanChunk;

/**
 * @author emeroad
 */
public final class SpanUtils {
    private SpanUtils() {
    }

    @Deprecated
    public static byte[] getAgentIdTraceIndexRowKey(String agentId, long timestamp) {
        if (agentId == null) {
            throw new IllegalArgumentException("agentId must not null");
        }
        final byte[] bAgentId = BytesUtils.toBytes(agentId);
        return RowKeyUtils.concatFixedByteAndLong(bAgentId, AGENT_NAME_MAX_LEN, TimeUtils.reverseTimeMillis(timestamp));
    }

    public static byte[] getApplicationTraceIndexRowKey(String applicationName, long timestamp) {
        if (applicationName == null) {
            throw new IllegalArgumentException("agentId must not null");
        }
        final byte[] bApplicationName = BytesUtils.toBytes(applicationName);
        return RowKeyUtils.concatFixedByteAndLong(bApplicationName, AGENT_NAME_MAX_LEN, TimeUtils.reverseTimeMillis(timestamp));
    }

    public static byte[] getTraceIndexRowKey(byte[] agentId, long timestamp) {
        if (agentId == null) {
            throw new NullPointerException("agentId must not be null");
        }
        return RowKeyUtils.concatFixedByteAndLong(agentId, AGENT_NAME_MAX_LEN, TimeUtils.reverseTimeMillis(timestamp));
    }

    public static byte[] getVarTransactionId(TSpan span) {
        if (span == null) {
            throw new NullPointerException("span must not be null");
        }
        final byte[] transactionIdBytes = span.getTransactionId();
        TransactionId transactionId = TransactionIdUtils.parseTransactionId(transactionIdBytes);
        String agentId = transactionId.getAgentId();
        if (agentId == null) {
            agentId = span.getAgentId();
        }

        final Buffer buffer= new AutomaticBuffer(32);
        buffer.putPrefixedString(agentId);
        buffer.putSVar(transactionId.getAgentStartTime());
        buffer.putVar(transactionId.getTransactionSequence());
        return buffer.getBuffer();
    }

    public static byte[] getTransactionId(TSpan span) {
        if (span == null) {
            throw new NullPointerException("span must not be null");
        }
        final byte[] transactionIdBytes = span.getTransactionId();
        TransactionId transactionId = TransactionIdUtils.parseTransactionId(transactionIdBytes);
        String agentId = transactionId.getAgentId();
        if (agentId == null) {
            agentId = span.getAgentId();
        }
        return BytesUtils.stringLongLongToBytes(agentId, AGENT_NAME_MAX_LEN, transactionId.getAgentStartTime(), transactionId.getTransactionSequence());

    }

    public static byte[] getTransactionId(TSpanChunk spanChunk) {
        if (spanChunk == null) {
            throw new NullPointerException("spanChunk must not be null");
        }
        final byte[] transactionIdBytes = spanChunk.getTransactionId();
        final TransactionId transactionId = TransactionIdUtils.parseTransactionId(transactionIdBytes);
        String agentId = transactionId.getAgentId();
        if (agentId == null) {
            agentId = spanChunk.getAgentId();
        }
        return BytesUtils.stringLongLongToBytes(agentId, AGENT_NAME_MAX_LEN, transactionId.getAgentStartTime(), transactionId.getTransactionSequence());
    }
}
