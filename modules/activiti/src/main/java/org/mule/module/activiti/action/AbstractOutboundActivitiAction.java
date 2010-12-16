/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.activiti.action;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleRuntimeException;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.module.activiti.ActivitiConnector;
import org.mule.module.activiti.i18n.ActivitiMessages;

public abstract class AbstractOutboundActivitiAction<V extends HttpMethod> implements OutboundActivitiAction<V>
{

    private String name;
    
    protected abstract URI resolveURI(OutboundEndpoint endpoint) throws URIException, NullPointerException;

    protected abstract void prepareMethod(V method, MuleMessage message) throws Exception;

    public String executeUsing(ActivitiConnector connector, HttpClient client, MuleMessage message, OutboundEndpoint endpoint)
    {
        V method = null;
        try
        {
            method = this.getMethod();
            connector.prepareMethod(method, client);
            URI methodURI = new URI(new URI(connector.getActivitiServerURL(), false), this.resolveURI(endpoint));
            method.setURI(methodURI);
            this.prepareMethod(method, message);
            client.executeMethod(method);
            return method.getResponseBodyAsString();
        }
        catch (Exception e)
        {
            throw new MuleRuntimeException(ActivitiMessages.failToExecuteOutboundAction(this), e);
        }
        finally
        {
            method.releaseConnection();
        }
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }
}
