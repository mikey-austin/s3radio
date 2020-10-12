package net.jackiemclean;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.google.common.collect.Iterators;
import com.google.common.io.Files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class S3Station implements Station, Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(S3Station.class);

    private final AtomicBoolean isStarted;
    private final AtomicReference<TrackStream> currentStream;
    private final String name;
    private final String bucket;
    private final long lastModified;
    private final AmazonS3 s3Client;
    private final TrackStreamerFactory streamerFactory;
    private final TrackFactory trackFactory;

    private volatile boolean shutdown = false;
    private volatile Thread streamThread = null;

    public S3Station(String name, String bucket, long lastModified, AmazonS3 s3Client,
            TrackStreamerFactory streamerFactory, TrackFactory trackFactory) {
        this.name = name;
        this.bucket = bucket;
        this.lastModified = lastModified;
        this.s3Client = s3Client;
        this.streamerFactory = streamerFactory;
        this.trackFactory = trackFactory;
        this.isStarted = new AtomicBoolean(false);
        this.currentStream = new AtomicReference<>(null);
    }

    @Override
    public Iterator<Track> iterator() {
        return this.new TrackIterator();
    }

    @Override
    public void run() {
        while (!shutdown) {
            for (Track track : this) {
                TrackStream stream = streamerFactory.createStream(track);
                this.currentStream.set(stream);
                stream.play();
                if (shutdown) {
                    break;
                }
            }
        }
    }

    @Override
    public void start() {
        if (!isStarted.compareAndSet(false, true)) {
            return;
        }

        LOG.info("starting station: " + name);
        this.streamThread = new Thread(this);
        this.streamThread.start();
    }

    @Override
    public void stop() {
        LOG.info("stopping station: " + name);
        this.shutdown = true;
        try {
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
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        S3Station other = (S3Station) obj;
        if (bucket == null) {
            if (other.bucket != null)
                return false;
        } else if (!bucket.equals(other.bucket))
            return false;
        if (lastModified != other.lastModified)
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }

    private class TrackIterator implements Iterator<Track> {

        private ObjectListing currentListing;
        private Iterator<S3ObjectSummary> listingIterator;

        public TrackIterator() {
            this.currentListing = s3Client.listObjects(bucket, name);
            setNextIterator(currentListing);
        }

        private void setNextIterator(ObjectListing listing) {
            this.listingIterator = Iterators.filter(listing.getObjectSummaries().iterator(),
                    os -> os.getKey().endsWith(".ogg"));
        }

        @Override
        public boolean hasNext() {
            return listingIterator.hasNext() || currentListing.isTruncated();
        }

        @Override
        public Track next() {
            if (listingIterator.hasNext()) {
                return createTrack(listingIterator.next());
            } else if (currentListing.isTruncated()) {
                currentListing = s3Client.listNextBatchOfObjects(currentListing);
                setNextIterator(currentListing);
                if (listingIterator.hasNext()) {
                    return createTrack(listingIterator.next());
                }
            }
            return null;
        }

        private Track createTrack(S3ObjectSummary summary) {
            S3Object obj = s3Client.getObject(bucket, summary.getKey());
            String name = Files.getNameWithoutExtension(summary.getKey());
            return trackFactory.create(name, S3Station.this, obj.getObjectContent(), summary.getSize());
        }
    }

    @Override
    public String toString() {
        return "S3Station [bucket=" + bucket + ", isStarted=" + isStarted + ", lastModified=" + lastModified + ", name="
                + name + "]";
    }
}
