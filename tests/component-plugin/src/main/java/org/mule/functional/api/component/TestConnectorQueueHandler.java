/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.api.component;


import static org.mule.functional.client.TestConnectorConfig.DEFAULT_CONFIG_ID;
import org.mule.functional.client.TestConnectorConfig;
import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.core.api.event.CoreEvent;


public class TestConnectorQueueHandler {

  private Registry registry;

  public TestConnectorQueueHandler(Registry registry) {
    this.registry = registry;
  }

  /**
   * Writes a event to a given queue waiting if necessary for space to become available
   *
   * @param queueName
   * @param event
   */
  public void write(String queueName, CoreEvent event) {
    TestConnectorConfig connectorConfig = (TestConnectorConfig) registry.lookupByName(DEFAULT_CONFIG_ID).get();
    connectorConfig.write(queueName, event);
  }

  /**
   * Reads an event from a given queue waiting if necessary until an element becomes available.
   *
   * @param queueName
   * @return the {@link CoreEvent} read from the queue
   */
  public CoreEvent read(String queueName) {
    TestConnectorConfig connectorConfig = (TestConnectorConfig) registry.lookupByName(DEFAULT_CONFIG_ID).get();
    return connectorConfig.take(queueName);
  }

  /**
   * Reads an event from a given queue waiting up to the specified wait time if necessary for an element to become available.
   * @param queueName
   * @param timeout
   * @return the {@link CoreEvent} read or null if timeout time is exceeded.
   */
  public CoreEvent read(String queueName, long timeout) {
    TestConnectorConfig connectorConfig = (TestConnectorConfig) registry.lookupByName(DEFAULT_CONFIG_ID).get();
    return connectorConfig.poll(queueName, timeout);
  }

}
