package com.infosys.wingspan.authoringkafkaconsumers.utils.health;

import org.apache.kafka.clients.admin.*;
import org.neo4j.driver.v1.Driver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnEnabledEndpoint;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

@Component
@ConditionalOnBean(value = KafkaAdmin.class)
@ConditionalOnAvailableEndpoint(endpoint = HealthEndpoint.class)
public class KafkaHealthIndicator implements HealthIndicator {

	final AdminClient kafkaAdminClient;

	public KafkaHealthIndicator(AdminClient kafkaAdminClient) {
		this.kafkaAdminClient = kafkaAdminClient;
	}

	@Override
	public Health health() {
		try {
			final DescribeClusterOptions describeClusterOptions = new DescribeClusterOptions().timeoutMs(1000);
			final DescribeClusterResult describeCluster = kafkaAdminClient.describeCluster(describeClusterOptions);
			describeCluster.clusterId().get();
			return Health.up().build();
		} catch (Exception e) {
			return Health.down()
					.withDetail("error", e.getClass().getName())
					.build();
		}
	}

}
