/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.routing;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.processor.Router;
import org.mule.runtime.core.api.processor.Processor;

public interface MatchableRouter extends MatchableMessageProcessor, Router {

  /**
   * Adds a new message processor to the list of routes
   *
   * @param processor new destination message processor
   * @throws MuleException
   */
  void addRoute(Processor processor) throws MuleException;

}


