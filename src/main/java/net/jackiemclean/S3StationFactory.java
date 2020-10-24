package net.jackiemclean;

import com.amazonaws.services.s3.AmazonS3;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.File;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class S3StationFactory {

    private final AmazonS3 s3Client;
    private final TrackStreamFactory streamerFactory;
    private final TrackFactory trackFactory;
    private final File standbyFileOgg;
    private final File standbyFileMp3;

    @Inject
    public S3StationFactory(
            AmazonS3 s3Client,
            TrackStreamFactory streamerFactory,
            TrackFactory trackFactory,
            @ConfigProperty(name = "standbyFile.ogg") String standbyFileOgg,
            @ConfigProperty(name = "standbyFile.mp3") String standbyFileMp3) {
        this.s3Client = s3Client;
        this.streamerFactory = streamerFactory;
        this.trackFactory = trackFactory;
        this.standbyFileOgg = new File(standbyFileOgg);
        this.standbyFileMp3 = new File(standbyFileMp3);
    }

    public S3Station create(String name, String bucket, long lastModified) {
        File standbyFile = name.endsWith("ogg") ? standbyFileOgg : standbyFileMp3;
        return new S3Station(
                name, bucket, lastModified, s3Client, streamerFactory, trackFactory, standbyFile);
    }
}
