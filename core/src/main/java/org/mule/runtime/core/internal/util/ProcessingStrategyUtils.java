/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util;

import static org.mule.runtime.core.api.transaction.TransactionCoordination.isTransactionActive;

import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.Pipeline;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategyFactory;
import org.mule.runtime.core.processor.strategy.BlockingProcessingStrategyFactory;
import org.mule.runtime.core.processor.strategy.TransactionAwareWorkQueueProcessingStrategyFactory;

public class ProcessingStrategyUtils {

  /**
   * Determines if the {@link FlowConstruct} based on implementation and processing strategy configured is synchronous.
   *
   * @param flowConstruct the flow construct.
   * @return true if sync processing is in use/
   */
  public static boolean isSynchronousProcessing(FlowConstruct flowConstruct) {
    return (flowConstruct instanceof Pipeline && ((Pipeline) flowConstruct).isSynchronous()) || isTransactionActive();
  }

}
