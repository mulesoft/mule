/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.store.ListableObjectStore;

/**
 * Configuration required for UntilSuccessful router processing strategy.
 */
public interface UntilSuccessfulConfiguration {

  /**
   * @return an ObjectStore to store until successful internal data. Always returns a not null value.
   */
  ListableObjectStore<Event> getObjectStore();

  /**
   * @return Expression to determine if the message was processed successfully or not. Always returns a not null value.
   */
  String getFailureExpression();

  /**
   * @return the route to which the message should be routed to. Always returns a not null value.
   */
  Processor getRoute();

  /**
   * @return the MuleContext within the until-successful router was defined. Always returns a not null value.
   */
  MuleContext getMuleContext();

  /**
   * @return the FlowConstruct within the until-successful router was defined. Always returns a not null value.
   */
  FlowConstruct getFlowConstruct();

  /**
   * @return the expression that will define the returned payload after the until successful route execution.
   */
  String getAckExpression();

  /**
   * @return the number of milliseconds between retries. Default value is 60000.
   */
  long getMillisBetweenRetries();

  /**
   * @return the number of retries to process the route when failing. Default value is 5.
   */
  int getMaxRetries();

  /**
   * @return the route to which the message must be sent if the processing fails.
   */
  Processor getDlqMP();

  /**
   * @return the until sucessful router instance.
   */
  Processor getRouter();
}
