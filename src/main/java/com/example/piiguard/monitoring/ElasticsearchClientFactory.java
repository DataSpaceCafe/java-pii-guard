package com.example.piiguard.monitoring;

import java.nio.charset.StandardCharsets;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;

/**
 * Factory for Elasticsearch REST clients.
 */
public final class ElasticsearchClientFactory {
  private ElasticsearchClientFactory() {
  }

  /**
   * Creates a RestClient with optional basic auth.
   *
   * @param url Elasticsearch URL
   * @param username basic auth username (optional)
   * @param password basic auth password (optional)
   * @return RestClient instance
   */
  public static RestClient create(String url, String username, String password) {
    var host = HttpHost.create(url);
    var builder = RestClient.builder(host);

    if (username != null && !username.isBlank()) {
      var credentialsProvider = new BasicCredentialsProvider();
      credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
      builder.setHttpClientConfigCallback(httpClientBuilder ->
          httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
      Header[] defaultHeaders = new Header[]{
          new BasicHeader("Content-Type", "application/x-ndjson"),
          new BasicHeader("Accept", "application/json; charset=" + StandardCharsets.UTF_8)
      };
      builder.setDefaultHeaders(defaultHeaders);
    }

    return builder.build();
  }
}
