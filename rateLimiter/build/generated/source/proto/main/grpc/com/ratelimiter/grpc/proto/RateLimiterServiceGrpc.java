package com.ratelimiter.grpc.proto;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.60.0)",
    comments = "Source: ratelimiter/v1/rate_limiter.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class RateLimiterServiceGrpc {

  private RateLimiterServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "ratelimiter.v1.RateLimiterService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.ratelimiter.grpc.proto.CheckRateLimitRequest,
      com.ratelimiter.grpc.proto.CheckRateLimitResponse> getCheckRateLimitMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CheckRateLimit",
      requestType = com.ratelimiter.grpc.proto.CheckRateLimitRequest.class,
      responseType = com.ratelimiter.grpc.proto.CheckRateLimitResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.ratelimiter.grpc.proto.CheckRateLimitRequest,
      com.ratelimiter.grpc.proto.CheckRateLimitResponse> getCheckRateLimitMethod() {
    io.grpc.MethodDescriptor<com.ratelimiter.grpc.proto.CheckRateLimitRequest, com.ratelimiter.grpc.proto.CheckRateLimitResponse> getCheckRateLimitMethod;
    if ((getCheckRateLimitMethod = RateLimiterServiceGrpc.getCheckRateLimitMethod) == null) {
      synchronized (RateLimiterServiceGrpc.class) {
        if ((getCheckRateLimitMethod = RateLimiterServiceGrpc.getCheckRateLimitMethod) == null) {
          RateLimiterServiceGrpc.getCheckRateLimitMethod = getCheckRateLimitMethod =
              io.grpc.MethodDescriptor.<com.ratelimiter.grpc.proto.CheckRateLimitRequest, com.ratelimiter.grpc.proto.CheckRateLimitResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CheckRateLimit"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.ratelimiter.grpc.proto.CheckRateLimitRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.ratelimiter.grpc.proto.CheckRateLimitResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RateLimiterServiceMethodDescriptorSupplier("CheckRateLimit"))
              .build();
        }
      }
    }
    return getCheckRateLimitMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.ratelimiter.grpc.proto.ReloadConfigRequest,
      com.ratelimiter.grpc.proto.ReloadConfigResponse> getReloadConfigMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ReloadConfig",
      requestType = com.ratelimiter.grpc.proto.ReloadConfigRequest.class,
      responseType = com.ratelimiter.grpc.proto.ReloadConfigResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.ratelimiter.grpc.proto.ReloadConfigRequest,
      com.ratelimiter.grpc.proto.ReloadConfigResponse> getReloadConfigMethod() {
    io.grpc.MethodDescriptor<com.ratelimiter.grpc.proto.ReloadConfigRequest, com.ratelimiter.grpc.proto.ReloadConfigResponse> getReloadConfigMethod;
    if ((getReloadConfigMethod = RateLimiterServiceGrpc.getReloadConfigMethod) == null) {
      synchronized (RateLimiterServiceGrpc.class) {
        if ((getReloadConfigMethod = RateLimiterServiceGrpc.getReloadConfigMethod) == null) {
          RateLimiterServiceGrpc.getReloadConfigMethod = getReloadConfigMethod =
              io.grpc.MethodDescriptor.<com.ratelimiter.grpc.proto.ReloadConfigRequest, com.ratelimiter.grpc.proto.ReloadConfigResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ReloadConfig"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.ratelimiter.grpc.proto.ReloadConfigRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.ratelimiter.grpc.proto.ReloadConfigResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RateLimiterServiceMethodDescriptorSupplier("ReloadConfig"))
              .build();
        }
      }
    }
    return getReloadConfigMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.ratelimiter.grpc.proto.HealthCheckRequest,
      com.ratelimiter.grpc.proto.HealthCheckResponse> getHealthCheckMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "HealthCheck",
      requestType = com.ratelimiter.grpc.proto.HealthCheckRequest.class,
      responseType = com.ratelimiter.grpc.proto.HealthCheckResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.ratelimiter.grpc.proto.HealthCheckRequest,
      com.ratelimiter.grpc.proto.HealthCheckResponse> getHealthCheckMethod() {
    io.grpc.MethodDescriptor<com.ratelimiter.grpc.proto.HealthCheckRequest, com.ratelimiter.grpc.proto.HealthCheckResponse> getHealthCheckMethod;
    if ((getHealthCheckMethod = RateLimiterServiceGrpc.getHealthCheckMethod) == null) {
      synchronized (RateLimiterServiceGrpc.class) {
        if ((getHealthCheckMethod = RateLimiterServiceGrpc.getHealthCheckMethod) == null) {
          RateLimiterServiceGrpc.getHealthCheckMethod = getHealthCheckMethod =
              io.grpc.MethodDescriptor.<com.ratelimiter.grpc.proto.HealthCheckRequest, com.ratelimiter.grpc.proto.HealthCheckResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "HealthCheck"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.ratelimiter.grpc.proto.HealthCheckRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.ratelimiter.grpc.proto.HealthCheckResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RateLimiterServiceMethodDescriptorSupplier("HealthCheck"))
              .build();
        }
      }
    }
    return getHealthCheckMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static RateLimiterServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<RateLimiterServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<RateLimiterServiceStub>() {
        @java.lang.Override
        public RateLimiterServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new RateLimiterServiceStub(channel, callOptions);
        }
      };
    return RateLimiterServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static RateLimiterServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<RateLimiterServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<RateLimiterServiceBlockingStub>() {
        @java.lang.Override
        public RateLimiterServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new RateLimiterServiceBlockingStub(channel, callOptions);
        }
      };
    return RateLimiterServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static RateLimiterServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<RateLimiterServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<RateLimiterServiceFutureStub>() {
        @java.lang.Override
        public RateLimiterServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new RateLimiterServiceFutureStub(channel, callOptions);
        }
      };
    return RateLimiterServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public interface AsyncService {

    /**
     */
    default void checkRateLimit(com.ratelimiter.grpc.proto.CheckRateLimitRequest request,
        io.grpc.stub.StreamObserver<com.ratelimiter.grpc.proto.CheckRateLimitResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCheckRateLimitMethod(), responseObserver);
    }

    /**
     */
    default void reloadConfig(com.ratelimiter.grpc.proto.ReloadConfigRequest request,
        io.grpc.stub.StreamObserver<com.ratelimiter.grpc.proto.ReloadConfigResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getReloadConfigMethod(), responseObserver);
    }

    /**
     */
    default void healthCheck(com.ratelimiter.grpc.proto.HealthCheckRequest request,
        io.grpc.stub.StreamObserver<com.ratelimiter.grpc.proto.HealthCheckResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getHealthCheckMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service RateLimiterService.
   */
  public static abstract class RateLimiterServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return RateLimiterServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service RateLimiterService.
   */
  public static final class RateLimiterServiceStub
      extends io.grpc.stub.AbstractAsyncStub<RateLimiterServiceStub> {
    private RateLimiterServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected RateLimiterServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new RateLimiterServiceStub(channel, callOptions);
    }

    /**
     */
    public void checkRateLimit(com.ratelimiter.grpc.proto.CheckRateLimitRequest request,
        io.grpc.stub.StreamObserver<com.ratelimiter.grpc.proto.CheckRateLimitResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCheckRateLimitMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void reloadConfig(com.ratelimiter.grpc.proto.ReloadConfigRequest request,
        io.grpc.stub.StreamObserver<com.ratelimiter.grpc.proto.ReloadConfigResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getReloadConfigMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void healthCheck(com.ratelimiter.grpc.proto.HealthCheckRequest request,
        io.grpc.stub.StreamObserver<com.ratelimiter.grpc.proto.HealthCheckResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getHealthCheckMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service RateLimiterService.
   */
  public static final class RateLimiterServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<RateLimiterServiceBlockingStub> {
    private RateLimiterServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected RateLimiterServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new RateLimiterServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public com.ratelimiter.grpc.proto.CheckRateLimitResponse checkRateLimit(com.ratelimiter.grpc.proto.CheckRateLimitRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCheckRateLimitMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.ratelimiter.grpc.proto.ReloadConfigResponse reloadConfig(com.ratelimiter.grpc.proto.ReloadConfigRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getReloadConfigMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.ratelimiter.grpc.proto.HealthCheckResponse healthCheck(com.ratelimiter.grpc.proto.HealthCheckRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getHealthCheckMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service RateLimiterService.
   */
  public static final class RateLimiterServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<RateLimiterServiceFutureStub> {
    private RateLimiterServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected RateLimiterServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new RateLimiterServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.ratelimiter.grpc.proto.CheckRateLimitResponse> checkRateLimit(
        com.ratelimiter.grpc.proto.CheckRateLimitRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCheckRateLimitMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.ratelimiter.grpc.proto.ReloadConfigResponse> reloadConfig(
        com.ratelimiter.grpc.proto.ReloadConfigRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getReloadConfigMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.ratelimiter.grpc.proto.HealthCheckResponse> healthCheck(
        com.ratelimiter.grpc.proto.HealthCheckRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getHealthCheckMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_CHECK_RATE_LIMIT = 0;
  private static final int METHODID_RELOAD_CONFIG = 1;
  private static final int METHODID_HEALTH_CHECK = 2;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final AsyncService serviceImpl;
    private final int methodId;

    MethodHandlers(AsyncService serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_CHECK_RATE_LIMIT:
          serviceImpl.checkRateLimit((com.ratelimiter.grpc.proto.CheckRateLimitRequest) request,
              (io.grpc.stub.StreamObserver<com.ratelimiter.grpc.proto.CheckRateLimitResponse>) responseObserver);
          break;
        case METHODID_RELOAD_CONFIG:
          serviceImpl.reloadConfig((com.ratelimiter.grpc.proto.ReloadConfigRequest) request,
              (io.grpc.stub.StreamObserver<com.ratelimiter.grpc.proto.ReloadConfigResponse>) responseObserver);
          break;
        case METHODID_HEALTH_CHECK:
          serviceImpl.healthCheck((com.ratelimiter.grpc.proto.HealthCheckRequest) request,
              (io.grpc.stub.StreamObserver<com.ratelimiter.grpc.proto.HealthCheckResponse>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  public static final io.grpc.ServerServiceDefinition bindService(AsyncService service) {
    return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
        .addMethod(
          getCheckRateLimitMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.ratelimiter.grpc.proto.CheckRateLimitRequest,
              com.ratelimiter.grpc.proto.CheckRateLimitResponse>(
                service, METHODID_CHECK_RATE_LIMIT)))
        .addMethod(
          getReloadConfigMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.ratelimiter.grpc.proto.ReloadConfigRequest,
              com.ratelimiter.grpc.proto.ReloadConfigResponse>(
                service, METHODID_RELOAD_CONFIG)))
        .addMethod(
          getHealthCheckMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.ratelimiter.grpc.proto.HealthCheckRequest,
              com.ratelimiter.grpc.proto.HealthCheckResponse>(
                service, METHODID_HEALTH_CHECK)))
        .build();
  }

  private static abstract class RateLimiterServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    RateLimiterServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.ratelimiter.grpc.proto.RateLimiterProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("RateLimiterService");
    }
  }

  private static final class RateLimiterServiceFileDescriptorSupplier
      extends RateLimiterServiceBaseDescriptorSupplier {
    RateLimiterServiceFileDescriptorSupplier() {}
  }

  private static final class RateLimiterServiceMethodDescriptorSupplier
      extends RateLimiterServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    RateLimiterServiceMethodDescriptorSupplier(java.lang.String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (RateLimiterServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new RateLimiterServiceFileDescriptorSupplier())
              .addMethod(getCheckRateLimitMethod())
              .addMethod(getReloadConfigMethod())
              .addMethod(getHealthCheckMethod())
              .build();
        }
      }
    }
    return result;
  }
}
