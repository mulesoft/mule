/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.processor;

import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.MuleEvent;

/**
 * Iterates over a list of {@link org.mule.runtime.core.api.processor.MessageProcessor}s executing them one by one using the
 * result of the first processor to invoke the second and so on. {@link org.mule.runtime.core.api.processor.ProcessorExecutor}
 * implementations aside from simply iterating over processors implement rules regarding if and when iteration should stop early
 * or even stop temporarily and be continued later.
 *
 * @since 3.7
 */
public interface ProcessorExecutor {

  /**
   * Executes a list of {@link org.mule.runtime.core.api.processor.MessageProcessor}s. Execution may or may not return the result
   * of executing all of the {@link org.mule.runtime.core.api.processor.MessageProcessor}'s or not depending on the
   * implementation. For example processing may be cut short in certain circumstance or be continued in another thread at a later
   * point in time.
   *
   * @return result of processing zero or more {@link org.mule.runtime.core.api.processor.MessageProcessor}'s synchronously.
   * @throws MessagingException exception thrown doing {@link org.mule.runtime.core.api.processor.MessageProcessor execution}, if
   *         any.
   */
  public MuleEvent execute() throws MessagingException;

}
