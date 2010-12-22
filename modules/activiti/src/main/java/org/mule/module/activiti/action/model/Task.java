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

public class Task implements Serializable
{

    private static final long serialVersionUID = 404948635324847864L;

    private String id;

    private String name;

    private String description;

    private int priority;

    private String assignee;

    private int executionId;

    private String formResourceKey;

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public int getPriority()
    {
        return priority;
    }

    public void setPriority(int priority)
    {
        this.priority = priority;
    }

    public String getAssignee()
    {
        return assignee;
    }

    public void setAssignee(String assignee)
    {
        this.assignee = assignee;
    }

    public int getExecutionId()
    {
        return executionId;
    }

    public void setExecutionId(int executionId)
    {
        this.executionId = executionId;
    }

    public String getFormResourceKey()
    {
        return formResourceKey;
    }

    public void setFormResourceKey(String formResourceKey)
    {
        this.formResourceKey = formResourceKey;
    }
}