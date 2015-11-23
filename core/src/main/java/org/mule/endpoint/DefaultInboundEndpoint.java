/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.endpoint;

import org.mule.MessageExchangePattern;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.context.MuleContextAware;
import org.mule.api.endpoint.EndpointMessageProcessorChainFactory;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.exception.MessagingExceptionHandler;
import org.mule.api.exception.MessagingExceptionHandlerAcceptor;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.LifecycleException;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.retry.RetryPolicyTemplate;
import org.mule.api.transaction.TransactionConfig;
import org.mule.api.transport.Connector;
import org.mule.config.MuleManifest;
import org.mule.config.i18n.CoreMessages;
import org.mule.exception.ChoiceMessagingExceptionStrategy;
import org.mule.exception.RollbackMessagingExceptionStrategy;
import org.mule.processor.AbstractRedeliveryPolicy;
import org.mule.transport.ConnectException;

import java.beans.ExceptionListener;
import java.util.List;
import java.util.Map;

public class DefaultInboundEndpoint extends AbstractEndpoint implements InboundEndpoint
{
    private static final long serialVersionUID = -4752772777414636142L;
    private MessageProcessor listener;
    private FlowConstruct flowConstruct;
    private ExceptionListener exceptionListener;

    public DefaultInboundEndpoint(Connector connector,
                                  EndpointURI endpointUri,
                                  String name,
                                  Map properties,
                                  TransactionConfig transactionConfig,
                                  boolean deleteUnacceptedMessage,
                                  MessageExchangePattern messageExchangePattern,
                                  int responseTimeout,
                                  String initialState,
                                  String endpointEncoding,
                                  String endpointBuilderName,
                                  MuleContext muleContext,
                                  RetryPolicyTemplate retryPolicyTemplate,
                                  AbstractRedeliveryPolicy redeliveryPolicy,
                                  EndpointMessageProcessorChainFactory messageProcessorsFactory,
                                  List <MessageProcessor> messageProcessors,
                                  List <MessageProcessor> responseMessageProcessors,
                                  boolean disableTransportTransformer,
                                  String mimeType)
    {
        super(connector, endpointUri, name, properties, 
            transactionConfig, deleteUnacceptedMessage,
            messageExchangePattern, responseTimeout, initialState, endpointEncoding, 
            endpointBuilderName, muleContext, retryPolicyTemplate, redeliveryPolicy, messageProcessorsFactory,
            messageProcessors, responseMessageProcessors, disableTransportTransformer,
            mimeType);
    }

    public MuleMessage request(long timeout) throws Exception
    {
        if (getConnector() != null)
        {
            return getConnector().request(this, timeout);
        }
        else
        {
            // TODO Either remove because this should never happen or i18n the
            // message
            throw new IllegalStateException("The connector on the endpoint: " + toString()
                                            + " is null. Please contact " + MuleManifest.getDevListEmail());
        }
    }

    public void setListener(MessageProcessor listener)
    {
        this.listener = listener;
    }

    public void start() throws MuleException
    {
        try
        {
            if (getMessageProcessorChain(flowConstruct) instanceof Startable)
            {
                ((Startable) getMessageProcessorChain(flowConstruct)).start();
            }
            getConnector().registerListener(this, getMessageProcessorChain(flowConstruct), flowConstruct);
        }
        // Let connection exceptions bubble up to trigger the reconnection strategy.
        catch (ConnectException ce)
        {
            throw ce;
        }
        catch (Exception e)
        {
            throw new LifecycleException(CoreMessages.failedToStartInboundEndpoint(this), e, this);
        }
    }

    public void stop() throws MuleException
    {
        try
        {
            getConnector().unregisterListener(this, flowConstruct);
            if (getMessageProcessorChain(flowConstruct) instanceof Stoppable)
            {
                ((Stoppable) getMessageProcessorChain(flowConstruct)).stop();
            }
        }
        catch (Exception e)
        {
            throw new LifecycleException(CoreMessages.failedToStopInboundEndpoint(this), e, this);
        }
    }

