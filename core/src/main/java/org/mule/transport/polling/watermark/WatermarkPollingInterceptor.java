/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.polling.watermark;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.config.ConfigurationException;
import org.mule.api.store.ObjectStoreException;
import org.mule.config.i18n.CoreMessages;
import org.mule.transport.polling.MessageProcessorPollingInterceptor;

/**
 * Implementation of {@link MessageProcessorPollingInterceptor} that retrieves the
 * current watermark value before the polling flow is executed and updates it when it
 * is finished.
 * 
 * @since 3.5.0
 */
public class WatermarkPollingInterceptor extends MessageProcessorPollingInterceptor
{

    protected final Watermark watermark;

    public WatermarkPollingInterceptor(Watermark watermark)
    {
        this.watermark = watermark;
    }

    /**
     * Watermark source preprocessing puts the watermark value into a flow variable
     */
    @Override
    public MuleEvent prepareSourceEvent(MuleEvent event) throws MuleException
    {
        this.watermark.putInto(event);
        return event;
    }

    /**
     * Watermark route preparation carries the value from the source event to the
     * flow event
     */
    @Override
    public MuleEvent prepareRouting(MuleEvent sourceEvent, MuleEvent event) throws ConfigurationException
    {
        if (!event.isSynchronous())
        {
            throw new ConfigurationException(CoreMessages.watermarkRequiresSynchronousProcessing());
        }

        String variableName = this.watermark.resolveVariable(event);
        event.setFlowVariable(variableName, sourceEvent.getFlowVariable(variableName));
        return event;
    }

    /**
     * Watermark post processing saves the flow variable to the object store
     */
    @Override
    public void postProcessRouting(MuleEvent event) throws ObjectStoreException
    {
        this.watermark.updateFrom(event);
    }

}
