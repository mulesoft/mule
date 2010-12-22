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
import org.mule.api.MuleMessage;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transport.Connector;
import org.mule.module.activiti.action.InboundActivitiAction;
import org.mule.transport.AbstractPollingMessageReceiver;

public class ActivitiMessageReceiver extends AbstractPollingMessageReceiver
{
    private InboundActivitiAction<?> action;

    private HttpClient client;

    public ActivitiMessageReceiver(Connector connector, FlowConstruct flowConstruct, InboundEndpoint endpoint)
        throws CreateException
    {
        super(connector, flowConstruct, endpoint);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doConnect() throws Exception
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
    @Override
    protected void doDisconnect() throws Exception
    {
        super.doDisconnect();
        this.client = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doInitialise() throws InitialisationException
    {
        super.doInitialise();
        this.action = (InboundActivitiAction<?>) this.getEndpoint().getProperty("action");
        this.setFrequency(this.action.getPollingFrequency());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void poll() throws Exception
    {
        String result = this.action.executeUsing(this.getConnector(), this.client, this.getEndpoint());
        MuleMessage message = new DefaultMuleMessage(result, this.getConnector().getMuleContext());
        this.routeMessage(message);
    }

    @Override
    public ActivitiConnector getConnector()
    {
        return (ActivitiConnector) super.getConnector();
    }
}
