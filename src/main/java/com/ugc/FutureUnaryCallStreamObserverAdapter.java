package com.ugc;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import com.google.protobuf.DynamicMessage;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

public class FutureUnaryCallStreamObserverAdapter implements StreamObserver<DynamicMessage> {
    private static final String ERROR_FIELD_NAME = "error";

    protected final CompletableFuture<DynamicMessage> completableFuture;

    public FutureUnaryCallStreamObserverAdapter(CompletableFuture<DynamicMessage> completableFuture) {
        this.completableFuture = Objects.<CompletableFuture<DynamicMessage>>requireNonNull(completableFuture,
                "completableFuture should not be null");

    }

    public void onNext(DynamicMessage value) {

        this.completableFuture.complete(value);

    }

    public void onError(Throwable t) {
        if (t instanceof StatusRuntimeException) {
            StatusRuntimeException e = (StatusRuntimeException) t;
            if (e.getStatus().equals(Status.DEADLINE_EXCEEDED)) {
                this.completableFuture.completeExceptionally((e));
                return;
            }
        }
        this.completableFuture.completeExceptionally(t);
    }

    public void onCompleted() {
    }

}
