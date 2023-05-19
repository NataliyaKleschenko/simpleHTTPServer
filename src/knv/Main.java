package knv;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Main {
    public static void main(String[] args) {
        new Server().bootstrap();
    }
}

class Server {
    private static final int BUFFER_SIZE = 256;
    private AsynchronousServerSocketChannel server;
    private static final String HEADERS =
            """
                    HTTP/1.1 200 OK
                    Server: simpleHTTPServer
                    Content-type: text/html
                    Content-Length: %s
                    Connection: close

                    """;

    public void bootstrap() {
        try {
            server = AsynchronousServerSocketChannel.open();
            server.bind(new InetSocketAddress("127.0.0.1", 8081));
            while (true) {
                Future<AsynchronousSocketChannel> future = server.accept();
                System.out.println("new client thread");
                try {
                    AsynchronousSocketChannel clientChannel = future.get(60, TimeUnit.SECONDS);
                    while (clientChannel != null && clientChannel.isOpen()) {
                        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
                        StringBuilder builder = new StringBuilder();
                        boolean keepReading = true;
                        while (keepReading) {
                            clientChannel.read(buffer).get();
                            int position = buffer.position();
                            keepReading = position == BUFFER_SIZE;
                            byte[] array = keepReading ? buffer.array() : Arrays.copyOfRange(buffer.array(), 0, position);
                            builder.append(new String(buffer.array()));
                            buffer.clear();
                        }
                        final String body = "<html><body><h1>Hello, simpleHTTPServer</h1></body></html>";
                        final String page = String.format(HEADERS, body.length()) + body;
                        ByteBuffer response = ByteBuffer.wrap(page.getBytes());
                        clientChannel.write(response);
                        clientChannel.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (TimeoutException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


