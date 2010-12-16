/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.activiti;

import org.apache.commons.httpclient.HttpClient;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.module.activiti.action.OutboundActivitiAction;
import org.mule.transport.AbstractMessageDispatcher;

public class ActivitiMessageDispatcher extends AbstractMessageDispatcher
{
    private OutboundActivitiAction<?> action;

    private HttpClient client;

    public ActivitiMessageDispatcher(OutboundEndpoint endpoint)
    {
        super(endpoint);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doInitialise() throws InitialisationException
    {
        super.doInitialise();
        this.action = (OutboundActivitiAction<?>) this.getEndpoint().getProperty("action");
    }

    /**
     * {@inheritDoc}
     */
    public void doDispatch(MuleEvent event) throws Exception
    {
        this.doSend(event);
    }

    /**
     * {@inheritDoc}
     */
    public MuleMessage doSend(MuleEvent event) throws Exception
    {
        Object result = this.action.executeUsing(this.getConnector(), this.client, event.getMessage(),
            this.getEndpoint());
        MuleMessage message = new DefaultMuleMessage(result, event.getMuleContext());
        return message;
    }

    @Override
    public ActivitiConnector getConnector()
    {
        return (ActivitiConnector) super.getConnector();
    }

    /**
     * {@inheritDoc}
     */
    public void doConnect() throws Exception
    {
        super.doConnect();
        if (this.client == null)
        {
            this.client = this.getConnector().getClient();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void doDisconnect() throws Exception
    {
        // DO NOTHING
    }

    /**
     * {@inheritDoc}
     */
    public void doDispose()
    {
        // DO NOTHING
    }
}
