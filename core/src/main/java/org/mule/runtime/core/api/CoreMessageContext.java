/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api;

import org.mule.runtime.core.api.context.notification.ProcessorsTrace;
import org.mule.runtime.core.config.DefaultMuleConfiguration;
import org.mule.runtime.core.management.stats.ProcessingTime;

/**
 * Extension of {@link MessageContext} with core specific methods.
 *
 * @since 4.0
 */
public interface CoreMessageContext extends MessageContext {

  /**
   * @returns information about the times spent processing the events for this context (so far).
   */
  ProcessingTime getProcessingTime();

  /**
   * Events have a list of message processor paths it went trough so that the execution path of an event can be reconstructed
   * after it has executed.
   * <p/>
   * This will only be enabled if {@link DefaultMuleConfiguration#isFlowTrace()} is {@code true}. If {@code false}, the list will
   * always be empty.
   * 
   * @return the message processors trace associated to this event.
   * 
   * @since 3.8.0
   */
  ProcessorsTrace getProcessorsTrace();
}
