/*
 * $Id: IdempotentMessageProcessor.java 17825 2010-07-02 12:57:50Z dfeist $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.processor;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.RoutingException;
import org.mule.api.store.ObjectStore;
import org.mule.config.i18n.CoreMessages;
import org.mule.util.store.InMemoryObjectStore;

import java.text.MessageFormat;

/**
 * <code>IdempotentReceiver</code> ensures that only unique messages are received by a
 * service. It does this by checking the unique ID of the incoming message. Note that
 * the underlying endpoint must support unique message IDs for this to work, otherwise a
 * <code>UniqueIdNotSupportedException</code> is thrown.<br>
 */
public class IdempotentMessageProcessor implements MessageProcessor
{
    protected volatile ObjectStore<String> store;
    protected volatile String assignedComponentName;

    protected String idExpression = MessageFormat.format("{0}message:id{1}",
                                                         ExpressionManager.DEFAULT_EXPRESSION_PREFIX,
                                                         ExpressionManager.DEFAULT_EXPRESSION_POSTFIX);

    protected void initialize(MuleEvent event) throws RoutingException
    {
        if (assignedComponentName == null)
        {
            this.assignedComponentName = event.getFlowConstruct().getName();
        }
        if (store == null)
        {
            try
            {
                this.store = this.createMessageIdStore();
            }
            catch (InitialisationException e)
            {
                throw new RoutingException(event.getMessage(), event.getEndpoint(), e);
            }
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

    protected boolean isMatch(MuleEvent event, String id) throws MessagingException
    {
        {
            if (store == null || assignedComponentName == null)
            {
                // we need to load this on the first request as we need the service
                // name
                synchronized (this)
                {
                    this.initialize(event);
                }
            }

            try
            {
                return !store.contains(id);
            }
            catch (Exception ex)
            {
                throw new RoutingException(event.getMessage(), event.getEndpoint(), ex);
            }
        }
    }

    /**
     *
     * @param event MuleEvent to be processed
     * @return null if this message has already been processed, or the input event if it has not.
     * @throws MessagingException
     */
    public MuleEvent process(MuleEvent event) throws MessagingException
    {
        String id = this.getIdForEvent(event);
        if (!isMatch(event, id))
            return null;

        String eventComponentName = event.getFlowConstruct().getName();
        if (!assignedComponentName.equals(eventComponentName))
        {
            IllegalArgumentException iex = new IllegalArgumentException(
                "This receiver is assigned to service: " + assignedComponentName
                                + " but has received an event for service: " + eventComponentName
                                + ". Please check your config to make sure each service"
                                + "has its own instance of IdempotentReceiver.");
            throw new RoutingException(event.getMessage(), event.getEndpoint(), iex);
        }

        try
        {
            store.store(id, id);
            return event;
        }
        catch (Exception e)
        {
            throw new RoutingException(CoreMessages.failedToWriteMessageToStore(id, assignedComponentName),
                event.getMessage(), event.getEndpoint(), e);
        }
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
}
