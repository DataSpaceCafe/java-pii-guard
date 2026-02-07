package com.example.piiguard.util;

import org.apache.beam.sdk.io.aws2.options.S3ClientBuilderFactory;
import org.apache.beam.sdk.io.aws2.options.S3Options;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;

/**
 * Custom S3 client builder enabling path-style access for MinIO.
 */
public class CustomS3ClientBuilderFactory implements S3ClientBuilderFactory {
  /**
   * Creates a customized S3 client builder.
   *
   * @param options S3 options
   * @return builder instance
   */
  @Override
  public S3ClientBuilder createBuilder(S3Options options) {
    var builder = software.amazon.awssdk.services.s3.S3Client.builder();
    if (options.getAwsRegion() != null) {
      builder.region(options.getAwsRegion());
    }
    if (options.getEndpoint() != null) {
      builder.endpointOverride(options.getEndpoint());
    }
    if (options.getAwsCredentialsProvider() != null) {
      builder.credentialsProvider(options.getAwsCredentialsProvider());
    }
    var pathStyle = Boolean.parseBoolean(System.getProperty("s3.path.style.access",
        System.getenv().getOrDefault("S3_PATH_STYLE_ACCESS", "false")));
    var configuration = S3Configuration.builder().pathStyleAccessEnabled(pathStyle).build();
    builder.serviceConfiguration(configuration);
    return builder;
  }
}
