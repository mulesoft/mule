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

import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.module.activiti.ActivitiConnector;

public class PerformTaskOperationAction extends AbstractOutboundActivitiAction<PutMethod>
{
    private String taskId;

    private Operation operation;

    public String executeUsing(ActivitiConnector connector,
                                HttpClient client,
                                MuleMessage message,
                                OutboundEndpoint endpoint)
    {
        if (message.getPayload() instanceof Map)
        {
            Map values = (Map) message.getPayload();

            if (values.containsKey("taskId"))
            {
                this.setTaskId((String) values.get("taskId"));
            }
            if (values.containsKey("operation"))
            {
                this.setOperation((Operation) values.get("operation"));
            }
        }

        return super.executeUsing(connector, client, message, endpoint);
    }

    @Override
    protected void prepareMethod(PutMethod method, MuleMessage message) throws Exception
    {
        RequestEntity requestEntity = new StringRequestEntity("{}", "application/json", "UTF-8");
        method.setRequestEntity(requestEntity);
    }

    @Override
    protected URI resolveURI(OutboundEndpoint endpoint) throws URIException, NullPointerException
    {
        StringBuffer uri = new StringBuffer();
        uri.append("task/");

        uri.append(this.getTaskId());
        uri.append("/");
        uri.append(this.getOperation());

        return new URI(uri.toString(), false);
    }

    public PutMethod getMethod()
    {
        return new PutMethod();
    }

    public String getTaskId()
    {
        return taskId;
    }

    public void setTaskId(String taskId)
    {
        this.taskId = taskId;
    }

    public Operation getOperation()
    {
        return operation;
    }

    public void setOperation(Operation operation)
    {
        this.operation = operation;
    }
}
