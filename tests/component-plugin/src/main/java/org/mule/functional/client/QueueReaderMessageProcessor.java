/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.client;

import static org.mule.functional.client.TestConnectorConfig.DEFAULT_CONFIG_ID;
import static org.mule.runtime.api.util.Preconditions.checkArgument;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.event.BaseEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.util.StringUtils;

/**
 * Reads {@link BaseEvent} from a test connector's queue.
 */
public class QueueReaderMessageProcessor implements Processor {

  private final Registry registry;
  private final String queueName;
  private final Long timeout;

  /**
   * Creates a queue reader
   *
   * @param registry application's mule context. Not null.
   * @param queueName name of the queue to use. Non empty
   * @param timeout number of milliseconds to wait for an available event. Non negative. Null means no timeout required.
   */
  public QueueReaderMessageProcessor(Registry registry, String queueName, Long timeout) {
    checkArgument(registry != null, "Registry cannot be null");
    checkArgument(!StringUtils.isEmpty(queueName), "Queue name cannot be empty");
    if (timeout != null) {
      checkArgument(timeout >= 0L, "Timeout cannot be negative");
    }

    this.registry = registry;
    this.queueName = queueName;
    this.timeout = timeout;
  }

  @Override
  public BaseEvent process(BaseEvent event) throws MuleException {
    TestConnectorConfig connectorConfig = registry.<TestConnectorConfig>lookupByName(DEFAULT_CONFIG_ID).get();

    if (timeout == null) {
      return connectorConfig.take(queueName);
    } else {
      return connectorConfig.poll(queueName, timeout);
    }
  }
}
