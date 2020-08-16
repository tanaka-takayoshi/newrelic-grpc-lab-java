package demo;

import com.google.common.annotations.VisibleForTesting;
import com.newrelic.api.agent.NewRelic;
import io.grpc.*;

import java.util.logging.Logger;

public class HeaderClientInterceptor implements ClientInterceptor {

    @VisibleForTesting
    static final Metadata.Key<String> CUSTOM_HEADER_KEY =
            Metadata.Key.of("newrelic", Metadata.ASCII_STRING_MARSHALLER);

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
                                                               CallOptions callOptions, Channel next) {
        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {

            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                //送信時の分散トレーシング用のヘッダーの挿入
                String payload = NewRelic.getAgent().getTransaction().createDistributedTracePayload().text();
                headers.put(CUSTOM_HEADER_KEY, payload);
                super.start(new ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT>(responseListener) {
                    @Override
                    public void onHeaders(Metadata headers) {
                        /**
                         * if you don't need receive header from server,
                         * you can use {@link io.grpc.stub.MetadataUtils#attachHeaders}
                         * directly to send header
                         */
                        super.onHeaders(headers);
                    }
                }, headers);
            }
        };
    }
}
