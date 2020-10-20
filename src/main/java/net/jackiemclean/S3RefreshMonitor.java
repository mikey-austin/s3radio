package net.jackiemclean;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.Startup;
import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
@Startup
public class S3RefreshMonitor implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(S3RefreshMonitor.class);

    private final Thread monitorThread;
    private final S3Radio radio;

    private volatile boolean shutdown = false;

    @Inject
    public S3RefreshMonitor(S3Radio radio) {
        this.monitorThread = new Thread(this);
        this.radio = radio;
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
                radio.refresh();
                Thread.sleep(600_000);
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                LOG.error("uncaught exception monitoring active listeners", e);
            }
        }
    }
}
