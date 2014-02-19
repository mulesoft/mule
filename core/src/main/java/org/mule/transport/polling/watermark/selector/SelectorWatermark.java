/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.polling.watermark.selector;

import org.mule.api.MuleEvent;
import org.mule.api.store.ObjectStore;
import org.mule.transport.polling.MessageProcessorPollingInterceptor;
import org.mule.transport.polling.watermark.Watermark;

import java.io.Serializable;

/**
 * Implementation of {@link Watermark} that relies on a {@link WatermarkSelector} to
 * update its values
 * 
 * @since 3.5.0
 */
public class SelectorWatermark extends Watermark
{

    private final WatermarkSelectorBroker selectorBroker;
    private final String selectorExpression;

    public SelectorWatermark(ObjectStore<Serializable> objectStore,
                             String variable,
                             String defaultExpression,
                             WatermarkSelectorBroker selectorBroker,
                             String selectorExpression)
    {
        super(objectStore, variable, defaultExpression);
        this.selectorBroker = selectorBroker;
        this.selectorExpression = selectorExpression;
    }

    /**
     * Returns the {@link #selectorBroker} value and resets it so that its reusable. Notice
     * that the selectorBroker is reusable without risk of concurrency issues because
     * watermark only works on synchronous flows
     */
    @Override
    protected Object getUpdatedValue(MuleEvent event)
    {
        // interceptor is responsible for returning the selected value
        return null;
    }

    /**
     * {@inheritDoc}
     * @return a new {@link SelectorWatermarkPollingInterceptor}
     */
    @Override
    public MessageProcessorPollingInterceptor interceptor()
    {
        return new SelectorWatermarkPollingInterceptor(this, this.selectorBroker.newSelector(), this.selectorExpression);
    }

}
