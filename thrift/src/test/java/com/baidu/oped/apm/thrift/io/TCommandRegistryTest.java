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

import org.junit.Assert;

import org.apache.thrift.TBase;
import org.apache.thrift.TException;
import org.junit.Test;

import com.baidu.oped.apm.thrift.dto.TResult;
import com.baidu.oped.apm.thrift.dto.command.TCommandThreadDump;
import com.baidu.oped.apm.thrift.io.Header;
import com.baidu.oped.apm.thrift.io.TCommandRegistry;
import com.baidu.oped.apm.thrift.io.TCommandType;
import com.baidu.oped.apm.thrift.io.TCommandTypeVersion;

/**
 * @author koo.taejin
 */
public class TCommandRegistryTest {

    @Test
    public void registryTest1() {
        TCommandRegistry registry = new TCommandRegistry(TCommandTypeVersion.UNKNOWN);

        Assert.assertFalse(registry.isSupport(TCommandType.RESULT.getType()));
        Assert.assertFalse(registry.isSupport(TCommandType.THREAD_DUMP.getType()));

        Assert.assertFalse(registry.isSupport(TResult.class));
        Assert.assertFalse(registry.isSupport(TCommandThreadDump.class));
    }

    @Test(expected = TException.class)
    public void registryTest2() throws TException {
        TCommandRegistry registry = new TCommandRegistry(TCommandTypeVersion.UNKNOWN);

        registry.headerLookup(new TResult());
    }

    @Test(expected = TException.class)
    public void registryTest3() throws TException {
        TCommandRegistry registry = new TCommandRegistry(TCommandTypeVersion.UNKNOWN);

        registry.tBaseLookup(TCommandType.RESULT.getType());
    }

    @Test
    public void registryTest4() {
        TCommandRegistry registry = new TCommandRegistry(TCommandTypeVersion.V_1_0_2_SNAPSHOT);

        Assert.assertTrue(registry.isSupport(TCommandType.RESULT.getType()));
        Assert.assertTrue(registry.isSupport(TCommandType.THREAD_DUMP.getType()));

        Assert.assertTrue(registry.isSupport(TResult.class));
        Assert.assertTrue(registry.isSupport(TCommandThreadDump.class));
    }

    @Test
    public void registryTest5() throws TException {
        TCommandRegistry registry = new TCommandRegistry(TCommandTypeVersion.V_1_0_2_SNAPSHOT);

        Header header = registry.headerLookup(new TResult());
        Assert.assertNotNull(header);
    }

    @Test
    public void registryTest6() throws TException {
        TCommandRegistry registry = new TCommandRegistry(TCommandTypeVersion.V_1_0_2_SNAPSHOT);

        TBase tBase = registry.tBaseLookup(TCommandType.RESULT.getType());
        Assert.assertEquals(tBase.getClass(), TResult.class);

        tBase = registry.tBaseLookup(TCommandType.THREAD_DUMP.getType());
        Assert.assertEquals(tBase.getClass(), TCommandThreadDump.class);
    }

}
