package net.jackiemclean;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Iterators;
import com.google.common.io.Files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class S3Station implements Station, Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(S3Station.class);

    private final AtomicBoolean isStarted;
    private final AtomicBoolean isMounted;
    private final AtomicReference<TrackStream> currentStream;
    private final String name;
    private final String bucket;
    private final long lastModified;
    private final AmazonS3 s3Client;
    private final TrackStreamFactory streamerFactory;
    private final TrackFactory trackFactory;
    private final Track standbyTrack;

    private volatile TrackIterator currentIterator = null;
    private volatile boolean standby = true;
    private volatile boolean shutdown = false;
    private volatile Thread streamThread = null;

    public S3Station(
            String name,
            String bucket,
            long lastModified,
            AmazonS3 s3Client,
            TrackStreamFactory streamerFactory,
            TrackFactory trackFactory,
            File standbyFile) {
        this.name = name;
        this.bucket = bucket;
        this.lastModified = lastModified;
        this.s3Client = s3Client;
        this.streamerFactory = streamerFactory;
        this.trackFactory = trackFactory;
        this.isStarted = new AtomicBoolean(false);
        this.currentStream = new AtomicReference<>(null);
        this.isMounted = new AtomicBoolean(false);
        this.standbyTrack = trackFactory.create("standby", this, standbyFile);
    }

    @Override
    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @Override
    @JsonProperty("trackStream")
    public Optional<TrackStream> getTrackStream() {
        return Optional.ofNullable(currentStream.get());
    }

    @Override
    @JsonProperty("streamUrl")
    public Optional<String> getStreamUrl() {
        return getTrackStream().map(TrackStream::getTrackStreamUri);
    }

    @Override
    public void run() {
        try {
            TrackStream stream = streamerFactory.createStream(this);
            stream.start();
            this.currentStream.set(stream);

            while (!shutdown) {
                Track track = nextTrack();
                stream.play(track);
                if (shutdown) {
                    break;
                }
            }

            stream.stop();
        } catch (Exception e) {
            LOG.error("exception during stream operation: {}", name, e);
        }
    }

    public TrackIterator iterator() {
        if (currentIterator != null && currentIterator.hasNext()) {
            return currentIterator;
        }
        return currentIterator = this.new TrackIterator();
    }

    private Track nextTrack() {
        return iterator().next();
    }

    @Override
    public void start() {
        if (!isStarted.compareAndSet(false, true)) {
            return;
        }

        LOG.info("starting station: " + name);
        try {
            this.streamThread = new Thread(this);
            this.streamThread.start();
            Thread.sleep(2_000);
        } catch (InterruptedException e) {
        }
    }

    @Override
    public void stop() {
        LOG.info("stopping station: " + name);
        this.shutdown = true;
        try {
            this.streamThread.interrupt();
            this.streamThread.join();
        } catch (Exception e) {
            LOG.error("error shutting down station: " + name, e);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((bucket == null) ? 0 : bucket.hashCode());
        result = prime * result + (int) (lastModified ^ (lastModified >>> 32));
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        S3Station other = (S3Station) obj;
        if (bucket == null) {
            if (other.bucket != null) return false;
        } else if (!bucket.equals(other.bucket)) return false;
        if (lastModified != other.lastModified) return false;
        if (name == null) {
            if (other.name != null) return false;
        } else if (!name.equals(other.name)) return false;
        return true;
    }

    private class TrackIterator implements Iterator<Track> {

        private ObjectListing currentListing;
        private Iterator<S3ObjectSummary> listingIterator;
        private int numEntries = 0;

        public TrackIterator() {
            this.currentListing = s3Client.listObjects(bucket, name);
            setNextIterator(currentListing);
        }

        private void setNextIterator(ObjectListing listing) {
            List<S3ObjectSummary> summaries = listing.getObjectSummaries();
            this.numEntries = summaries.size();
            this.listingIterator =
                    Iterators.filter(
                            summaries.iterator(),
                            os -> os.getKey().endsWith(".ogg") || os.getKey().endsWith(".mp3"));
        }

        @Override
        public boolean hasNext() {
            return listingIterator.hasNext() || currentListing.isTruncated();
        }

        @Override
        public Track next() {
            Track nextTrack = null;
            if (listingIterator.hasNext()) {
                S3ObjectSummary nextListing = listingIterator.next();
                nextTrack = standby ? standbyTrack : createTrack(nextListing);
            } else if (currentListing.isTruncated()) {
                currentListing = s3Client.listNextBatchOfObjects(currentListing);
                setNextIterator(currentListing);
                if (listingIterator.hasNext()) {
                    S3ObjectSummary nextListing = listingIterator.next();
                    nextTrack = standby ? standbyTrack : createTrack(nextListing);
                }
            }
            return nextTrack;
        }

        private Track createTrack(S3ObjectSummary summary) {
            S3Object obj = s3Client.getObject(bucket, summary.getKey());
            String name = Files.getNameWithoutExtension(summary.getKey());
            return trackFactory.create(
                    name, S3Station.this, obj.getObjectContent(), summary.getSize());
        }
    }

    @Override
    public void standbyOn() {
        mount();
        if (!standby) {
            LOG.info("{} entering standby", name);
        }
        standby = true;
    }

    @Override
    public void standbyOff() {
        mount();
        if (standby) {
            LOG.info("{} leaving standby", name);
        }
        standby = false;
    }

    @Override
    public String toString() {
        return "S3Station [bucket="
                + bucket
                + ", isStarted="
                + isStarted
                + ", lastModified="
                + lastModified
                + ", name="
                + name
                + "]";
    }

    @Override
    public boolean isMounted() {
        return isMounted.get();
    }

    @Override
    public void mount() {
        isMounted.set(true);
    }

    @Override
    public void unmount() {
        LOG.warn("unmounting {}", this);
        isMounted.set(false);
    }
}
