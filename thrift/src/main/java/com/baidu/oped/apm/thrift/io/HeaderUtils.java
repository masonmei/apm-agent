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

/**
 * @author emeroad
 */
final class HeaderUtils {
    public static final int OK = Header.SIGNATURE;
    // TODO Maybe PASS_L4 should be a modifiable variable
    public static final int PASS_L4 = 85; // Udp
    public static final int FAIL = 0;

    private HeaderUtils() {
    }

    public static int validateSignature(byte signature) {
        if (Header.SIGNATURE == signature) {
            return OK;
        } else if (PASS_L4 == signature) {
            return PASS_L4;
        }
        return FAIL;
    }
}
