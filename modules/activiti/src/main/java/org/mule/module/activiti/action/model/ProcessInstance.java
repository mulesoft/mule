/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.activiti.action.model;

import java.io.Serializable;
import java.util.List;

public class ProcessInstance implements Serializable
{
    private static final long serialVersionUID = -8055270881231310548L;

    private String id;

    private String processDefinitionId;

    private List<String> activityNames;

    private boolean ended;

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public String getProcessDefinitionId()
    {
        return processDefinitionId;
    }

    public void setProcessDefinitionId(String processDefinitionId)
    {
        this.processDefinitionId = processDefinitionId;
    }

    public List<String> getActivityNames()
    {
        return activityNames;
    }

    public void setActivityNames(List<String> activityNames)
    {
        this.activityNames = activityNames;
    }

    public boolean isEnded()
    {
        return ended;
    }

    public void setEnded(boolean ended)
    {
        this.ended = ended;
    }
}