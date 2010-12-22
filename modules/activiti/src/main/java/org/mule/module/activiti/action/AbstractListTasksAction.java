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

import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.mule.api.endpoint.InboundEndpoint;

public abstract class AbstractListTasksAction extends AbstractInboundActivitiAction<GetMethod>
{

    private Long start;
    
    private Long size;
    
    @Override
    protected void prepareMethod(GetMethod method)
    {
        // DO NOTHING
    }

    @Override
    protected URI resolveURI(InboundEndpoint endpoint) throws URIException, NullPointerException
    {
        StringBuffer uri = new StringBuffer();
        uri.append("tasks?");
        this.appendType(uri, endpoint);

        if (this.getStart() != null) {
            uri.append("&start=");
            uri.append(this.getStart());
        }
        
        if (this.getSize() != null) {
            uri.append("&size=");
            uri.append(this.getSize());
        }

        return new URI(uri.toString(), false);
    }

    protected abstract void appendType(StringBuffer uri, InboundEndpoint endpoint);

    public GetMethod getMethod()
    {
        return new GetMethod();
    }

    public Long getStart()
    {
        return start;
    }

    public void setStart(Long start)
    {
        this.start = start;
    }

    public Long getSize()
    {
        return size;
    }

    public void setSize(Long size)
    {
        this.size = size;
    }
}
