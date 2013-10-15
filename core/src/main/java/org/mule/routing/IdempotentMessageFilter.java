/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.routing.RoutingException;
import org.mule.api.store.ObjectStore;
import org.mule.config.i18n.CoreMessages;
import org.mule.processor.AbstractFilteringMessageProcessor;
import org.mule.util.store.InMemoryObjectStore;

import java.text.MessageFormat;

/**
 * <code>IdempotentMessageFilter</code> ensures that only unique messages are passed on. It does this by
 * checking the unique ID of the incoming message. Note that the underlying endpoint must support unique
 * message IDs for this to work, otherwise a <code>UniqueIdNotSupportedException</code> is thrown.<br>
 * <p>
 * <b>EIP Reference:</b> <a href="http://www.eaipatterns.com/IdempotentReceiver.html">http://www.eaipatterns.com/IdempotentReceiver.html</a>
 */
public class IdempotentMessageFilter extends AbstractFilteringMessageProcessor implements FlowConstructAware, Initialisable
{
    protected volatile ObjectStore<String> store;
    protected volatile String assignedComponentName;
    protected FlowConstruct flowConstruct;

    protected String idExpression = MessageFormat.format("{0}message:id{1}",
        ExpressionManager.DEFAULT_EXPRESSION_PREFIX, ExpressionManager.DEFAULT_EXPRESSION_POSTFIX);

    protected String valueExpression = MessageFormat.format("{0}message:id{1}",
        ExpressionManager.DEFAULT_EXPRESSION_PREFIX, ExpressionManager.DEFAULT_EXPRESSION_POSTFIX);

    public IdempotentMessageFilter()
    {
        super();
    }

    public void initialise() throws InitialisationException
    {
        if (store == null)
        {
            this.store = this.createMessageIdStore();
        }
    }

    protected ObjectStore<String> createMessageIdStore() throws InitialisationException
    {
        InMemoryObjectStore<String> s = new InMemoryObjectStore<String>();
        s.setName(assignedComponentName);
        s.setMaxEntries(-1);
        s.setEntryTTL(60 * 5 * 1000);
        s.setExpirationInterval(6000);
        s.initialise();
        return s;
    }

    @Override
    protected MuleEvent processNext(MuleEvent event) throws MuleException
    {
        String id = this.getIdForEvent(event);
        String value = this.getValueForEvent(event);
        try
        {
            store.store(id, value);
            return super.processNext(event);
        }
        catch (Exception e)
        {
            throw new RoutingException(CoreMessages.failedToWriteMessageToStore(id, assignedComponentName),
                event, this, e);
        }
    }

    protected String getValueForEvent(MuleEvent event) throws MessagingException
    {
        return event.getMuleContext().getExpressionManager().parse(valueExpression, event.getMessage(), true);
    }

    protected String getIdForEvent(MuleEvent event) throws MessagingException
    {
        return event.getMuleContext().getExpressionManager().parse(idExpression, event.getMessage(), true);
    }

    public String getIdExpression()
    {
        return idExpression;
    }

    public void setIdExpression(String idExpression)
    {
        this.idExpression = idExpression;
    }

    public ObjectStore<String> getStore()
    {
        return store;
    }

    public void setStore(ObjectStore<String> store)
    {
        this.store = store;
    }

    @Override
    protected boolean accept(MuleEvent event)
    {
        return event != null && acceptMessageForFlowConstruct(event) && isNewMessage(event);
    }

    protected boolean acceptMessageForFlowConstruct(MuleEvent event)
    {
        if (flowConstruct.getName().equals(event.getFlowConstruct().getName()))
        {
            return true;
        }
        else
        {
            logger.error("This IdempotentMessageFilter was configured on the service: "
                         + assignedComponentName + " but has received an event for service: "
                         + flowConstruct.getName() + ". Please check your config to make sure each service"
                         + "has its own instance of IdempotentMessageFilter.");
            return false;
        }
    }

    protected boolean isNewMessage(MuleEvent event)
    {
        try
        {
            String id = this.getIdForEvent(event);
            if (store == null)
            {
                synchronized (this)
                {
                    initialise();
                }
            }
            return !store.contains(id);
        }
        catch (MuleException e)
        {
            logger.error("Exception attempting to determine idempotency of incoming message for "
                         + event.getFlowConstruct().getName() + " from the endpoint "
                         + event.getEndpoint().getEndpointURI().getUri(), e);
            return false;
        }
    }

    public void setFlowConstruct(FlowConstruct flowConstruct)
    {
        this.flowConstruct = flowConstruct;
    }

    public String getValueExpression()
    {
        return valueExpression;
    }

    public void setValueExpression(String valueExpression)
    {
        this.valueExpression = valueExpression;
    }

}
