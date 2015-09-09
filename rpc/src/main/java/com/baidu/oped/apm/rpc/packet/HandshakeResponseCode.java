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

package com.baidu.oped.apm.rpc.packet;

public enum HandshakeResponseCode {

    SUCCESS(0, 0, "Success."), 
    SIMPLEX_COMMUNICATION(0, 1, "Simplex Connection successfully established."), 
    DUPLEX_COMMUNICATION(0, 2, "Duplex Connection successfully established."),

    ALREADY_KNOWN(1, 0, "Already Known."),
    ALREADY_SIMPLEX_COMMUNICATION(1, 1, "Already Simplex Connection eastablished."), 
    ALREADY_DUPLEX_COMMUNICATION(1, 2, "Already Duplex Connection established."),
    
    PROPERTY_ERROR(2, 0, "Property error."),

    PROTOCOL_ERROR(3, 0, "Illegal protocol error."),

    UNKNOWN_ERROR(4, 0, "Unkown Error."),

    UNKOWN_CODE(-1, -1, "Unkown Code.");

    private final int code;
    private final int subCode;
    private final String codeMessage;

    private HandshakeResponseCode(int code, int subCode, String codeMessage) {
        this.code = code;
        this.subCode = subCode;
        this.codeMessage = codeMessage;
    }

    public int getCode() {
        return code;
    }

    public int getSubCode() {
        return subCode;
    }

    public String getCodeMessage() {
        return codeMessage;
    }

    public static HandshakeResponseCode getValue(int code, int subCode) {
        for (HandshakeResponseCode value : HandshakeResponseCode.values()) {
            if (code == value.getCode() && subCode == value.getSubCode()) {
                return value;
            }
        }
        
        for (HandshakeResponseCode value : HandshakeResponseCode.values()) {
            if (code == value.getCode() && 0 == value.getSubCode()) {
                return value;
            }
        }

        return UNKOWN_CODE;
    }

}
