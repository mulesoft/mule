/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.policy;

import org.mule.runtime.core.api.Event;

/**
 * Functional interface to define the behaviour of the next-operation message processor in policies.
 * 
 * @since 4.0
 */
@FunctionalInterface
public interface NextOperation {

  /**
   * Executes the operation.
   *
   * @param event the input event
   * @return the result of processing the input event
   * @throws Exception exception thrown during processing.
   */
  Event execute(Event event) throws Exception;

}
