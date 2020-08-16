/*
 * Copyright 2015 The gRPC Authors
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

package demo;

import com.newrelic.api.agent.ExternalParameters;
import com.newrelic.api.agent.HttpParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import io.grpc.*;
import io.grpc.examples.GreeterGrpc;
import io.grpc.examples.GreeterOuterClass;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;


public class HelloWorldClient {
  private static final Logger logger = Logger.getLogger(HelloWorldClient.class.getName());

  private final GreeterGrpc.GreeterBlockingStub blockingStub;
  private URI target;
  private ManagedChannel managedChannel;

  /** Construct client for accessing HelloWorld server using the existing channel. */
  public HelloWorldClient(String host, int port) {
    try {
      target = new URI("grpc://"+host+":"+port);
    } catch (URISyntaxException e) {
      e.printStackTrace();
      try {
        target = new URI("grpc://invalid.example/");
      } catch (URISyntaxException uriSyntaxException) {
        //ignore
      }
    }
    managedChannel = ManagedChannelBuilder.forAddress(host, port)
            // Channels are secure by default (via SSL/TLS). For the example we disable TLS to avoid
            // needing certificates.
            .usePlaintext()
            .build();
    ClientInterceptor interceptor = new HeaderClientInterceptor();
    Channel channel = ClientInterceptors.intercept(managedChannel, interceptor);
    blockingStub = GreeterGrpc.newBlockingStub(channel);
  }

  @Trace
  /** Say hello to server. */
  public void greet(String name) throws URISyntaxException {
    // External（外部呼び出し）として認識させるための設定
    // URI（接続先）、procedure、などの情報はClientクラスでないと取れなさそう。
    // interceptorから取得できれば、この処理もinterceptorにまとめられる
    ExternalParameters params = HttpParameters
            .library("grpc")
            .uri(target)
            .procedure("sayHello")
            .noInboundHeaders()
            //.inboundHeaders(inboundHeaders)
            .build();
    NewRelic.getAgent().getTracedMethod().reportAsExternal(params);

    logger.info("Will try to greet " + name + " ...");
    GreeterOuterClass.HelloRequest request = GreeterOuterClass.HelloRequest.newBuilder().setName(name).build();
    GreeterOuterClass.HelloReply response;
    try {
      response = blockingStub.sayHello(request);
    } catch (StatusRuntimeException e) {
      logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
      return;
    }
    logger.info("Greeting: " + response.getMessage());
  }

  HelloWorldClient shutdownNow(){
    managedChannel.shutdownNow();
    return this;
  }

  boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    return managedChannel.awaitTermination(timeout, unit);
  }
}
