package net.jackiemclean;

import java.io.IOException;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.nio.client.HttpAsyncClient;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.Startup;
import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
@Startup
public class HttpClientFactory {

    private final CloseableHttpAsyncClient client;

    @Inject
    public HttpClientFactory(@ConfigProperty(name = "icecast.username") String username,
            @ConfigProperty(name = "icecast.password") String password) {
        CredentialsProvider creds = new BasicCredentialsProvider();
        creds.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
        this.client = HttpAsyncClients.custom().setDefaultCredentialsProvider(creds).build();
    }

    @Produces
    public HttpAsyncClient makeClient() {
        return client;
    }

    public void onStartup(@Observes StartupEvent startupEvent) {
        client.start();
    }

    public void onShutdown(@Observes ShutdownEvent shutdownEvent) throws IOException {
        client.close();
    }
}
