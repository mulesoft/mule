/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.services;

import org.mule.umo.UMOEventContext;
import org.mule.umo.lifecycle.Callable;
import org.mule.util.UUID;
import org.mule.util.object.Identifiable;

/**
 * Each instance of this component should contain a unique ID.  
 * Useful for testing object pools.
 */
public class UniqueComponent implements Callable, Identifiable
{
    String id = UUID.getUUID();

    public Object onCall(UMOEventContext eventContext) throws Exception
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


