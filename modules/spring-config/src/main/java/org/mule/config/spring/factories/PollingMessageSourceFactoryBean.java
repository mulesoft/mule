/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.factories;

import org.mule.api.config.ConfigurationException;
import org.mule.api.endpoint.EndpointFactory;
import org.mule.api.processor.MessageProcessor;
import org.mule.config.i18n.MessageFactory;
import org.mule.endpoint.URIBuilder;
import org.mule.transport.AbstractConnector;
import org.mule.transport.polling.MessageProcessorPollingMessageReceiver;
import org.mule.transport.polling.watermark.builder.DefaultWatermarkConfiguration;
import org.mule.transport.polling.watermark.builder.NullWatermarkConfiguration;
import org.mule.transport.polling.watermark.builder.WatermarkConfiguration;

public class PollingMessageSourceFactoryBean extends InboundEndpointFactoryBean
{
    protected WatermarkConfiguration watermark;
    protected MessageProcessor messageProcessor;
    protected Long frequency;

    @Override
    public Object doGetObject() throws Exception
    {
        uriBuilder = new URIBuilder("polling://" + hashCode(), muleContext);

        properties.put(MessageProcessorPollingMessageReceiver.SOURCE_MESSAGE_PROCESSOR_PROPERTY_NAME, messageProcessor);
        properties.put(MessageProcessorPollingMessageReceiver.WATERMARK_PROPERTY_NAME, getWatermarkProcessorBuilder());
        properties.put(AbstractConnector.PROPERTY_POLLING_FREQUENCY, frequency);

        EndpointFactory ef = muleContext.getEndpointFactory();
        if (ef != null)
        {
            return ef.getInboundEndpoint(this);
        }
        else
        {
            throw new ConfigurationException(
                MessageFactory.createStaticMessage("EndpointFactory not found in Registry"));
        }
    }


    private WatermarkConfiguration getWatermarkProcessorBuilder()
    {
        return watermark == null ? new NullWatermarkConfiguration() : watermark;
    }

    public void setMessageProcessor(MessageProcessor messageProcessor)
    {
        this.messageProcessor = messageProcessor;
    }

    public void setFrequency(Long frequency)
    {
        this.frequency = frequency;
    }

    public void setWatermark(DefaultWatermarkConfiguration watermark)
    {
        this.watermark = watermark;
    }
}
