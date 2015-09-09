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

package com.baidu.oped.apm.profiler.context;

import com.baidu.oped.apm.common.ServiceType;

/**
 * @author emeroad
 */
public class RootStackFrame implements StackFrame {

    private final Span span;
    private int stackId;
    private Object frameObject;


    public RootStackFrame(Span span) {
        if (span == null) {
            throw new NullPointerException("span must not be null");
        }
        this.span = span;
    }


    @Override
    public int getStackFrameId() {
        return stackId;
    }

    @Override
    public void setStackFrameId(int stackId) {
        this.stackId = stackId;
    }

    @Override
    public void markBeforeTime() {
        this.span.markBeforeTime();
    }

    @Override
    public long getBeforeTime() {
        return this.span.getStartTime();
    }

    @Override
    public void markAfterTime() {
        this.span.markAfterTime();
    }

    @Override
    public long getAfterTime() {
        return span.getAfterTime();
    }

    @Override
    public int getElapsedTime() {
        return span.getElapsed();
    }


    public Span getSpan() {
        return span;
    }

    @Override
    public void setEndPoint(String endPoint) {
        this.span.setEndPoint(endPoint);
    }

    @Override
    public void setRpc(String rpc) {
        this.span.setRpc(rpc);
    }

    @Override
    public void setApiId(int apiId) {
        this.span.setApiId(apiId);
    }

    @Override
    public void setExceptionInfo(int exceptionId, String exceptionMessage) {
        this.span.setExceptionInfo(exceptionId, exceptionMessage);
    }

    @Override
    public void setServiceType(short serviceType) {
        this.span.setServiceType(serviceType);
    }

    @Override
    public void addAnnotation(Annotation annotation) {
        this.span.addAnnotation(annotation);
    }

    public void setRemoteAddress(String remoteAddress) {
        this.span.setRemoteAddr(remoteAddress);
    }

    @Override
    public void attachFrameObject(Object frameObject) {
        this.frameObject = frameObject;
    }

    @Override
    public Object getFrameObject() {
        return this.frameObject;
    }

    @Override
    public Object detachFrameObject() {
        Object copy = this.frameObject;
        this.frameObject = null;
        return copy;
    }
    
    @Override
    public ServiceType getServiceType() {
        return ServiceType.findServiceType(this.span.getServiceType());
    }
}
