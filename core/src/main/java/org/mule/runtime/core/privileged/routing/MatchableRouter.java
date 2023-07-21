/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.privileged.routing;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.privileged.processor.Router;

/**
 * A router which route will be conditionally executed.
 * 
 * @since 4.0
 */
public interface MatchableRouter extends MatchableMessageProcessor, Router {

  /**
   * Adds a new message processor to the list of routes
   *
   * @param processor new destination message processor
   * @throws MuleException
   */
  void addRoute(Processor processor) throws MuleException;

}


