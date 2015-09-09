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

import com.baidu.oped.apm.bootstrap.Agent;
import com.baidu.oped.apm.bootstrap.logging.PLogger;
import com.baidu.oped.apm.bootstrap.logging.PLoggerFactory;

/**
 * @author emeroad
 * @author hyungil.jeong
 */
public class LifeCycleEventListener {

    private final static PLogger logger = PLoggerFactory.getLogger(LifeCycleEventListener.class.getName());

    private final Agent agent;

    public LifeCycleEventListener(Agent agent) {
        if (agent == null) {
            throw new IllegalArgumentException("agent must not be null");
        }
        this.agent = agent;
    }

    public void start() {
        logger.info("LifeCycleEventListener start");
        agent.start();
    }

    public void stop() {
        logger.info("LifeCycleEventListener stop");
        agent.stop();
    }
}
