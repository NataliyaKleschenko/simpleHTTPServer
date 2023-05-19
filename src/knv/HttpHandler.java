package knv;

public interface HttpHandler {
    String handle(HttpRequest request, HttpResponse response);
}
