package demo;

import com.google.common.annotations.VisibleForTesting;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Transaction;
import io.grpc.*;

import java.util.logging.Logger;

public class HeaderServerInterceptor implements ServerInterceptor {

    private static final Logger logger = Logger.getLogger(HeaderServerInterceptor.class.getName());

    @VisibleForTesting
    static final Metadata.Key<String> CUSTOM_HEADER_KEY =
            Metadata.Key.of("newrelic", Metadata.ASCII_STRING_MARSHALLER);

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            final Metadata requestHeaders,
            ServerCallHandler<ReqT, RespT> next) {
        logger.info("header received from client:" + requestHeaders);
        //受信時の分散トレーシングpayloadの適用
        final String payload = requestHeaders.get(CUSTOM_HEADER_KEY);
        //ここだとまだTransactionがはじまっていない
        return next.startCall(new ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(call) {
            @Override
            public void sendHeaders(Metadata responseHeaders) {
                //こちら側でpayloadを適用する
                Transaction transaction = NewRelic.getAgent().getTransaction();
                transaction.acceptDistributedTracePayload(payload);
                super.sendHeaders(responseHeaders);
            }
        }, requestHeaders);
    }
}
