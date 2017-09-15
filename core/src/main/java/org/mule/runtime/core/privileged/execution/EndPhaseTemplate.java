/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.execution;

/**
 * Phase were the {@link MessageProcessTemplate} is notified that the message processing has ended
 */
public interface EndPhaseTemplate extends MessageProcessTemplate {

  /**
   * template method call when the message processing ends
   */
  void messageProcessingEnded();

}
