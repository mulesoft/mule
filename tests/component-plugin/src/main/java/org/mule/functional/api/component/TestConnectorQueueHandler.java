/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.api.component;


import static org.mule.functional.client.TestConnectorConfig.DEFAULT_CONFIG_ID;
import org.mule.functional.client.TestConnectorConfig;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.BaseEvent;


public class TestConnectorQueueHandler {

  private MuleContext muleContext;

  public TestConnectorQueueHandler(MuleContext muleContext) {
    this.muleContext = muleContext;
  }

  /**
   * Writes a even to to a given queue waiting if necessary for space to become available
   * 
   * @param queueName
   * @param event
   */
  public void write(String queueName, BaseEvent event) {
    TestConnectorConfig connectorConfig = muleContext.getRegistry().lookupObject(DEFAULT_CONFIG_ID);
    connectorConfig.write(queueName, event);
  }

  /**
   * Reads an event from a given queue waiting if necessary until an element becomes available.
   *
   * @param queueName
   * @return the {@link BaseEvent} read from the queue
   */
  public BaseEvent read(String queueName) {
    TestConnectorConfig connectorConfig = muleContext.getRegistry().lookupObject(DEFAULT_CONFIG_ID);
    return connectorConfig.take(queueName);
  }

  /**
   * Reads an event from a given queue waiting up to the specified wait time if necessary for an element to become available.
   * @param queueName
   * @param timeout
   * @return the {@link BaseEvent} read or null if timeout time is exceeded.
   */
  public BaseEvent read(String queueName, long timeout) {
    TestConnectorConfig connectorConfig = muleContext.getRegistry().lookupObject(DEFAULT_CONFIG_ID);
    return connectorConfig.poll(queueName, timeout);
  }

}
