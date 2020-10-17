package net.jackiemclean;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.amazonaws.services.s3.AmazonS3;

@ApplicationScoped
public class S3StationFactory {

    private final AmazonS3 s3Client;
    private final TrackStreamFactory streamerFactory;
    private final TrackFactory trackFactory;

    @Inject
    public S3StationFactory(AmazonS3 s3Client, TrackStreamFactory streamerFactory, TrackFactory trackFactory) {
        this.s3Client = s3Client;
        this.streamerFactory = streamerFactory;
        this.trackFactory = trackFactory;
    }

    public S3Station create(String name, String bucket, long lastModified) {
        return new S3Station(name, bucket, lastModified, s3Client, streamerFactory, trackFactory);
    }
}
