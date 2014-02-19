/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.polling.watermark.selector;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.context.MuleContextAware;
import org.mule.api.expression.InvalidExpressionException;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.store.ObjectStore;
import org.mule.config.i18n.MessageFactory;
import org.mule.transport.polling.MessageProcessorPollingInterceptor;
import org.mule.transport.polling.watermark.Watermark;

import java.io.Serializable;

/**
 * Implementation of {@link Watermark} that relies on a {@link WatermarkSelector} to
 * update its values
 * 
 * @since 3.5.0
 */
public class SelectorWatermark extends Watermark implements Initialisable, MuleContextAware
{

    private final WatermarkSelectorBroker selectorBroker;
    private final String selectorExpression;

    private MuleContext muleContext;

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

    @Override
    public void initialise() throws InitialisationException
    {
        try
        {
            this.muleContext.getExpressionManager().validateExpression(this.selectorExpression);
        }
        catch (InvalidExpressionException e)
        {
            throw new InitialisationException(
                    MessageFactory.createStaticMessage(
                            String.format("selector-expression requires a valid MEL expression. '%s' was found instead", this.selectorExpression))
                    , e, this);
        }
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

    @Override
    public void setMuleContext(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }
}
