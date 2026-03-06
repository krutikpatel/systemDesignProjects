package com.ratelimiter.grpc;

import io.grpc.ForwardingServerCall;
import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingInterceptor implements ServerInterceptor {
    private static final Logger log = LoggerFactory.getLogger(LoggingInterceptor.class);

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next
    ) {
        long startNanos = System.nanoTime();
        String method = call.getMethodDescriptor().getFullMethodName();
        String peer = String.valueOf(call.getAttributes().get(io.grpc.Grpc.TRANSPORT_ATTR_REMOTE_ADDR));

        log.info("Inbound RPC method={} peer={}", method, peer);

        ServerCall<ReqT, RespT> loggingCall = new ForwardingServerCall.SimpleForwardingServerCall<>(call) {
            @Override
            public void close(Status status, Metadata trailers) {
                long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
                log.info("Completed RPC method={} status={} durationMs={}", method, status.getCode(), durationMs);
                super.close(status, trailers);
            }
        };

        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<>(
                next.startCall(loggingCall, headers)
        ) {
        };
    }

}
