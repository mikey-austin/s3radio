package net.jackiemclean;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.Startup;
import io.quarkus.runtime.StartupEvent;

@Startup
@ApplicationScoped
public class S3Radio implements Radio {

    private static final Logger LOG = LoggerFactory.getLogger(S3Radio.class);

    private final AmazonS3 s3Client;
    private final String bucketName;
    private final Set<Station> stations;
    private final S3StationFactory stationFactory;

    @Inject
    public S3Radio(AmazonS3 s3Client, @ConfigProperty(name = "s3.bucketName") String bucketName,
            S3StationFactory stationFactory) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
        this.stations = ConcurrentHashMap.newKeySet();
        this.stationFactory = stationFactory;
    }

    public void onStartup(@Observes StartupEvent startupEvent) {
        Set<Station> stationsFromBucket = readFromS3();
        merge(stationsFromBucket);
    }

    private void merge(Set<Station> stationsFromBucket) {
        Set<Station> removedStations = new HashSet<>(stations);
        removedStations.removeAll(stationsFromBucket);

        stations.addAll(stationsFromBucket);
        stations.removeAll(removedStations);

        stations.forEach(Station::start);
        removedStations.forEach(Station::stop);
    }

    private Set<Station> readFromS3() {
        LOG.info("searching for stations in the S3 bucket: " + bucketName);

        Set<Station> stations = new HashSet<>();
        ObjectListing objectList = s3Client.listObjects(bucketName);
        boolean isTruncated = false;
        String lastName = "";
        do {
            isTruncated = objectList.isTruncated();

            for (S3ObjectSummary summary : objectList.getObjectSummaries()) {
                String name = summary.getKey().split("/")[0];
                if (!name.equals(lastName)) {
                    long lastModified = summary.getLastModified().toInstant().getEpochSecond();
                    stations.add(stationFactory.create(name, summary.getBucketName(), lastModified));
                    lastName = name;
                }
            }

            if (isTruncated) {
                objectList = s3Client.listNextBatchOfObjects(objectList);
            }
        } while (isTruncated);

        return stations;
    }

    public void onShutdown(@Observes ShutdownEvent shutdownEvent) {
        stations.forEach(Station::stop);
    }

    @Override
    public Collection<Station> getStations() {
        return Collections.unmodifiableSet(stations);
    }

    @Override
    public Optional<Station> getStation(String name) {
        return stations.stream().filter(s -> name.equals(s.getName())).findFirst();
    }

    @Override
    public String toString() {
        return "S3Radio [bucketName=" + bucketName + ", stations=" + stations + "]";
    }
}