    @Override
    public MessageProcessor createMessageProcessorChain(FlowConstruct flowContruct) throws MuleException
    {
        EndpointMessageProcessorChainFactory factory = getMessageProcessorsFactory();
        MessageProcessor processorChain = factory.createInboundMessageProcessorChain(this, flowConstruct,
            listener);
        if (processorChain instanceof MuleContextAware)
        {
            ((MuleContextAware) processorChain).setMuleContext(getMuleContext());
        }
        if (processorChain instanceof FlowConstructAware)
        {
            ((FlowConstructAware) processorChain).setFlowConstruct(flowContruct);
        }
        if (processorChain instanceof Initialisable)
        {
            ((Initialisable) processorChain).initialise();
        }
        return processorChain;
    }

    public void setFlowConstruct(FlowConstruct flowConstruct)
    {
        this.flowConstruct = flowConstruct;
    }

    public ExceptionListener getExceptionListener()
    {
        return exceptionListener;
    }

    public void setExceptionListener(ExceptionListener exceptionListener)
    {
        this.exceptionListener = exceptionListener;
    }

    @Override
    public void dispose()
    {
        super.dispose();
        this.flowConstruct = null;
        this.listener = null;
    }
    
    @Override
    public int hashCode()
    {
        // We need unique hashcode for each inbound endpoint instance because flowConstuct and listener are not
        // injected until after endpoint has been created and cached and the key used for caching is hashcode.
        // If we don't do this then endpoints which are configured identically but used with different
        // services get mixed up after deserialization of events
        return System.identityHashCode(this);
    }

    @Override
    public AbstractRedeliveryPolicy getRedeliveryPolicy()
    {
        /*TODO Next commit will fix this horrible thing:
         inbound endpoint should only be aware of a redelivery policy configured on it
         flowConstruct should be responsible of redelivery policy use */
        AbstractRedeliveryPolicy redeliveryPolicy = super.getRedeliveryPolicy();
        RollbackMessagingExceptionStrategy rollbackMessagingExceptionStrategy = null;
        if (flowConstruct != null && flowConstruct.getExceptionListener() != null)
        {
            MessagingExceptionHandler exceptionListener = flowConstruct.getExceptionListener();
            if (exceptionListener instanceof RollbackMessagingExceptionStrategy)
            {
                rollbackMessagingExceptionStrategy = (RollbackMessagingExceptionStrategy) exceptionListener;
            }
            else if (exceptionListener instanceof ChoiceMessagingExceptionStrategy)
            {
                ChoiceMessagingExceptionStrategy choiceMessagingExceptionStrategy = (ChoiceMessagingExceptionStrategy) exceptionListener;
                for (MessagingExceptionHandlerAcceptor messagingExceptionHandlerAcceptor : choiceMessagingExceptionStrategy.getExceptionListeners())
                {
                    if (messagingExceptionHandlerAcceptor instanceof RollbackMessagingExceptionStrategy && ((RollbackMessagingExceptionStrategy) messagingExceptionHandlerAcceptor).hasMaxRedeliveryAttempts())
                    {
                        rollbackMessagingExceptionStrategy = (RollbackMessagingExceptionStrategy) messagingExceptionHandlerAcceptor;
                        break;
                    }
                }
            }
        }
        if (rollbackMessagingExceptionStrategy != null && rollbackMessagingExceptionStrategy.hasMaxRedeliveryAttempts())
        {
            if (redeliveryPolicy == null)
            {
                redeliveryPolicy = createDefaultRedeliveryPolicy(rollbackMessagingExceptionStrategy.getMaxRedeliveryAttempts());
            }
            else
            {
                redeliveryPolicy.setMaxRedeliveryCount(rollbackMessagingExceptionStrategy.getMaxRedeliveryAttempts());
            }
        }
        return redeliveryPolicy;
    }

    @Override
    public AbstractRedeliveryPolicy createDefaultRedeliveryPolicy(int maxRedelivery)
    {
        return getConnector().createDefaultRedeliveryPolicy(maxRedelivery);
    }
    
    public FlowConstruct getFlowConstruct()
    {
        return flowConstruct;
    }
}
