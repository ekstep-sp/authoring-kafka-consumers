package com.infosys.wingspan.authoringkafkaconsumers.utils.health;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.neo4j.driver.v1.AccessMode;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.exceptions.SessionExpiredException;
import org.neo4j.driver.v1.summary.ResultSummary;
import org.neo4j.driver.v1.summary.ServerInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnEnabledEndpoint;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(value = Driver.class)
@ConditionalOnAvailableEndpoint(endpoint = HealthEndpoint.class)
public class Neo4jHealthIndicator implements HealthIndicator {
	private static final Log logger = LogFactory.getLog(Neo4jHealthIndicator.class);
	static final String CYPHER = "RETURN 1 AS result";
	static final String MESSAGE_HEALTH_CHECK_FAILED = "Neo4j health check failed";
	static final String MESSAGE_SESSION_EXPIRED = "Neo4j session has expired, retrying one single time to retrieve server health.";

	private final Driver driver;

	public Neo4jHealthIndicator(Driver driver) {
		this.driver = driver;
	}

	@Override
	public Health health() {
		try {
			try {
				this.runHealthCheckQuery();
			} catch (SessionExpiredException var4) {
				logger.warn(MESSAGE_SESSION_EXPIRED);
				this.runHealthCheckQuery();
			}

			return Health.up().build();
		} catch (Exception var5) {
			logger.error(MESSAGE_HEALTH_CHECK_FAILED);
			return Health.down().withDetail("error",var5.getClass().getName()).build();
		}

	}

	private Health buildStatusUp(ResultSummary resultSummary) {
		ServerInfo serverInfo = resultSummary.server();
		return Health.up().withDetail("server", serverInfo.version() + "@" + serverInfo.address()).build();
	}

	ResultSummary runHealthCheckQuery() {
		Session session = this.driver.session(AccessMode.WRITE);
		Throwable var2 = null;

		ResultSummary var4;
		try {
			ResultSummary resultSummary = session.run(CYPHER).consume();
			var4 = resultSummary;
		} catch (Throwable var13) {
			var2 = var13;
			throw var13;
		} finally {
			if (session != null) {
				if (var2 != null) {
					try {
						session.close();
					} catch (Throwable var12) {
						var2.addSuppressed(var12);
					}
				} else {
					session.close();
				}
			}

		}

		return var4;
	}
}
