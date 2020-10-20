package net.jackiemclean;

import java.net.ConnectException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.ws.rs.core.UriBuilder;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.Startup;
import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
@Startup
public class ActiveListenerMonitor implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(ActiveListenerMonitor.class);
    private static final String MOUNTPOINT_PATH = "admin/listmounts";

    private final Thread monitorThread;
    private final S3Radio radio;
    private final URI listMountsUri;
    private final HttpClient httpClient;

    private volatile boolean shutdown = false;
    private String icecastPassword;

    @Inject
    public ActiveListenerMonitor(S3Radio radio, @ConfigProperty(name = "icecast.baseUri") String icecastBaseUri,
            @ConfigProperty(name = "icecast.password") String icecastPassword, HttpClient httpClient) {
        this.monitorThread = new Thread(this);
        this.radio = radio;
        this.listMountsUri = UriBuilder.fromUri(icecastBaseUri).path(MOUNTPOINT_PATH).build();
        this.httpClient = httpClient;
        this.icecastPassword = icecastPassword;
    }

    public void onStartup(@Observes StartupEvent startupEvent) {
        monitorThread.start();
    }

    public void onShutdown(@Observes ShutdownEvent shutdownEvent) {
        try {
            this.shutdown = true;
            monitorThread.interrupt();
            monitorThread.join();
        } catch (InterruptedException e) {
            LOG.error("exception caught shutting down listener monitor", e);
        }
    }

    @Override
    public void run() {
        while (!shutdown) {
            try {
                Document mountPoints = fetchMountpoints();
                if (mountPoints != null) {
                    radio.getStations().forEach(station -> checkActiveListeners(station, mountPoints));
                }
                Thread.sleep(10_000);
            } catch (InterruptedException e) {
                break;
            } catch (ConnectException e) {
                try {
                    LOG.warn("couldn't connect to icecast, trying again in 30 seconds");
                    Thread.sleep(30_000);
                } catch (InterruptedException e1) {
                    break;
                }
            } catch (Exception e) {
                LOG.error("uncaught exception monitoring active listeners", e);
            }
        }
    }

    private Document fetchMountpoints() throws Exception {
        HttpGet getReq = new HttpGet(listMountsUri);
        String auth = "admin:" + icecastPassword;
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.ISO_8859_1));
        String authHeader = "Basic " + new String(encodedAuth);
        getReq.setHeader(HttpHeaders.AUTHORIZATION, authHeader);
        HttpResponse response = httpClient.execute(getReq);

        if (response.getStatusLine().getStatusCode() != 200) {
            LOG.error("failed to get icecast stats: {}", response.getStatusLine().toString());
            response.getEntity().consumeContent();
            return null;
        }

        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = builderFactory.newDocumentBuilder();

        return builder.parse(response.getEntity().getContent());
    }

    private void checkActiveListeners(Station station, Document mountPoints) {
        String expr = String.format("//source[@mount = '/%s']/listeners/text()", station.getName());
        XPath xPath = XPathFactory.newInstance().newXPath();
        try {
            Object result = xPath.compile(expr).evaluate(mountPoints, XPathConstants.NUMBER);
            if (result == null || ((Number) result).intValue() <= 0) {
                station.standbyOn();
            } else {
                station.standbyOff();
            }
        } catch (XPathExpressionException e) {
            LOG.error("error in xpath evaluation", e);
        }
    }
}
