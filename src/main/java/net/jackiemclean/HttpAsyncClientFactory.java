package net.jackiemclean;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.nio.client.HttpAsyncClient;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Dependent
public class HttpAsyncClientFactory {

    private final String username;
    private final String password;

    @Inject
    public HttpAsyncClientFactory(@ConfigProperty(name = "icecast.username") String username,
            @ConfigProperty(name = "icecast.password") String password) {
        this.username = username;
        this.password = password;
    }

    @Produces
    @Singleton
    public HttpAsyncClient makeClient() {
        CredentialsProvider creds = new BasicCredentialsProvider();
        creds.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
        CloseableHttpAsyncClient client = HttpAsyncClients.custom().setDefaultCredentialsProvider(creds).build();
        client.start();
        return client;
    }
}
