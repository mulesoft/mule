/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.processor;

import org.mule.runtime.core.api.Event;

import java.util.Map;

/**
 * Implementations provide a means of resolving the parameters that the processor will receive, performing any required value
 * resolution.
 */
public interface ParametersResolverProcessor {

  /**
   * @param event the event entering the processor for whom parameters are to be resolved
   * @return the resolved parameterts, with the name of the parameter as the key of each entry.
   */
  Map<String, Object> resolveParameters(Event event);

}
