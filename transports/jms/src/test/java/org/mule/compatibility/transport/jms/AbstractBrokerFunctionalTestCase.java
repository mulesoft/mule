/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.jms;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.test.infrastructure.process.rules.ActiveMQBroker;

import org.junit.Rule;

/**
 * Base test class for tests that require an Active MQ broker that is not embedded in the Mule application.
 */
public class AbstractBrokerFunctionalTestCase extends FunctionalTestCase {

  @Rule
  public ActiveMQBroker amqBroker = new ActiveMQBroker("port");

  @Override
  protected void doSetUpBeforeMuleContextCreation() throws Exception {
    amqBroker.start();
  }

  @Override
  protected void doTearDownAfterMuleContextDispose() throws Exception {
    amqBroker.stop();
  }

  protected int getConnectionsCount() {
    return amqBroker.getConnectionsCount();
  }

  public String getConnectorUrl() {
    return amqBroker.getConnectorUrl();
  }

}
