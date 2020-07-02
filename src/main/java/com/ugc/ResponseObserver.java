package com.ugc;

import com.google.protobuf.DynamicMessage;

import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

/**
 * A {@link StreamObserver} which logs the progress of the rpc and some stats
 * about the results once the rpc has completed. Note that this does *not* log
 * the contents of the response.
 */
@Slf4j
public class ResponseObserver implements StreamObserver<DynamicMessage> {
	private int numResponses;

	public ResponseObserver() {
		numResponses = 0;
	}

	@Override
	public void onCompleted() {
		log.info("Completed rpc with " + numResponses + " response(s)");
	}

	@Override
	public void onError(Throwable t) {
		log.error("Aborted rpc due to error", t);
	}

	@Override
	public void onNext(DynamicMessage message) {
		log.info("Got response message");
		System.out.println("Am I response:" + message);
		++numResponses;
	}
}
