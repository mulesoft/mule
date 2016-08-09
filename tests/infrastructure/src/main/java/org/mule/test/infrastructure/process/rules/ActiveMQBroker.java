/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.infrastructure.process.rules;

import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.TransportConnector;
import org.junit.rules.ExternalResource;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;

/**
 * A {@link TestRule} which starts an {@link BrokerService}.
 * <p>
 * It automatically allocates a dynamic port and exposes the selected port on a system property under a configurable key.
 *
 * @since 4.0
 */
public class ActiveMQBroker extends ExternalResource {

  protected final DynamicPort dynamicPort;

  private final String connectorUrl;
  private BrokerService broker;
  private TransportConnector transportConnector;

  /**
   * Creates a new instance
   *
   * @param amqBrokerPortName the name of the system property on which the port will be exposed
   */
  public ActiveMQBroker(String amqBrokerPortName) {
    dynamicPort = new DynamicPort(amqBrokerPortName);

    connectorUrl = "tcp://localhost:" + dynamicPort.getValue();
  }

  @Override
  public Statement apply(Statement base, Description description) {
    base = dynamicPort.apply(base, description);
    return super.apply(base, description);
  }

  public void start() {
    broker = new BrokerService();

    try {
      broker.setUseJmx(false);
      broker.setPersistent(false);
      transportConnector = broker.addConnector(connectorUrl);
      broker.start(true);
      broker.waitUntilStarted();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void stop() {
    try {
      broker.stop();
      broker.waitUntilStopped();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public String getConnectorUrl() {
    return connectorUrl;
  }

  public int getConnectionsCount() {
    return transportConnector.getConnections().size();
  }
}
