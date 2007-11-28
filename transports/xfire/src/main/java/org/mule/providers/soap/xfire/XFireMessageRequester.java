/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap.xfire;

import org.mule.config.MuleProperties;
import org.mule.impl.MuleMessage;
import org.mule.providers.AbstractMessageRequester;
import org.mule.providers.soap.xfire.transport.MuleUniversalTransport;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOImmutableEndpoint;

import java.util.Iterator;
import java.util.Properties;

import org.codehaus.xfire.XFire;
import org.codehaus.xfire.client.Client;
import org.codehaus.xfire.service.OperationInfo;
import org.codehaus.xfire.service.Service;


/**
 * Used for making Soap client requests to remote services.
 */
public class XFireMessageRequester extends AbstractMessageRequester
{
    // Since the MessageDispatcher is guaranteed to serve a single thread,
    // the Dispatcher can own the xfire Client as an instance variable
    protected Client client = null;
    protected final XFireConnector connector;

    public XFireMessageRequester(UMOImmutableEndpoint endpoint)
    {
        super(endpoint);
        this.connector = (XFireConnector) endpoint.getConnector();
    }

    protected void doConnect() throws Exception
    {
        if (client == null)
        {
            client = connector.doClientConnect(endpoint, this);
        }
    }

    protected void doDisconnect() throws Exception
    {
        client = null;
    }

    protected void doDispose()
    {
        // nothing to do
    }

    /**
     * Make a specific request to the underlying transport
     *
     * @param timeout the maximum time the operation should block before returning.
     *            The call should return immediately if there is data available. If
     *            no data becomes available before the timeout elapses, null will be
     *            returned
     * @return the result of the request wrapped in a UMOMessage object. Null will be
     *         returned if no data was avaialable
     * @throws Exception if the call to the underlying protocal cuases an exception
     */
    protected UMOMessage doRequest(long timeout) throws Exception
    {
        String serviceName = XFireMessageDispatcher.getServiceName(endpoint);

        XFire xfire = connector.getXfire();
        Service service = xfire.getServiceRegistry().getService(serviceName);

        Client client = new Client(new MuleUniversalTransport(), service, endpoint.getEndpointURI()
            .toString());
        client.setXFire(xfire);
        client.setTimeout((int)timeout);
        client.setEndpointUri(endpoint.getEndpointURI().toString());

        String method = (String)endpoint.getProperty(MuleProperties.MULE_METHOD_PROPERTY);
        OperationInfo op = service.getServiceInfo().getOperation(method);

        Properties params = endpoint.getEndpointURI().getUserParams();
        String args[] = new String[params.size()];
        int i = 0;
        for (Iterator iterator = params.values().iterator(); iterator.hasNext(); i++)
        {
            args[i] = iterator.next().toString();
        }

        Object[] response = client.invoke(op, args);

        if (response != null && response.length == 1)
        {
            return new MuleMessage(response[0]);
        }
        else
        {
            return new MuleMessage(response);
        }
    }

}