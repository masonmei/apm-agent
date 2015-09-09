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

package com.baidu.oped.apm.profiler.modifier.spring.beans.interceptor;

import java.lang.reflect.Method;

import com.baidu.oped.apm.bootstrap.logging.PLogger;
import com.baidu.oped.apm.bootstrap.logging.PLoggerFactory;
import com.baidu.oped.apm.profiler.ClassFileRetransformer;
import com.baidu.oped.apm.profiler.modifier.Modifier;

/**
 * 
 * @author Jongho Moon <jongho.moon@navercorp.com>
 *
 */
public class CreateBeanInstanceInterceptor extends AbstractSpringBeanCreationInterceptor {
    private final PLogger logger = PLoggerFactory.getLogger(getClass());
    
    public CreateBeanInstanceInterceptor(ClassFileRetransformer retransformer, Modifier modifier, TargetBeanFilter filter) {
        super(retransformer, modifier, filter);
    }

    @Override
    public void after(Object target, Object[] args, Object result, Throwable throwable) {
        try {
            if (result == null) {
                return;
            }
            
            Object bean;
            
            try {
                Method getter = result.getClass().getMethod("getWrappedInstance"); 
                bean = getter.invoke(result);
            } catch (Exception e) {
                logger.warn("Fail to get create bean instance", e);
                return;
            }
            
            String beanName = (String)args[0];

            processBean(beanName, bean);
        } catch (Throwable t) {
            logger.warn("Unexpected exception", t);
        }
    }
}
