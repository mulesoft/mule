/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.exception.MessagingException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.construct.FlowConstruct;

/**
 * Defines a processing strategy for until successful router.
 */
public interface UntilSuccessfulProcessingStrategy {

  /**
   * @param event the message to be routed through the until-successful router.
   * @param flowConstruct
   * @return the return event from the until-successful execution.
   * @throws MessagingException exception thrown during until-successful execution.
   */
  Event route(final Event event, FlowConstruct flowConstruct) throws MuleException;

  /**
   * @param untilSuccessfulConfiguration until successful configuration.
   */
  void setUntilSuccessfulConfiguration(final UntilSuccessfulConfiguration untilSuccessfulConfiguration);

}
