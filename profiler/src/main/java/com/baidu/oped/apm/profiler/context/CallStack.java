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

import com.baidu.oped.apm.exception.PinpointException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * @author netspider
 * @author emeroad
 */
public class CallStack {

    private static final Logger logger = LoggerFactory.getLogger(CallStack.class);

    private static final int STACK_SIZE = 8;

    private final Span span;
    
    private StackFrame[] stack = new StackFrame[STACK_SIZE];


    private int index = -1;

    public CallStack(Span span) {
        if (span == null) {
            throw new NullPointerException("span  must not be null");
        }
        this.span = span;
    }

    public Span getSpan() {
        return span;
    }

    public int getIndex() {
       return index;
    }


    public StackFrame getCurrentStackFrame() {
        return stack[index];
    }

    public StackFrame getParentStackFrame() {
        if (index > 0) {
            return stack[index - 1];
        }
        return null;
    }

    public void setStackFrame(StackFrame stackFrame) {
        if (stackFrame == null) {
            throw new NullPointerException("stackFrame must not be null");
        }
        stack[index] = stackFrame;
    }

    public int push() {
        checkExtend(index + 1);
        return ++index;
    }

    private void checkExtend(final int index) {
        final StackFrame[] originalStack = this.stack;
        if (index >= originalStack.length) {
            final int copyStackSize = originalStack.length << 1;
            final StackFrame[] copyStack = new StackFrame[copyStackSize];
            System.arraycopy(originalStack, 0, copyStack, 0, originalStack.length);
            this.stack = copyStack;
        }
    }

    public int getStackFrameIndex() {
        return index;
    }

    public void popRoot() {
        pop("popRoot");
        // check empty root index
        if (index != -1) {
            PinpointException ex = createStackException("invalid root stack found", this.index);
            throw ex;
        }
    }

    public StackFrame pop() {
        pop("pop");
        if (index == -1) {
            return null;
        } else {
            return getCurrentStackFrame();
        }
    }

    private void pop(String stackApiPoint) {
        final int currentIndex = this.index;
        final StackFrame[] currentStack = this.stack;
        if (currentIndex >= 0) {
            currentStack[currentIndex] = null;
            this.index = currentIndex - 1;
        } else {
            PinpointException ex = createStackException(stackApiPoint, this.index);
            throw ex;
        }
    }


    private PinpointException createStackException(String stackApiPoint, final int index) {
        final PinpointException ex = new PinpointException("Profiler CallStack check. index:" + index + " stackApiPoint:" + stackApiPoint);
        if (logger.isWarnEnabled()) {
            // need to dump stack.
            logger.warn("invalid callStack found stack dump:{}", this, ex);
        }
        return ex;
    }


    public void currentStackFrameClear() {
        stack[index] = null;
    }

    public StackFrame[] copyStackFrame() {
        // without synchronization arraycopy, last index is null reference
        final StackFrame[] currentStack = this.stack;
        final StackFrame[] copyStack = new StackFrame[currentStack.length];
        System.arraycopy(currentStack, 0, copyStack, 0, currentStack.length);
        return copyStack;
    }

    @Override
    public String toString() {
        final StackFrame[] stack = this.stack;
        return "CallStack{" +
                "stack=" + (stack == null ? null : Arrays.toString(stack)) +
                ", index=" + index +
                '}';
    }
}
