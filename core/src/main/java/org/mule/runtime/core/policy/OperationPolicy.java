/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.policy;

import org.mule.runtime.core.api.Event;

public interface OperationPolicy {

  /**
   * Process the policy chain of processors. The provided {@code nextOperation} function has the behaviour to be executed by the
   * next-operation of the chain.
   *
   * @param operationEvent the event with the data to execute the operation
   * @return the result of processing the {@code event} through the policy chain.
   * @throws Exception
   */
  Event process(Event operationEvent) throws Exception;

}
