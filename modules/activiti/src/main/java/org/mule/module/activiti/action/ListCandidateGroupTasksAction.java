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

public class ListCandidateGroupTasksAction extends AbstractListTasksAction
{
    private String candidateGroup;
    
    @Override
    protected void appendType(StringBuffer uri, InboundEndpoint endpoint)
    {
        if (this.getCandidateGroup() != null)
        {
            uri.append("&candidate-group=");
            uri.append(this.getCandidateGroup());
        }
    }

    public String getCandidateGroup()
    {
        return candidateGroup;
    }

    public void setCandidateGroup(String candidateGroup)
    {
        this.candidateGroup = candidateGroup;
    }
}
