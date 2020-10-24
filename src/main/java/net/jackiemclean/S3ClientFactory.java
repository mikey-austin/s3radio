package net.jackiemclean;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

@Dependent
public class S3ClientFactory {

    private final String accessKey;
    private final String secretKey;
    private final String endpoint;
    private final String region;
    private final boolean withTls;

    @Inject
    public S3ClientFactory(
            @ConfigProperty(name = "s3.accessKey") String accessKey,
            @ConfigProperty(name = "s3.secretKey") String secretKey,
            @ConfigProperty(name = "s3.endpoint") String endpoint,
            @ConfigProperty(name = "s3.region") String region,
            @ConfigProperty(name = "s3.tls", defaultValue = "true") boolean withTls) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.endpoint = endpoint;
        this.region = region;
        this.withTls = withTls;
    }

    @Produces
    @Singleton
    public AmazonS3 buildS3Client() {
        AWSCredentialsProvider doCred =
                new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey));
        AmazonS3ClientBuilder builder =
                AmazonS3ClientBuilder.standard()
                        .withCredentials(doCred)
                        .withEndpointConfiguration(new EndpointConfiguration(endpoint, region));
        if (!withTls) {
            builder.withClientConfiguration(new ClientConfiguration().withProtocol(Protocol.HTTP));
        }
        return builder.build();
    }
}
