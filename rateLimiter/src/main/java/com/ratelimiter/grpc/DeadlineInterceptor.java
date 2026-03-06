package com.ratelimiter.grpc;

import io.grpc.Context;
import io.grpc.Deadline;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeadlineInterceptor implements ServerInterceptor {
    private static final Logger log = LoggerFactory.getLogger(DeadlineInterceptor.class);
    private static final long MIN_BUDGET_MS = 5;

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next
    ) {
        Deadline deadline = Context.current().getDeadline();
        if (deadline == null) {
            return next.startCall(call, headers);
        }

        long remainingMs = deadline.timeRemaining(TimeUnit.MILLISECONDS);
        if (deadline.isExpired() || remainingMs < MIN_BUDGET_MS) {
            String method = call.getMethodDescriptor().getFullMethodName();
            String peer = String.valueOf(call.getAttributes().get(io.grpc.Grpc.TRANSPORT_ATTR_REMOTE_ADDR));
            log.warn("deadline_exceeded method={} peer={} remainingMs={}", method, peer, remainingMs);
            call.close(Status.DEADLINE_EXCEEDED.withDescription("deadline exceeded before processing"), new Metadata());
            return new ServerCall.Listener<>() {
            };
        }

        return next.startCall(call, headers);
    }
}
