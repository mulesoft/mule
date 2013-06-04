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
import org.mule.MessageExchangePattern;
import org.mule.VoidMuleEvent;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.construct.FlowConstruct;
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
import org.mule.transport.polling.watermark.builder.WatermarkConfiguration;
import org.mule.util.StringUtils;

import java.util.Map;

/**
 * Poll receiver. Is the one who does the polling action.
 */
public class MessageProcessorPollingMessageReceiver extends AbstractPollingMessageReceiver
{

    /**
     * The configured message source name. The poll contains a message source that is executed before executing the flow
     * construct
     */
    public static final String SOURCE_MESSAGE_PROCESSOR_PROPERTY_NAME = MuleProperties.ENDPOINT_PROPERTY_PREFIX + "sourceMessageProcessor";

    /**
     * The Watermark configuration name. The poll configuration admits watermark configuration in order to store a
     * watermark value in the object store and retrieve it every time the poll is executed.
     */
    public static final String WATERMARK_PROPERTY_NAME = MuleProperties.ENDPOINT_PROPERTY_PREFIX + "watermark";


    /**
     * The poll message source. This is the one who is going to get the Mule event to execute the flow.
     */
    protected MessageProcessor sourceMessageProcessor;


    public MessageProcessorPollingMessageReceiver(Connector connector,
                                                  FlowConstruct flowConstruct,
                                                  InboundEndpoint endpoint) throws CreateException
    {
        super(connector, flowConstruct, endpoint);
    }

    /**
     * Sets the source message processor and the activates the watermark configuration.
     *
     * @throws InitialisationException In case initialization fails.
     */
    @Override
    protected void doInitialise() throws InitialisationException
    {
        super.doInitialise();

        checkWatermark();

        sourceMessageProcessor = watermark().buildMessageSourceFrom(configuredMessageSource());
        watermark().registerPipelineNotificationListener(this.getFlowConstruct());

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

                    MuleEvent event = new DefaultMuleEvent(request, MessageExchangePattern.REQUEST_RESPONSE, flowConstruct);

                    MuleEvent sourceEvent = sourceMessageProcessor.process(event);
                    if (isNewMessage(sourceEvent))
                    {
                        routeMessage(sourceEvent.getMessage());
                    }
                    else
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

    /**
     * Only consider response for source message processor a new message if it is not
     * null and payload is not NullPayload
     */
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

    private void checkWatermark() throws InitialisationException
    {
        if ( watermark() == null ){
            throw new InitialisationException(CoreMessages.createStaticMessage(
                    "The watermark configuration must not be null"), this);
        }
    }

    private WatermarkConfiguration watermark()
    {
        return (WatermarkConfiguration) endpoint.getProperty(WATERMARK_PROPERTY_NAME);
    }

    private MessageProcessor configuredMessageSource() throws InitialisationException
    {
        MessageProcessor configuredMessageSource = (MessageProcessor) endpoint.getProperty(SOURCE_MESSAGE_PROCESSOR_PROPERTY_NAME);

        if (configuredMessageSource instanceof OutboundEndpoint
            && !((OutboundEndpoint) configuredMessageSource).getExchangePattern().hasResponse())
        {
            // TODO DF: i18n
            throw new InitialisationException(CoreMessages.createStaticMessage(String.format(
                    "The endpoint %s does not return responses and therefore can't be used for polling.",
                    sourceMessageProcessor)), this);
        }

        return configuredMessageSource;
    }


}
