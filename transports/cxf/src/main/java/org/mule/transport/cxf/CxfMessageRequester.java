/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.cxf;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.transport.AbstractMessageRequester;

import java.util.Iterator;
import java.util.Properties;

import org.apache.cxf.endpoint.ClientImpl;

/**
 * <code>AxisMessageDispatcher</code> is used to make soap requests via the Axis
 * soap client.
 */
public class CxfMessageRequester extends AbstractMessageRequester
{

    protected CxfConnector connector;
    private ClientWrapper wrapper;
    
    public CxfMessageRequester(InboundEndpoint endpoint)
    {
        super(endpoint);
        this.connector = (CxfConnector)endpoint.getConnector();
    }

    protected void doConnect() throws Exception
    {
        wrapper = new ClientWrapper(connector.getCxfBus(), endpoint);
    }

    protected void doDisconnect() throws Exception
    {
    }

    protected void doDispose()
    {
        // template method
    }


    /**
     * Make a specific request to the underlying transport
     * 
     * @param timeout the maximum time the operation should block before returning.
     *            The call should return immediately if there is data available. If
     *            no data becomes available before the timeout elapses, null will be
     *            returned
     * @return the result of the request wrapped in a MuleMessage object. Null will be
     *         returned if no data was avaialable
     * @throws Exception if the call to the underlying protocal cuases an exception
     */
    protected MuleMessage doRequest(long timeout) throws Exception
    {
        ((ClientImpl)wrapper.getClient()).setSynchronousTimeout((int)timeout);

        String method = (String)endpoint.getProperty(MuleProperties.MULE_METHOD_PROPERTY);

        if (method == null) 
        {
            method = (String)endpoint.getProperty(CxfConstants.OPERATION);
        }
        
        Properties params = endpoint.getEndpointURI().getUserParams();
        Object args[] = new Object[params.size()];
        int i = 0;
        for (Iterator<Object> iterator = params.values().iterator(); iterator.hasNext(); i++)
        {
            args[i] = iterator.next().toString();
        }

        Object[] response = wrapper.getClient().invoke(method, args);

        if (response != null && response.length == 1)
        {
            return new DefaultMuleMessage(response[0]);
        }
        else
        {
            return new DefaultMuleMessage(response);
        }
    }

    
}
