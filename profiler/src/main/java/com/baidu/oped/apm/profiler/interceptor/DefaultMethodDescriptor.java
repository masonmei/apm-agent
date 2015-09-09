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

package com.baidu.oped.apm.profiler.interceptor;

import com.baidu.oped.apm.bootstrap.interceptor.MethodDescriptor;
import com.baidu.oped.apm.profiler.util.ApiUtils;

import java.util.Arrays;

/**
 * @author emeroad
 */
public class DefaultMethodDescriptor implements MethodDescriptor {
    private String className;

    private String methodName;

    private String[] parameterTypes;

    private String[] parameterVariableName;


    private String parameterDescriptor;

    private String apiDescriptor;

    private int lineNumber;

    private int apiId = 0;

    private String fullName;

    public DefaultMethodDescriptor() {
    }

    public DefaultMethodDescriptor(String className, String methodName, String[] parameterTypes, String[] parameterVariableName) {
        this.className = className;
        this.methodName = methodName;
        this.parameterTypes = parameterTypes;
        this.parameterVariableName = parameterVariableName;
        this.parameterDescriptor = ApiUtils.mergeParameterVariableNameDescription(parameterTypes, parameterVariableName);
        this.apiDescriptor = ApiUtils.mergeApiDescriptor(className, methodName, parameterDescriptor);
    }

    public String getParameterDescriptor() {
        return parameterDescriptor;
    }

    public void setParameterDescriptor(String parameterDescriptor) {
        this.parameterDescriptor = parameterDescriptor;
    }


    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public void setParameterTypes(String[] parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    public void setParameterVariableName(String[] parameterVariableName) {
        this.parameterVariableName = parameterVariableName;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    @Override
    public String getMethodName() {
        return methodName;
    }

    @Override
    public String getClassName() {
        return className;
    }


    public void setClassName(String className) {
        this.className = className;
    }

    @Override
    public String[] getParameterTypes() {
        return parameterTypes;
    }

    @Override
    public String[] getParameterVariableName() {
        return parameterVariableName;
    }


    public int getLineNumber() {
        return lineNumber;
    }

    @Override
    public String getFullName() {
        if (fullName != null) {
            return fullName;
        }
        StringBuilder buffer = new StringBuilder(256);
        buffer.append(className);
        buffer.append(".");
        buffer.append(methodName);
        buffer.append(parameterDescriptor);
        if (lineNumber != -1) {
            buffer.append(":");
            buffer.append(lineNumber);
        }
        fullName = buffer.toString();
        return fullName;
    }

    public void setApiDescriptor(String apiDescriptor) {
        this.apiDescriptor = apiDescriptor;
    }

    @Override
    public String getApiDescriptor() {
        return apiDescriptor;
    }

    @Override
    public void setApiId(int apiId) {
        this.apiId = apiId;
    }

    @Override
    public int getApiId() {
        return apiId;
    }


    @Override
    public String toString() {
        return "DefaultMethodDescriptor{" +
                "className='" + className + '\'' +
                ", methodName='" + methodName + '\'' +
                ", parameterTypes=" + (parameterTypes == null ? null : Arrays.asList(parameterTypes)) +
                ", parameterVariableName=" + (parameterVariableName == null ? null : Arrays.asList(parameterVariableName)) +
                ", parameterDescriptor='" + parameterDescriptor + '\'' +
                ", apiDescriptor='" + apiDescriptor + '\'' +
                ", lineNumber=" + lineNumber +
                ", apiId=" + apiId +
                ", fullName='" + fullName + '\'' +
                '}';
    }
}
