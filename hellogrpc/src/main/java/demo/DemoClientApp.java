package demo;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.concurrent.TimeUnit;

public class DemoClientApp {
    public static void main(String[] args) throws Exception {
        String user = "world";
        // Access a service running on the local machine on port 50051
        String host = "localhost";
        int port = 6565;
        // Allow passing in the user and target strings as command line arguments
        if (args.length > 0) {
            if ("--help".equals(args[0])) {
                System.err.println("Usage: [name [target]]");
                System.err.println("");
                System.err.println("  name    The name you wish to be greeted by. Defaults to " + user);
                System.err.println("  target  The server to connect to. Defaults to " + host + ":" + port);
                System.exit(1);
            }
            user = args[0];
        }
        if (args.length > 3) {
            host = args[1];
            port = Integer.valueOf(args[2]);
        }

        HelloWorldClient client = null;
        try {
            client = new HelloWorldClient(host, port);
            client.greet(user);
        } finally {
            // ManagedChannels use resources like threads and TCP connections. To prevent leaking these
            // resources the channel should be shut down when it will no longer be used. If it may be used
            // again leave it running.
            if (client != null)
                client.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}
