package net.jackiemclean;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.Startup;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

@ApplicationScoped
@Startup
public class HttpClientFactory {

    private final CloseableHttpClient client;

    @Inject
    public HttpClientFactory() {
        this.client = HttpClients.createDefault();
    }

    @Produces
    public HttpClient makeClient() {
        return client;
    }

    public void onShutdown(@Observes ShutdownEvent shutdownEvent) throws IOException {
        client.close();
    }
}
