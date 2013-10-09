/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.tck.services;

import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;
import org.mule.util.UUID;

/**
 * Each instance of this service should contain a unique ID.  
 * Useful for testing object pools.
 */
public class UniqueComponent implements Callable
{
    String id = UUID.getUUID();

    public Object onCall(MuleEventContext eventContext) throws Exception
    {
        return getId();
    }

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }
}


