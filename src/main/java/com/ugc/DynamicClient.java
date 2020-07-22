package com.ugc;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.util.JsonFormat;
import com.ugc.exceptions.ApplicationException;
import com.ugc.interceptors.MetadataClientInterceptor;
import com.ugc.marsheller.DynamicMessageMarshaller;
import com.ugc.utils.DynamicRequest;
import com.ugc.utils.GeneralUtil;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.MethodDescriptor.MethodType;
import io.grpc.stub.ClientCalls;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DynamicClient {

    Map<String, String> crossRequestKeyToMetdataValueMap = new HashMap<>();
    Map<String, MethodDescriptor> fullNameToMethodDescriptorMap;

    public DynamicClient(Map<String, MethodDescriptor> fullNameToMethodDescriptorMap) {
        this.fullNameToMethodDescriptorMap = fullNameToMethodDescriptorMap;
    }

    public String invokeService(DynamicRequest request) {

        feedMetadataFromPreviousServiceResponse(crossRequestKeyToMetdataValueMap, request);
        ManagedChannelBuilder<?> channelBuilder = ManagedChannelBuilder.forAddress(request.getHost(), request.getPort())
                .intercept(new MetadataClientInterceptor(request.getMetadata()));

        Channel channel;

        if (request.isSecured()) {
            channel = channelBuilder.build();
        } else {
            channel = channelBuilder.usePlaintext().build();
        }
        if (fullNameToMethodDescriptorMap.get(request.getFullMethodName()) == null) {
            throw new ApplicationException("Descriptor not found for method:{}, Possibly proto file is not provided",
                    request.getFullMethodName());
        }
        io.grpc.MethodDescriptor<DynamicMessage, DynamicMessage> grpcMethodDescriptor = buildGrpcMethodDescriptor(
                fullNameToMethodDescriptorMap, request.getFullMethodName());

        CompletableFuture<DynamicMessage> completableFuture = new CompletableFuture<>();

        StreamObserver<DynamicMessage> streamObserver = new FutureUnaryCallStreamObserverAdapter(
                completableFuture);

        log.info("*****calling service: {}", request.getFullMethodName());
        ClientCalls.asyncUnaryCall(channel.newCall(grpcMethodDescriptor, CallOptions.DEFAULT),
                request.getMessage(), streamObserver);
        String response;
        try {
            response = JsonFormat.printer().print(completableFuture.get());
            log.debug("Response for the service:{} is {}", request.getFullMethodName(), response);
        } catch (Exception e) {
            throw new ApplicationException(e, "Couldnt read response for service {}", request.getFullMethodName());
        }
        handleKeyExport(request, response, crossRequestKeyToMetdataValueMap);
        return response;
    }

    private io.grpc.MethodDescriptor<DynamicMessage, DynamicMessage> buildGrpcMethodDescriptor(
            Map<String, MethodDescriptor> fullNameToMethodDescriptorMap, String fullMethodName) {
        io.grpc.MethodDescriptor.Marshaller<DynamicMessage> requestMarshaller = new DynamicMessageMarshaller(
                fullNameToMethodDescriptorMap.get(fullMethodName).getInputType());

        io.grpc.MethodDescriptor.Marshaller<DynamicMessage> responseMarshaller = new DynamicMessageMarshaller(
                fullNameToMethodDescriptorMap.get(fullMethodName).getOutputType());

        String fullServiceName = fullNameToMethodDescriptorMap.get(fullMethodName).getService()
                .getFullName();
        String methodName = fullNameToMethodDescriptorMap.get(fullMethodName).getName();

        String fullmethodName = io.grpc.MethodDescriptor.generateFullMethodName(fullServiceName, methodName);

        return io.grpc.MethodDescriptor
                .<DynamicMessage, DynamicMessage>newBuilder().setRequestMarshaller(requestMarshaller)
                .setResponseMarshaller(responseMarshaller)
                .setFullMethodName(fullmethodName)
                .setType(MethodType.UNARY).build();
    }

    private void feedMetadataFromPreviousServiceResponse(Map<String, String> crossRequestKeyToMetdataValueMap,
            DynamicRequest request) {
        if (request.getCrossRequestKeyToMetadataKey() != null)
            request.getCrossRequestKeyToMetadataKey().keySet().forEach(key -> request.getMetadata()
                    .put(request.getCrossRequestKeyToMetadataKey().get(key),
                            crossRequestKeyToMetdataValueMap.get(key)));

    }

    private void handleKeyExport(DynamicRequest request, String response,
            Map<String, String> crossRequestKeyToMetdataValueMap) {
        if (request.getResponseFieldToCrossRequestKey() != null)
            request.getResponseFieldToCrossRequestKey().keySet().stream()
                    .forEach(keyHeirarchy -> crossRequestKeyToMetdataValueMap.put(
                            request.getResponseFieldToCrossRequestKey().get(keyHeirarchy),
                            GeneralUtil.getValueFromJson(keyHeirarchy, response)));
    }
}
