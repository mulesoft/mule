/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.bpm.test;

import org.mule.module.bpm.BPMS;
import org.mule.module.bpm.MessageService;

import java.util.Map;

/**
 * Dummy BPMS for unit testing.
 */
public class TestBpms implements BPMS
{
    private String foo;
    
    public void abortProcess(Object processId) throws Exception
    {
        // nop
    }

    public Object advanceProcess(Object processId, Object transition, Map processVariables) throws Exception
    {
        return null;
    }

    public Object getId(Object process) throws Exception
    {
        return null;
    }

    public Object getState(Object process) throws Exception
    {
        return null;
    }

    public boolean hasEnded(Object process) throws Exception
    {
        return false;
    }

    public boolean isProcess(Object obj) throws Exception
    {
        return false;
    }

    public Object lookupProcess(Object processId) throws Exception
    {
        return null;
    }

    public void setMessageService(MessageService msgService)
    {
        // nop
    }

    public Object startProcess(Object processType, Object transition, Map processVariables) throws Exception
    {
        return null;
    }

    public Object updateProcess(Object processId, Map processVariables) throws Exception
    {
        return null;
    }

    public String getFoo()
    {
        return foo;
    }

    public void setFoo(String foo)
    {
        this.foo = foo;
    }

    public void deployProcess(String resource) throws Exception
    {
        // nop
    }

    public void undeployProcess(String resource) throws Exception
    {
        // nop
    }

    public String getName()
    {
        return null;
    }

    public void setName(String name)
    {
        // nop
    }
}
