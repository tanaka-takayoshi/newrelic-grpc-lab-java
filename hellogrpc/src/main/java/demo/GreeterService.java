package demo;

import com.newrelic.api.agent.Trace;
import io.grpc.*;
import io.grpc.examples.GreeterGrpc;
import io.grpc.examples.GreeterOuterClass;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.lognet.springboot.grpc.GRpcService;

import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

@Slf4j
@GRpcService(interceptors = { HeaderServerInterceptor.class })
public class GreeterService extends GreeterGrpc.GreeterImplBase {
    @Override
    @Trace(dispatcher = true)
    public void sayHello(GreeterOuterClass.HelloRequest request, StreamObserver<GreeterOuterClass.HelloReply> responseObserver) {
        String message = "Hello " + request.getName();

        if (!request.getName().equals("stop")) {
            String user = "stop";
            HelloWorldClient client = null;
            try {
                client = new HelloWorldClient("localhost", 6565);
                client.greet(user);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            } finally {
                try {
                    client.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        final GreeterOuterClass.HelloReply.Builder replyBuilder = GreeterOuterClass.HelloReply.newBuilder().setMessage(message);
        responseObserver.onNext(replyBuilder.build());
        responseObserver.onCompleted();
    }
}