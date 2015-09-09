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

package com.baidu.oped.apm.profiler.util;

import com.baidu.oped.apm.bootstrap.instrument.Scope;

/**
 * @author emeroad
 */
public class ThreadLocalScope implements Scope {

    private final NamedThreadLocal<Scope> scope;


    public ThreadLocalScope(final ScopeFactory scopeFactory) {
        if (scopeFactory == null) {
            throw new NullPointerException("scopeFactory must not be null");
        }
        this.scope = new NamedThreadLocal<Scope>(scopeFactory.getName()) {
            @Override
            protected Scope initialValue() {
                return scopeFactory.createScope();
            }
        };
    }

    @Override
    public int push() {
        final Scope localScope = getLocalScope();
        return localScope.push();
    }

    @Override
    public int depth() {
        final Scope localScope = getLocalScope();
        return localScope.depth();
    }

    @Override
    public int pop() {
        final Scope localScope = getLocalScope();
        return localScope.pop();
    }

    protected Scope getLocalScope() {
        return scope.get();
    }


    @Override
    public String getName() {
        return scope.getName();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ThreadLocalScope{");
        sb.append("scope=").append(scope.getName());
        sb.append('}');
        return sb.toString();
    }
}