/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
