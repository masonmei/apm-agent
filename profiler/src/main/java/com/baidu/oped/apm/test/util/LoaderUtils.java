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

package com.baidu.oped.apm.test.util;

import javassist.ClassPool;
import javassist.Loader;

import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * @author emeroad
 */
public final class LoaderUtils {

    private static final SecurityManager SECURITY_MANAGER = System.getSecurityManager();

    private LoaderUtils() {
    }

    public static Loader createLoader(final ClassPool classPool) {
        if (classPool == null) {
            throw new NullPointerException("classPool must not be null");
        }
        Loader loader;
        if (SECURITY_MANAGER != null) {
            loader = AccessController.doPrivileged(new PrivilegedAction<Loader>() {
                public Loader run() {
                    return new Loader(classPool);
                }
            });
        } else {
            loader = new Loader(classPool);
        }
        loader.delegateLoadingOf("org.apache.log4j.");
        return loader;
    }

}
