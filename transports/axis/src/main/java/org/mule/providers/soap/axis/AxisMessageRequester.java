/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap.axis;

import org.mule.config.MuleProperties;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.providers.AbstractMessageRequester;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.endpoint.UMOImmutableEndpoint;

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.xml.soap.SOAPEnvelope;

import org.apache.axis.AxisProperties;
import org.apache.axis.EngineConfiguration;
import org.apache.axis.Message;
import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.axis.configuration.FileProvider;

/**
 * <code>AxisMessageDispatcher</code> is used to make soap requests via the Axis
 * soap client.
 */
public class AxisMessageRequester extends AbstractMessageRequester
{

    protected EngineConfiguration clientConfig;
    protected AxisConnector connector;
    protected Service service;

    public AxisMessageRequester(UMOImmutableEndpoint endpoint)
    {
        super(endpoint);
        this.connector = (AxisConnector)endpoint.getConnector();
        AxisProperties.setProperty("axis.doAutoTypes", Boolean.toString(connector.isDoAutoTypes()));
    }

    protected void doConnect() throws Exception
    {
        if (service == null)
        {
            service = createService(endpoint);
        }
    }

    protected void doDisconnect() throws Exception
    {
        if (service != null)
        {
            service = null;
        }
    }

    protected void doDispose()
    {
        // template method
    }

    protected synchronized EngineConfiguration getClientConfig(UMOImmutableEndpoint endpoint)
    {
        if (clientConfig == null)
        {
            // Allow the client config to be set on the endpoint
            String config;
            config = (String)endpoint.getProperty(AxisConnector.AXIS_CLIENT_CONFIG_PROPERTY);

            if (config != null)
            {
                clientConfig = new FileProvider(config);
            }
            else
            {
                clientConfig = connector.getClientProvider();
            }
        }
        return clientConfig;
    }

    protected Service createService(UMOImmutableEndpoint endpoint) throws Exception
    {
        // Create a simple axis service without wsdl
        EngineConfiguration config = getClientConfig(endpoint);
        return new Service(config);
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
        Call call = new Call(service);
        String uri = endpoint.getEndpointURI().toString();
        call.setSOAPActionURI(uri);
        call.setTargetEndpointAddress(uri);

        Properties params = endpoint.getEndpointURI().getUserParams();
        String method = (String)params.remove(MuleProperties.MULE_METHOD_PROPERTY);
        call.setOperationName(method);

        String args[] = new String[params.size()];
        int i = 0;
        for (Iterator iterator = params.values().iterator(); iterator.hasNext(); i++)
        {
            args[i] = iterator.next().toString();
        }

        call.setOperationName(method);
        call.setProperty(MuleProperties.MULE_ENDPOINT_PROPERTY, endpoint);
        Object result = call.invoke(method, args);
        return AxisMessageDispatcher.createMessage(result, call);
    }

    public UMOMessage request(String endpoint, Object[] args) throws Exception
    {
        Call call = new Call(service);

        call.setSOAPActionURI(endpoint);
        call.setTargetEndpointAddress(endpoint);

        if (!endpoint.startsWith("axis:"))
        {
            endpoint = "axis:" + endpoint;
        }
        UMOEndpointURI ep = new MuleEndpointURI(endpoint);
        String method = (String)ep.getParams().remove(MuleProperties.MULE_METHOD_PROPERTY);
        call.setOperationName(method);

        call.setOperationName(method);
        Object result = call.invoke(method, args);
        return AxisMessageDispatcher.createMessage(result, call);
    }

    public UMOMessage request(String endpoint, SOAPEnvelope envelope) throws Exception
    {
        Call call = new Call(service);

        call.setSOAPActionURI(endpoint);
        call.setTargetEndpointAddress(endpoint);
        Object result = call.invoke(new Message(envelope));
        return AxisMessageDispatcher.createMessage(result, call);
    }

}
