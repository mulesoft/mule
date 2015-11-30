/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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


