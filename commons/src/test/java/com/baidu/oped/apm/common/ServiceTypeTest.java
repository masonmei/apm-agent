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

package com.baidu.oped.apm.common;

import org.junit.Assert;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baidu.oped.apm.common.ServiceType;

import java.util.List;

public class ServiceTypeTest {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());


    @Test
    public void testIndexable() {
        logger.debug("{}", ServiceType.TOMCAT.isIndexable());
        logger.debug("{}", ServiceType.BLOC.isIndexable());
        logger.debug("{}", ServiceType.ARCUS.isIndexable());
    }

    @Test
    public void findDesc() {
        String desc = "MYSQL";
        List<ServiceType> mysqlList = ServiceType.findDesc(desc);
        boolean find = false;
        for (ServiceType serviceType : mysqlList) {
            if(serviceType.getDesc().equals(desc)) {
                find = true;
            }
        }
        Assert.assertTrue(find);

        try {
            mysqlList.add(ServiceType.ARCUS);
            Assert.fail();
        } catch (Exception e) {
        }
    }

    @Test
    public void child() {
        ServiceType oracle = ServiceType.ORACLE;


    }

    @Test
    public void test() {
        ServiceType[] values = ServiceType.values();
        for (ServiceType value : values) {
            logger.debug(value.toString() + " " + value.getCode());
        }

    }

}
