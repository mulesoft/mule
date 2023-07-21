/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.execution;

import org.mule.runtime.core.api.source.MessageSource;
import org.mule.sdk.api.runtime.source.DistributedTraceContextManager;

/**
 * In charge of processing messages through mule.
 *
 * @since 4.3.0
 */
public interface MessageProcessingManager {

  /**
   * Process a message by routing it through a flow.
   *
   * @param messageProcessTemplate contains template methods that will be executed by each phase in specific parts of the phase so
   *                               the {@link MessageSource} can apply custom logic during message processing. The message will
   *                               participate only on those phases were the template defines the required template methods
   * @param messageProcessContext  defines the context of execution of the message
   */
  void processMessage(FlowProcessTemplate messageProcessTemplate, MessageProcessContext messageProcessContext);


  /**
   * Process a message by routing it through a flow.
   *
   * @param messageProcessTemplate               contains template methods that will be executed by each phase in specific parts
   *                                             of the phase so the {@link MessageSource} can apply custom logic during message
   *                                             processing. The message will participate only on those phases were the template
   *                                             defines the required template methods
   * @param messageProcessContext                defines the context of execution of the message
   * @param sourceDistributedTraceContextManager the {@link DistributedTraceContextManager} from the source of the flow.
   */
  default void processMessage(FlowProcessTemplate messageProcessTemplate, MessageProcessContext messageProcessContext,
                              DistributedTraceContextManager sourceDistributedTraceContextManager) {
    processMessage(messageProcessTemplate, messageProcessContext);
  }

}
