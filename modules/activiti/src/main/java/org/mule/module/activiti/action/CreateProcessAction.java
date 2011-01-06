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
import java.util.Map.Entry;

import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.OutboundEndpoint;

public class CreateProcessAction extends AbstractOutboundActivitiAction<PostMethod>
{

    @Override
    protected URI resolveURI(OutboundEndpoint endpoint) throws URIException, NullPointerException
    {
        return new URI("process-instance", false);
    }

    public PostMethod getMethod()
    {
        return new PostMethod();
    }

    @Override
    protected void prepareMethod(PostMethod method, MuleMessage message) throws Exception
    {
        Object payload = message.getPayload();
        String json = "";
        if (payload instanceof Map)
        {
            Map map = (Map) payload;
            StringBuffer buffer = new StringBuffer();
            buffer.append("{");
            for (Object entryObject : map.entrySet())
            {
                Entry entry = (Entry) entryObject;
                buffer.append(entry.getKey());
                buffer.append(" : \"");
                buffer.append(entry.getValue());
                buffer.append("\",");
            }
            buffer.append("}");
            json = buffer.toString();
        }
        else
        {
            String processDefinitionId = message.getPayloadAsString();
            json = "{" + "\"processDefinitionId\":\"" + processDefinitionId + "\"," + "}";
        }
        
        RequestEntity requestEntity = new StringRequestEntity(json, "application/json", "UTF-8");
        method.setRequestEntity(requestEntity);
    }
}
