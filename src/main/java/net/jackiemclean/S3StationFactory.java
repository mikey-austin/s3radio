package net.jackiemclean;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.amazonaws.services.s3.AmazonS3;

@ApplicationScoped
public class S3StationFactory {

    private final AmazonS3 s3Client;
    private final TrackStreamerFactory streamerFactory;
    private final TrackFactory trackFactory;

    @Inject
    public S3StationFactory(AmazonS3 s3Client, TrackStreamerFactory streamerFactory, TrackFactory trackFactory) {
        this.s3Client = s3Client;
        this.streamerFactory = streamerFactory;
        this.trackFactory = trackFactory;
    }

    public Station create(String name, String bucket, long lastModified) {
        return new S3Station(name, bucket, lastModified, s3Client, streamerFactory, trackFactory);
    }
}
