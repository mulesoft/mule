/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.execution;

import org.mule.runtime.core.api.source.MessageSource;

/**
 * In charge of processing messages through mule.
 */
public interface MessageProcessingManager {

  /**
   * Process a message by routing it through a flow.
   *
   * @param messageProcessTemplate contains template methods that will be executed by each phase in specific parts of the phase so
   *        the {@link MessageSource} can apply custom logic during message processing. The
   *        message will participate only on those phases were the template defines the required template methods
   * @param messageProcessContext defines the context of execution of the message
   */
  void processMessage(FlowProcessTemplate messageProcessTemplate, MessageProcessContext messageProcessContext);

}
