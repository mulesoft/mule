/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.routing.correlation.CollectionCorrelatorCallback;
import org.mule.runtime.core.routing.correlation.EventCorrelatorCallback;

/**
 * <code>AbstractCorrelationAggregatingMessageProcessor</code> uses the CorrelationID and CorrelationGroupSize properties of the
 * {@link org.mule.runtime.core.api.MuleMessage} to manage message groups.
 */
public abstract class AbstractCorrelationAggregator extends AbstractAggregator {

  @Override
  protected EventCorrelatorCallback getCorrelatorCallback(MuleContext muleContext) {
    return new DelegateCorrelatorCallback(muleContext);
  }

  protected abstract MuleEvent aggregateEvents(EventGroup events) throws AggregationException;

  private class DelegateCorrelatorCallback extends CollectionCorrelatorCallback {

    public DelegateCorrelatorCallback(MuleContext muleContext) {
      super(muleContext, storePrefix);
    }

    @Override
    public MuleEvent aggregateEvents(EventGroup events) throws AggregationException {
      return AbstractCorrelationAggregator.this.aggregateEvents(events);
    }
  }

}
