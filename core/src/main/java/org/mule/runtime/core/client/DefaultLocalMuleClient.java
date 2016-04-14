/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.client;

import static org.mule.api.client.SimpleOptionsBuilder.newOptions;
import static org.mule.api.config.MuleProperties.OBJECT_CONNECTOR_MESSAGE_PROCESSOR_LOCATOR;
import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.VoidMuleEvent;
import org.mule.api.DefaultMuleException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleRuntimeException;
import org.mule.api.client.LocalMuleClient;
import org.mule.api.client.OperationOptions;
import org.mule.api.connector.ConnectorOperationLocator;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.exception.MessagingExceptionHandler;
import org.mule.api.lifecycle.LifecycleState;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorChain;
import org.mule.api.routing.MessageInfoMapping;
import org.mule.config.i18n.CoreMessages;
import org.mule.exception.DefaultMessagingExceptionStrategy;
import org.mule.management.stats.FlowConstructStatistics;
import org.mule.api.message.NullPayload;

import java.util.Map;

public class DefaultLocalMuleClient implements LocalMuleClient
{
    protected final MuleContext muleContext;
    private ConnectorOperationLocator connectorOperatorLocator;

    public DefaultLocalMuleClient(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

    private ConnectorOperationLocator getConnectorMessageProcessLocator()
    {
        if (connectorOperatorLocator == null)
        {
            this.connectorOperatorLocator = muleContext.getRegistry().get(OBJECT_CONNECTOR_MESSAGE_PROCESSOR_LOCATOR);
            if (this.connectorOperatorLocator == null)
            {
                throw new MuleRuntimeException(CoreMessages.createStaticMessage("Could not find required %s in the registry under key %s", ConnectorOperationLocator.class.getName(), OBJECT_CONNECTOR_MESSAGE_PROCESSOR_LOCATOR));
            }
        }
        return connectorOperatorLocator;
    }

    @Override
    public void dispatch(String url, Object payload, Map<String, Object> messageProperties)
        throws MuleException
    {
        dispatch(url, new DefaultMuleMessage(payload, messageProperties, muleContext));
    }

    @Override
    public MuleMessage send(String url, Object payload, Map<String, Object> messageProperties)
        throws MuleException
    {
        return send(url, new DefaultMuleMessage(payload, messageProperties, muleContext));
    }

    @Override
    public MuleMessage send(String url, MuleMessage message) throws MuleException
    {
        final MessageProcessor connectorMessageProcessor = getConnectorMessageProcessLocator().locateConnectorOperation(url, newOptions().build(), MessageExchangePattern.REQUEST_RESPONSE);
        if (connectorMessageProcessor != null)
        {
            return returnMessage(connectorMessageProcessor.process(createRequestResponseMuleEvent(message)));
        }
        throw createUnsupportedUrlException(url);
    }

    private MuleException createUnsupportedUrlException(String url)
    {
        return new DefaultMuleException("No installed connector supports url " + url);
    }

    @Override
    public MuleMessage send(String url, MuleMessage message, OperationOptions operationOptions) throws MuleException
    {
        final MessageProcessor connectorMessageProcessor = getConnectorMessageProcessLocator().locateConnectorOperation(url, operationOptions, MessageExchangePattern.REQUEST_RESPONSE);
        if (connectorMessageProcessor != null)
        {
            return returnMessage(connectorMessageProcessor.process(createRequestResponseMuleEvent(message)));
        }
        throw createUnsupportedUrlException(url);
    }

    @Override
    public MuleMessage send(String url, Object payload, Map<String, Object> messageProperties, long timeout)
        throws MuleException
    {
        return send(url, new DefaultMuleMessage(payload, messageProperties, muleContext), timeout);

    }

    @Override
    public MuleMessage send(String url, MuleMessage message, long timeout) throws MuleException
    {
        return send(url, message, newOptions().responseTimeout(timeout).build());
    }

    @Override
    public void dispatch(String url, MuleMessage message) throws MuleException
    {
        final MessageProcessor connectorMessageProcessor = getConnectorMessageProcessLocator().locateConnectorOperation(url, newOptions().build(), MessageExchangePattern.ONE_WAY);
        if (connectorMessageProcessor != null)
        {
            connectorMessageProcessor.process(createRequestResponseMuleEvent(message));
        }
        createUnsupportedUrlException(url);
    }

    @Override
    public void dispatch(String url, MuleMessage message, OperationOptions operationOptions) throws MuleException
    {
        final MessageProcessor connectorMessageProcessor = getConnectorMessageProcessLocator().locateConnectorOperation(url, operationOptions, MessageExchangePattern.ONE_WAY);
        if (connectorMessageProcessor != null)
        {
            connectorMessageProcessor.process(createRequestResponseMuleEvent(message));
        }
        else
        {
            dispatch(url, message);
        }
    }

    @Override
    public MuleMessage request(String url, long timeout) throws MuleException
    {
        final OperationOptions operationOptions = newOptions().responseTimeout(timeout).build();
        //final OperationOptions operationOptions = newOptions().responseTimeout(timeout).build();
        final MessageProcessor connectorMessageProcessor = getConnectorMessageProcessLocator().locateConnectorOperation(url, operationOptions, MessageExchangePattern.ONE_WAY);
        if (connectorMessageProcessor != null)
        {
            final MuleEvent event = connectorMessageProcessor.process(createOneWayMuleEvent(new DefaultMuleMessage(NullPayload.getInstance(), muleContext)));

            return event == null || event instanceof VoidMuleEvent ? null : event.getMessage();
        }
        throw createUnsupportedUrlException(url);
    }

    protected MuleEvent createRequestResponseMuleEvent(MuleMessage message)
            throws MuleException
    {
        return new DefaultMuleEvent(message, MessageExchangePattern.REQUEST_RESPONSE, new MuleClientFlowConstruct(
                muleContext));
    }

    protected MuleEvent createOneWayMuleEvent(MuleMessage message)
            throws MuleException
    {
        return new DefaultMuleEvent(message, MessageExchangePattern.ONE_WAY, new MuleClientFlowConstruct(
                muleContext));
    }

    protected MuleMessage returnMessage(MuleEvent event)
    {
        if (event != null && !VoidMuleEvent.getInstance().equals(event))
        {
            return event.getMessage();
        }
        else
        {
            return null;
        }
    }

    /**
     * Placeholder class which makes the default exception handler available.
     */
    static public class MuleClientFlowConstruct implements FlowConstruct
    {
        MuleContext muleContext;

        public MuleClientFlowConstruct(MuleContext muleContext)
        {
            this.muleContext = muleContext;
        }

        @Override
        public String getName()
        {
            return "MuleClient";
        }

        @Override
        public MessagingExceptionHandler getExceptionListener()
        {
            return new DefaultMessagingExceptionStrategy(muleContext);
        }

        @Override
        public LifecycleState getLifecycleState()
        {
            return null;
        }

        @Override
        public FlowConstructStatistics getStatistics()
        {
            return null;
        }

        @Override
        public MuleContext getMuleContext()
        {
            return muleContext;
        }

        @Override
        public MessageInfoMapping getMessageInfoMapping()
        {
            return null;
        }

        public MessageProcessorChain getMessageProcessorChain()
        {
            return null;
        }
    }
}
