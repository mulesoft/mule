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

import org.mule.api.endpoint.InboundEndpoint;

public class ListAssignedTasksAction extends AbstractListTasksAction
{
    private String user;
    
    @Override
    protected void appendType(StringBuffer uri, InboundEndpoint endpoint)
    {
        if (this.getUser() != null)
        {
            uri.append("&assignee=");
            uri.append(this.getUser());
        }
    }

    public String getUser()
    {
        return user;
    }

    public void setUser(String user)
    {
        this.user = user;
    }
}
