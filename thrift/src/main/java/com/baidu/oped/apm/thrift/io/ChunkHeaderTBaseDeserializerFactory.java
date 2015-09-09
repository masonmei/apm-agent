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

package com.baidu.oped.apm.thrift.io;

import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocolFactory;

/**
 * @author koo.taejin
 */
public final class ChunkHeaderTBaseDeserializerFactory implements DeserializerFactory<ChunkHeaderTBaseDeserializer> {

    private static final TBaseLocator DEFAULT_TBASE_LOCATOR = new DefaultTBaseLocator();

    private static final TProtocolFactory DEFAULT_PROTOCOL_FACTORY = new TCompactProtocol.Factory();

    public static final ChunkHeaderTBaseDeserializerFactory DEFAULT_FACTORY = new ChunkHeaderTBaseDeserializerFactory();

    private final TProtocolFactory protocolFactory;
    private TBaseLocator locator;

    public ChunkHeaderTBaseDeserializerFactory() {
        this(DEFAULT_PROTOCOL_FACTORY, DEFAULT_TBASE_LOCATOR);
    }

    public TBaseLocator getLocator() {
        return locator;
    }

    public TProtocolFactory getProtocolFactory() {
        return protocolFactory;
    }

    public ChunkHeaderTBaseDeserializerFactory(TProtocolFactory protocolFactory, TBaseLocator locator) {
        if (protocolFactory == null) {
            throw new NullPointerException("protocolFactory must not be null");
        }
        if (locator == null) {
            throw new NullPointerException("locator must not be null");
        }
        this.protocolFactory = protocolFactory;
        this.locator = locator;
    }


    @Override
    public ChunkHeaderTBaseDeserializer createDeserializer() {
        return new ChunkHeaderTBaseDeserializer(protocolFactory, locator);
    }

}
