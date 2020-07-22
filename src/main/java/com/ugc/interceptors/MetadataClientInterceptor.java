package com.ugc.interceptors;

import java.util.Map;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ClientInterceptors;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.StatusException;

public class MetadataClientInterceptor implements ClientInterceptor {

    private Map<String, String> metadataMap;

    public MetadataClientInterceptor(Map<String, String> metadataMap) {
        this.metadataMap = metadataMap;
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions, Channel channel) {
        return new ClientInterceptors.CheckedForwardingClientCall<ReqT, RespT>(channel.newCall(method, callOptions)) {
            @Override
            protected void checkedStart(Listener<RespT> responseListener, Metadata headers)
                    throws StatusException {
                for (Map.Entry<String, String> entry : metadataMap.entrySet()) {
                    Metadata.Key<String> key = Metadata.Key.of(entry.getKey(), Metadata.ASCII_STRING_MARSHALLER);
                    headers.put(key, entry.getValue());
                }
                delegate().start(responseListener, headers);
            }
        };
    }
}
