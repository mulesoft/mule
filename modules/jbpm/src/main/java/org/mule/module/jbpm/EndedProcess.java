/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.jbpm;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.jbpm.api.Execution;
import org.jbpm.api.ProcessInstance;

/**
 * Placeholder used to get status for a process in a uniform way, even if it has ended.
 */
public class EndedProcess implements ProcessInstance
{
    private String id;

    public EndedProcess(String id)
    {
        this.id = id;
    }
    
    public String getId()
    {
        return id;
    }

    public boolean isEnded()
    {
        return true;
    }

    public Set<String> findActiveActivityNames()
    {
        return null;
    }

    public Execution findActiveExecutionIn(String activityName)
    {
        return null;
    }

    public Execution getExecution(String name)
    {
        return null;
    }

    public Collection<? extends Execution> getExecutions()
    {
        return null;
    }

    public Map<String, Execution> getExecutionsMap()
    {
        return null;
    }

    public boolean getIsProcessInstance()
    {
        return true;
    }

    public String getKey()
    {
        return null;
    }

    public String getName()
    {
        return null;
    }

    public Execution getParent()
    {
        return null;
    }

    public int getPriority()
    {
        return 0;
    }

    public String getProcessDefinitionId()
    {
        return null;
    }

    public Execution getProcessInstance()
    {
        return null;
    }

    public Execution getSubProcessInstance()
    {
        return null;
    }
    
    public String getState()
    {
        return ProcessInstance.STATE_ENDED;
    }

    public boolean hasExecution(String executionName)
    {
        return false;
    }

    public boolean isActive(String activityName)
    {
        return false;
    }

    public boolean isSuspended()
    {
        return false;
    }
}


