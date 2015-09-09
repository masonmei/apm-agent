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

package com.baidu.oped.apm.bootstrap.interceptor;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author emeroad
 */
public class AtomicMaxUpdater {

    private final AtomicInteger maxIndex = new AtomicInteger(-1);

    public boolean updateMax(int max) {
        while (true) {
            final int currentMax = maxIndex.get();
            if (currentMax >= max) {
                return false;
            }
            final boolean update = maxIndex.compareAndSet(currentMax, max);
            if (update) {
                return true;
            }
        }
    }

    public int getIndex() {
        return maxIndex.get();
    }
}
