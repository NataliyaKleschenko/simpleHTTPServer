package knv;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

class Server {
    private static final int BUFFER_SIZE = 256;
    private AsynchronousServerSocketChannel server;
    private final HttpHandler handler;

    Server(HttpHandler handler) {
        this.handler = handler;
    }

    public void bootstrap() {
        try {
            server = AsynchronousServerSocketChannel.open();
            server.bind(new InetSocketAddress("127.0.0.1", 8081));
            while (true) {
                Future<AsynchronousSocketChannel> future = server.accept();
                System.out.println("new client connection");
                try {
                    AsynchronousSocketChannel clientChannel = future.get();
                    while (clientChannel != null && clientChannel.isOpen()) {
                        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
                        StringBuilder builder = new StringBuilder();
                        boolean keepReading = true;
                        while (keepReading) {
                            int readResult = clientChannel.read(buffer).get();
                            keepReading = readResult == BUFFER_SIZE;
                            buffer.flip();
                            CharBuffer decode = StandardCharsets.UTF_8.decode(buffer);
                            builder.append(decode);
                            buffer.clear();
                        }
                        HttpRequest httpRequest = new HttpRequest(builder.toString());
                        HttpResponse httpResponse = new HttpResponse();
                        if (handler != null) {
                            try {
                                //    throw new RuntimeException("oops!"); checking the 500 error
                                String body = this.handler.handle(httpRequest, httpResponse);
                                if (body != null && !body.isBlank()) {
                                    if (httpResponse.getHeaders().get("Content-Type") == null) {
                                        httpResponse.addHeader("Content-Type", "text/html; charset=utf-8");
                                    }
                                    httpResponse.setBody(body);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                httpResponse.setStatusCode(500);
                                httpResponse.setStatus("Internal server error");
                                httpResponse.addHeader("Content-Type", "text/html; charset=utf-8");
                                httpResponse.setBody("<html><body><h1>Error</h1></body></html>");
                            }
                        } else {
                            httpResponse.setStatusCode(404);
                            httpResponse.setStatus("Not found");
                            httpResponse.addHeader("Content-Type", "text/html; charset=utf-8");
                            httpResponse.setBody("<html><body><h1>Resource not found</h1></body></html>");
                        }
                        ByteBuffer response = ByteBuffer.wrap(httpResponse.getBytes());
                        clientChannel.write(response);
                        clientChannel.close();
                    }
                } catch (IOException | InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
