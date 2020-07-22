package com.ugc.utils;

import java.util.Map;

import com.google.protobuf.DynamicMessage;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DynamicRequest {
    private Integer serviceCallOrder;
    private boolean ignoreThisRequest;
    private String fullMethodName;
    private String host;
    private int port;
    private boolean isSecured;
    private DynamicMessage message;
    private String requestJson;
    private Map<String, String> metadata;
    private Map<String, String> crossRequestKeyToMetadataKey;
    private Map<String, String> responseFieldToCrossRequestKey;
}
