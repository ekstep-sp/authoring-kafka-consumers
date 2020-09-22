package com.infosys.wingspan.authoringkafkaconsumers.utils.health;

import org.apache.http.StatusLine;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Requires bean createRestHighLevelClient of class org.elasticsearch.client.RestHighLevelClient
 * /health endpoint must be enabled
 * Can be configured using management.health.custom.elasticsearch.enabled property defaults to true if missing
 */
@Component
@ConditionalOnBean(name = "createRestHighLevelClient")
@ConditionalOnAvailableEndpoint(endpoint = HealthEndpoint.class)
@ConditionalOnProperty(prefix = "management.health.custom", name = "elasticsearch.enabled", havingValue = "true", matchIfMissing = true)
public class ElasticsearchHealthIndicator implements HealthIndicator {
	private static final String RED_STATUS = "red";
	private final RestClient client;
	private final JsonParser jsonParser;

	public ElasticsearchHealthIndicator(@Qualifier("createRestHighLevelClient") RestHighLevelClient restHighLevelClient) {
		this.client = restHighLevelClient.getLowLevelClient();
		this.jsonParser = JsonParserFactory.getJsonParser();
	}

	public void doHealthCheck(Health.Builder builder, String json) {
		Map<String, Object> response = this.jsonParser.parseMap(json);
		String status = (String) response.get("status");
		if (RED_STATUS.equals(status)) {
			// RED Status indicates temporarily out of service
			builder.outOfService().withDetail("health", RED_STATUS);
		} else {
			builder.up();
		}
	}

	@Override
	public Health health() {
		Health.Builder builder = Health.up();
		try {
			Response response;
			response = this.client.performRequest(new Request("GET", "/_cluster/health/"));
			StatusLine statusLine = response.getStatusLine();
			if (statusLine.getStatusCode() != 200) {
				builder.down().withDetail("reasonPhrase", statusLine.getReasonPhrase());
			} else {
				InputStream inputStream = response.getEntity().getContent();
				Throwable var5 = null;
				try {
					this.doHealthCheck(builder, StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8));
				} catch (Throwable var14) {
					var5 = var14;
					throw var14;
				} finally {
					if (inputStream != null) {
						if (var5 != null) {
							try {
								inputStream.close();
							} catch (Throwable var13) {
								var5.addSuppressed(var13);
							}
						} else {
							inputStream.close();
						}
					}

				}
			}
		} catch (IOException e) {
			builder.down().withDetail("error", e.getClass().getName());
		}
		return builder.build();
	}
}
