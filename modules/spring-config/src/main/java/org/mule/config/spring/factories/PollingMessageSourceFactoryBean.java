/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.factories;

import org.mule.api.config.ConfigurationException;
import org.mule.api.endpoint.EndpointFactory;
import org.mule.api.processor.MessageProcessor;
import org.mule.config.i18n.MessageFactory;
import org.mule.endpoint.URIBuilder;
import org.mule.transport.AbstractConnector;
import org.mule.transport.polling.MessageProcessorPollingMessageReceiver;

public class PollingMessageSourceFactoryBean extends InboundEndpointFactoryBean
{

    protected MessageProcessor messageProcessor;
    protected Long frequency;

    @Override
    public Object doGetObject() throws Exception
    {
        uriBuilder = new URIBuilder("polling://" + hashCode(), muleContext);

        properties.put(MessageProcessorPollingMessageReceiver.SOURCE_MESSAGE_PROCESSOR_PROPERTY_NAME, messageProcessor);
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

    public void setMessageProcessor(MessageProcessor messageProcessor)
    {
        this.messageProcessor = messageProcessor;
    }

    public void setFrequency(Long frequency)
    {
        this.frequency = frequency;
    }

}
