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

package com.baidu.oped.apm.bootstrap.plugin;

import com.baidu.oped.apm.bootstrap.interceptor.SimpleAroundInterceptor;

public class TestInterceptor implements SimpleAroundInterceptor {
    private final String field;
    
    public TestInterceptor(String field) {
        this.field = field;
    }

    @Override
    public void before(Object target, Object[] args) {
        // TODO Auto-generated method stub

    }

    @Override
    public void after(Object target, Object[] args, Object result, Throwable throwable) {
        // TODO Auto-generated method stub

    }

}
