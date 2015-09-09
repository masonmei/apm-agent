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

import com.baidu.oped.apm.profiler.DefaultAgent;

/**
 * @author hyungil.jeong
 */
public class TestClassLoaderForClover extends TestClassLoader {
    
    private final String cloverRuntimePackage;

    public TestClassLoaderForClover(DefaultAgent agent, String cloverRuntimePackage) {
        super(agent);
        this.cloverRuntimePackage = cloverRuntimePackage;
    }

    @Override
    protected Class<?> loadClassByDelegation(String name) throws ClassNotFoundException {
        if (name.startsWith(this.cloverRuntimePackage)) {
            return super.delegateToParent(name);
        }
        return super.loadClassByDelegation(name);
    }
    
}
