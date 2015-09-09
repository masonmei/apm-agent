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

package com.baidu.oped.apm.profiler.modifier.orm.mybatis.filter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.baidu.oped.apm.bootstrap.instrument.MethodFilter;
import com.baidu.oped.apm.bootstrap.instrument.MethodInfo;

/**
 * @author Hyun Jeong
 */
public class SqlSessionMethodFilter implements MethodFilter {

    private static final boolean TRACK = false;
    private static final boolean DO_NOT_TRACK = true;

    private static final Set<String> WHITE_LIST_API = createWhiteListApi();

    private static Set<String> createWhiteListApi() {
        return new HashSet<String>(Arrays.asList(
                "selectOne",
                "selectList",
                "selectMap",
                "select",
                "insert",
                "update",
                "delete"
//                "commit",
//                "rollback",
//                "flushStatements",
//                "close",
//                "getConfiguration",
//                "getMapper",
//                "getConnection"
        ));
    }

    @Override
    public boolean filter(MethodInfo ctMethod) {
        if (WHITE_LIST_API.contains(ctMethod.getName())) {
            return TRACK;
        }
        return DO_NOT_TRACK;
    }

}
