/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.routing;

import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.transaction.TransactionConfig;

import java.util.List;

/**
 * <code>OutboundRouter</code> is used to control outbound routing behaviour for an event. One or more Outbound routers can be
 * associated with an <code>OutboundRouterCollection</code> and will be selected based on the filters set on the individual
 * Outbound Router.
 */
public interface OutboundRouter
    extends MatchableRouter, RouterStatisticsRecorder, Lifecycle, MuleContextAware {

  void setTransactionConfig(TransactionConfig transactionConfig);

  /**
   * Gets a list of MessageProcessor instances associated with this router
   * 
   * @return a list of MessageProcessor instances
   */
  List<Processor> getRoutes();

}
