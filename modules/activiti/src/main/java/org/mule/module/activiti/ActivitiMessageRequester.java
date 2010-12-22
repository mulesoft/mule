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
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.module.activiti.action.InboundActivitiAction;
import org.mule.transport.AbstractMessageRequester;

public class ActivitiMessageRequester extends AbstractMessageRequester
{
    private InboundActivitiAction<?> action;
    
    private HttpClient client;
    
    public ActivitiMessageRequester(InboundEndpoint endpoint)
    {
        super(endpoint);
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
    protected MuleMessage doRequest(long timeout) throws Exception
    {
        Object result = this.action.executeUsing(this.getConnector(), this.client, this.getEndpoint());
        MuleMessage message = new DefaultMuleMessage(result, this.getConnector().getMuleContext());
        return message;
    }
    
    /** 
     * {@inheritDoc} 
     */
    @Override
    protected void doInitialise() throws InitialisationException
    {
        super.doInitialise();
        this.action = (InboundActivitiAction<?>) this.getEndpoint().getProperty("action");
    }
    
    @Override
    public ActivitiConnector getConnector()
    {
        return (ActivitiConnector) super.getConnector();
    }
}
