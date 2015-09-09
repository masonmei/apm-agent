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

package com.baidu.oped.apm.profiler.interceptor.bci.mock;

import com.baidu.oped.apm.common.util.BytesUtils;
import com.baidu.oped.apm.profiler.interceptor.aspect.Aspect;
import com.baidu.oped.apm.profiler.interceptor.aspect.JointPoint;
import com.baidu.oped.apm.profiler.interceptor.aspect.PointCut;

import java.util.Map;

/**
 * @author emeroad
 */
@Aspect
public abstract class TestAspect extends Original {

    @PointCut
    public void testVoid() {
        touchBefore();
        __testVoid();
        touchAfter();
    }

    @JointPoint
    abstract void __testVoid();


    @PointCut
    public int testInt() {
        touchBefore();
        final int result = __testInt();
        touchAfter();
        return result;
    }

    @JointPoint
    abstract int __testInt();


    @PointCut
    public String testString() {
        touchBefore();
        String s = __testString();
        touchAfter();
        return s;
    }

    @JointPoint
    abstract String __testString();

    @PointCut
    public int testUtilMethod() {
        touchBefore();
        int result = __testInt();
        utilMethod();
        touchAfter();
        return result;
    }

    private String utilMethod() {
        return "Util";
    }

    @PointCut
    public void testNoTouch() {
         __testVoid();
    }

    @PointCut
    public void testInternalMethod() {
        __testVoid();
    }

    @PointCut
    public void testMethodCall() {
        BytesUtils.toBytes("test");
        __testMethodCall();
    }

    @JointPoint
    abstract void __testMethodCall();

    @PointCut
    public Map<String, String> testGeneric() {
        return null;
    }

}
