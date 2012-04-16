/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.polling;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.VoidMuleEvent;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.execution.ExecutionCallback;
import org.mule.api.execution.ExecutionTemplate;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transport.Connector;
import org.mule.config.i18n.CoreMessages;
import org.mule.transport.AbstractConnector;
import org.mule.transport.AbstractPollingMessageReceiver;
import org.mule.transport.NullPayload;
import org.mule.util.StringUtils;

import java.util.Map;

public class MessageProcessorPollingMessageReceiver extends AbstractPollingMessageReceiver
{
    public static final String SOURCE_MESSAGE_PROCESSOR_PROPERTY_NAME = MuleProperties.ENDPOINT_PROPERTY_PREFIX + "sourceMessageProcessor";

    protected MessageProcessor sourceMessageProcessor;

    public MessageProcessorPollingMessageReceiver(Connector connector,
                                                  FlowConstruct flowConstruct,
                                                  InboundEndpoint endpoint) throws CreateException
    {
        super(connector, flowConstruct, endpoint);
    }

    @Override
    protected void doInitialise() throws InitialisationException
    {
        super.doInitialise();

        sourceMessageProcessor = (MessageProcessor) endpoint.getProperty(SOURCE_MESSAGE_PROCESSOR_PROPERTY_NAME);

        if (sourceMessageProcessor instanceof OutboundEndpoint
            && !((OutboundEndpoint) sourceMessageProcessor).getExchangePattern().hasResponse())
        {
            // TODO DF: i18n
            throw new InitialisationException(CoreMessages.createStaticMessage(String.format(
                "The endpoint %s does not return responses and therefore can't be used for polling.",
                sourceMessageProcessor)), this);
        }

        Long tempPolling = (Long) endpoint.getProperties().get(AbstractConnector.PROPERTY_POLLING_FREQUENCY);
        if (tempPolling != null)
        {
            setFrequency(tempPolling);
        }
    }

    @Override
    public void poll() throws Exception
    {
        ExecutionTemplate<MuleEvent> executionTemplate = createExecutionTemplate();
        try
        {
            executionTemplate.execute(new ExecutionCallback<MuleEvent>()
            {
                @Override
                public MuleEvent process() throws Exception
                {
                    MuleMessage request = new DefaultMuleMessage(StringUtils.EMPTY, (Map<String, Object>) null,
                            connector.getMuleContext());
                    ImmutableEndpoint ep = endpoint;
                    if (sourceMessageProcessor instanceof ImmutableEndpoint)
                    {
                        ep = (ImmutableEndpoint) sourceMessageProcessor;
                    }

                    MuleEvent event = new DefaultMuleEvent(request, ep.getExchangePattern(), flowConstruct);

                    MuleEvent sourceEvent = sourceMessageProcessor.process(event);
                    if (isNewMessage(sourceEvent))
                    {
                        routeMessage(sourceEvent.getMessage());
                    } else
                    {
                        // TODO DF: i18n
                        logger.info(String.format("Polling of '%s' returned null, the flow will not be invoked.",
                                sourceMessageProcessor));
                    }
                    return null;
                }
            });
        }
        catch (MessagingException e)
        {
            //Already handled by TransactionTemplate
        }
        catch (Exception e)
        {
            connector.getMuleContext().getExceptionListener().handleException(e);
        }
    }

    @Override
    protected boolean pollOnPrimaryInstanceOnly()
    {
        return true;
    }
    
    // Only consider response for source message processor a new message if it is not
    // null and payload is not NullPayload
    protected boolean isNewMessage(MuleEvent sourceEvent)
    {
        if (sourceEvent != null && !VoidMuleEvent.getInstance().equals(sourceEvent) && sourceEvent.getMessage() != null)
        {
            MuleMessage message = sourceEvent.getMessage();
            if (message.getPayload().equals(NullPayload.getInstance()))
            {
                return false;
            }
            else
            {
                return true;
            }
        }
        return false;
    }
}
